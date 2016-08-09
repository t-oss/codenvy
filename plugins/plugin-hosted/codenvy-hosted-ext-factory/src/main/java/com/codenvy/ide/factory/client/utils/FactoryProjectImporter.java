/*
 *  [2012] - [2016] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.ide.factory.client.utils;

import com.codenvy.ide.factory.client.FactoryLocalizationConstant;
import com.google.gwt.core.client.JsArrayMixed;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;

import org.eclipse.che.api.core.model.project.SourceStorage;
import org.eclipse.che.api.factory.shared.dto.FactoryDto;
import org.eclipse.che.api.git.shared.GitCheckoutEvent;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.dialogs.DialogFactory;
import org.eclipse.che.ide.api.importer.AbstractImporter;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.notification.StatusNotification;
import org.eclipse.che.ide.api.oauth.OAuth2Authenticator;
import org.eclipse.che.ide.api.oauth.OAuth2AuthenticatorRegistry;
import org.eclipse.che.ide.api.oauth.OAuth2AuthenticatorUrlProvider;
import org.eclipse.che.ide.api.project.MutableProjectConfig;
import org.eclipse.che.ide.api.project.wizard.ImportProjectNotificationSubscriberFactory;
import org.eclipse.che.ide.api.project.wizard.ProjectNotificationSubscriber;
import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.resource.Path;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.RestContext;
import org.eclipse.che.ide.util.ExceptionUtils;
import org.eclipse.che.ide.util.StringUtils;
import org.eclipse.che.ide.websocket.MessageBus;
import org.eclipse.che.ide.websocket.MessageBusProvider;
import org.eclipse.che.ide.websocket.WebSocketException;
import org.eclipse.che.ide.websocket.rest.SubscriptionHandler;
import org.eclipse.che.security.oauth.OAuthStatus;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.eclipse.che.api.core.ErrorCodes.FAILED_CHECKOUT;
import static org.eclipse.che.api.core.ErrorCodes.FAILED_CHECKOUT_WITH_START_POINT;
import static org.eclipse.che.api.core.ErrorCodes.UNABLE_GET_PRIVATE_SSH_KEY;
import static org.eclipse.che.api.core.ErrorCodes.UNAUTHORIZED_GIT_OPERATION;
import static org.eclipse.che.api.git.shared.ProviderInfo.AUTHENTICATE_URL;
import static org.eclipse.che.api.git.shared.ProviderInfo.PROVIDER_NAME;
import static org.eclipse.che.ide.api.notification.StatusNotification.DisplayMode.FLOAT_MODE;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.FAIL;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.PROGRESS;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.SUCCESS;

/**
 * @author Sergii Leschenko
 * @author Valeriy Svydenko
 * @author Anton Korneta
 */
public class FactoryProjectImporter extends AbstractImporter {
    private static final String CHANNEL = "git:checkout:";

    private final MessageBusProvider          messageBusProvider;
    private final FactoryLocalizationConstant locale;
    private final NotificationManager         notificationManager;
    private final String                      restContext;
    private final DialogFactory               dialogFactory;
    private final OAuth2AuthenticatorRegistry oAuth2AuthenticatorRegistry;
    private final DtoUnmarshallerFactory      dtoUnmarshallerFactory;

    private FactoryDto          factory;
    private AsyncCallback<Void> callback;

    @Inject
    public FactoryProjectImporter(AppContext appContext,
                                  NotificationManager notificationManager,
                                  FactoryLocalizationConstant locale,
                                  ImportProjectNotificationSubscriberFactory subscriberFactory,
                                  @RestContext String restContext,
                                  DialogFactory dialogFactory,
                                  OAuth2AuthenticatorRegistry oAuth2AuthenticatorRegistry,
                                  MessageBusProvider messageBusProvider,
                                  DtoUnmarshallerFactory dtoUnmarshallerFactory) {
        super(appContext, subscriberFactory);
        this.notificationManager = notificationManager;
        this.locale = locale;
        this.restContext = restContext;
        this.dialogFactory = dialogFactory;
        this.oAuth2AuthenticatorRegistry = oAuth2AuthenticatorRegistry;
        this.messageBusProvider = messageBusProvider;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
    }

    public void startImporting(FactoryDto factory, AsyncCallback<Void> callback) {
        this.callback = callback;
        this.factory = factory;
        importProjects();
    }

    /**
     * Import source projects
     */
    private void importProjects() {
        final Project[] projects = appContext.getProjects();

        Set<String> projectNames = new HashSet<>();
        String createPolicy = factory.getPolicies() != null ? factory.getPolicies().getCreate() : null;
        for (Project project : projects) {
            if (project.getSource() == null || project.getSource().getLocation() == null) {
                continue;
            }

            if (project.exists()) {
                // to prevent warning when reusing same workspace
                if (!("perUser".equals(createPolicy) || "perAccount".equals(createPolicy))) {
                    notificationManager.notify("Import", locale.projectAlreadyImported(project.getName()), FAIL, FLOAT_MODE);
                }
                continue;
            }

            projectNames.add(project.getName());
        }
        importProjects(projectNames);
    }

    /**
     * Import source projects and if it's already exist in workspace
     * then show warning notification
     *
     * @param projectsToImport
     *         set of project names that already exist in workspace and will be imported on file system
     */
    private void importProjects(Set<String> projectsToImport) {
        final List<Promise<Project>> promises = new ArrayList<>();
        for (final ProjectConfigDto projectConfig : factory.getWorkspace().getProjects()) {
            if (projectsToImport.contains(projectConfig.getName())) {
                promises.add(startImport(Path.valueOf(projectConfig.getPath()), projectConfig.getSource()).thenPromise(
                        new Function<Project, Promise<Project>>() {
                            @Override
                            public Promise<Project> apply(Project project) throws FunctionException {
                                return project.update().withBody(projectConfig).send();
                            }
                        }));
            }
        }

        Promises.all(promises.toArray(new Promise<?>[promises.size()]))
                .then(new Operation<JsArrayMixed>() {
                    @Override
                    public void apply(JsArrayMixed arg) throws OperationException {
                        callback.onSuccess(null);
                    }
                })
                .catchError(new Operation<PromiseError>() {
                    @Override
                    public void apply(PromiseError promiseError) throws OperationException {
                        // If it is unable to import any number of projects then factory import status will be success anyway
                        callback.onSuccess(null);
                    }
                });
    }

    @Override
    protected Promise<Project> importProject(@NotNull final Path pathToProject,
                                             @NotNull final SourceStorage sourceStorage) {
        return doImport(pathToProject, sourceStorage);
    }


    private Promise<Project> doImport(@NotNull final Path pathToProject,
                                      @NotNull final SourceStorage sourceStorage) {
        final String projectName = pathToProject.lastSegment();
        final StatusNotification notification = notificationManager.notify(locale.cloningSource(projectName), null, PROGRESS, FLOAT_MODE);
        final ProjectNotificationSubscriber subscriber = subscriberFactory.createSubscriber();
        subscriber.subscribe(projectName, notification);
        String location = sourceStorage.getLocation();
        // it's needed for extract repository name from repository url e.g https://github.com/codenvy/che-core.git
        // lastIndexOf('/') + 1 for not to capture slash and length - 4 for trim .git
        final String repository = location.substring(location.lastIndexOf('/') + 1).replace(".git", "");
        final Map<String, String> parameters = firstNonNull(sourceStorage.getParameters(), Collections.<String, String>emptyMap());
        final String branch = parameters.get("branch");
        final String startPoint = parameters.get("startPoint");
        final MessageBus messageBus = messageBusProvider.getMachineMessageBus();
        final String channel = CHANNEL + appContext.getWorkspaceId() + ':' + projectName;
        final SubscriptionHandler<GitCheckoutEvent> successImportHandler = new SubscriptionHandler<GitCheckoutEvent>(
                dtoUnmarshallerFactory.newWSUnmarshaller(GitCheckoutEvent.class)) {
            @Override
            protected void onMessageReceived(GitCheckoutEvent result) {
                if (result.isCheckoutOnly()) {
                    notificationManager.notify(locale.clonedSource(projectName),
                                               locale.clonedSourceWithCheckout(projectName, repository, result.getBranchRef(), branch),
                                               SUCCESS,
                                               FLOAT_MODE);
                } else {
                    notificationManager.notify(locale.clonedSource(projectName),
                                               locale.clonedWithCheckoutOnStartPoint(projectName, repository, startPoint, branch),
                                               SUCCESS,
                                               FLOAT_MODE);
                }
            }

            @Override
            protected void onErrorReceived(Throwable e) {
                try {
                    messageBus.unsubscribe(channel, this);
                } catch (WebSocketException ignore) {
                }
            }
        };
        try {
            messageBus.subscribe(channel, successImportHandler);
        } catch (WebSocketException ignore) {
        }

        MutableProjectConfig importConfig = new MutableProjectConfig();
        importConfig.setPath(pathToProject.toString());
        importConfig.setSource(sourceStorage);

        return appContext.getWorkspaceRoot()
                         .importProject()
                         .withBody(importConfig)
                         .send()
                         .then(new Function<Project, Project>() {
                             @Override
                             public Project apply(Project project) throws FunctionException {
                                 subscriber.onSuccess();
                                 notification.setContent(locale.clonedSource(projectName));
                                 notification.setStatus(SUCCESS);

                                 return project;
                             }
                         })
                         .catchErrorPromise(new Function<PromiseError, Promise<Project>>() {
                             @Override
                             public Promise<Project> apply(PromiseError err) throws FunctionException {
                                 final int errorCode = ExceptionUtils.getErrorCode(err.getCause());
                                 switch (errorCode) {
                                     case UNAUTHORIZED_GIT_OPERATION:
                                         subscriber.onFailure(err.getMessage());
                                         final Map<String, String> attributes = ExceptionUtils.getAttributes(err.getCause());
                                         final String providerName = attributes.get(PROVIDER_NAME);
                                         final String authenticateUrl = attributes.get(AUTHENTICATE_URL);
                                         final boolean authenticated = Boolean.parseBoolean(attributes.get("authenticated"));
                                         if (!StringUtils.isNullOrEmpty(providerName) && !StringUtils.isNullOrEmpty(authenticateUrl)) {
                                             if (!authenticated) {
                                                 return tryAuthenticateAndRepeatImport(providerName,
                                                                                       authenticateUrl,
                                                                                       pathToProject,
                                                                                       sourceStorage,
                                                                                       subscriber);
                                             } else {
                                                 dialogFactory.createMessageDialog(locale.cloningSourceSshKeyUploadFailedTitle(),
                                                                                   locale.cloningSourcesSshKeyUploadFailedText(), null)
                                                              .show();
                                             }
                                         } else {
                                             dialogFactory.createMessageDialog(locale.oauthFailedToGetAuthenticatorTitle(),
                                                                               locale.oauthFailedToGetAuthenticatorText(), null).show();
                                         }

                                         break;
                                     case UNABLE_GET_PRIVATE_SSH_KEY:
                                         subscriber.onFailure(locale.acceptSshNotFoundText());
                                         break;
                                     case FAILED_CHECKOUT:
                                         subscriber.onFailure(locale.cloningSourceWithCheckoutFailed(branch, repository));
                                         notification.setTitle(locale.cloningSourceFailedTitle(projectName));
                                         break;
                                     case FAILED_CHECKOUT_WITH_START_POINT:
                                         subscriber.onFailure(locale.cloningSourceCheckoutFailed(branch, startPoint));
                                         notification.setTitle(locale.cloningSourceFailedTitle(projectName));
                                         break;
                                     default:
                                         subscriber.onFailure(err.getMessage());
                                         notification.setTitle(locale.cloningSourceFailedTitle(projectName));
                                         notification.setStatus(FAIL);
                                 }

                                 return Promises.resolve(null);
                             }
                         });
    }

    private Promise<Project> tryAuthenticateAndRepeatImport(@NotNull final String providerName,
                                                            @NotNull final String authenticateUrl,
                                                            @NotNull final Path pathToProject,
                                                            @NotNull final SourceStorage sourceStorage,
                                                            @NotNull final ProjectNotificationSubscriber subscriber) {
        OAuth2Authenticator authenticator = oAuth2AuthenticatorRegistry.getAuthenticator(providerName);
        if (authenticator == null) {
            authenticator = oAuth2AuthenticatorRegistry.getAuthenticator("default");
        }
        return authenticator.authenticate(OAuth2AuthenticatorUrlProvider.get(restContext, authenticateUrl)).thenPromise(
                new Function<OAuthStatus, Promise<Project>>() {
                    @Override
                    public Promise<Project> apply(OAuthStatus result) throws FunctionException {
                        if (!result.equals(OAuthStatus.NOT_PERFORMED)) {
                            return doImport(pathToProject, sourceStorage);
                        } else {
                            subscriber.onFailure("Authentication cancelled");
                            callback.onSuccess(null);
                        }

                        return Promises.resolve(null);
                    }
                }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError caught) throws OperationException {
                callback.onFailure(new Exception(caught.getMessage()));
            }
        });
    }
}
