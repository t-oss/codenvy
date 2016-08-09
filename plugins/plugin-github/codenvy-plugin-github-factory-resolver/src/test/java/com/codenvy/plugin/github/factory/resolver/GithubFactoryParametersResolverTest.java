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
package com.codenvy.plugin.github.factory.resolver;

import com.codenvy.plugin.urlfactory.CreateFactoryParams;
import com.codenvy.plugin.urlfactory.ProjectConfigDtoMerger;
import com.codenvy.plugin.urlfactory.URLFactoryBuilder;

import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.factory.shared.dto.FactoryDto;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.SourceStorageDto;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Map;

import static com.codenvy.plugin.github.factory.resolver.GithubFactoryParametersResolver.URL_PARAMETER_NAME;
import static java.util.Collections.singletonMap;
import static org.eclipse.che.dto.server.DtoFactory.newDto;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Validate operations performed by the Github Factory service
 *
 * @author Florent Benoit
 */
@Listeners(MockitoTestNGListener.class)
public class GithubFactoryParametersResolverTest {

    /**
     * Parser which will allow to check validity of URLs and create objects.
     */
    @Spy
    private GithubUrlParser githubUrlParser = new GithubUrlParser();

    /**
     * Converter allowing to convert github URL to other objects.
     */
    @Spy
    private GithubSourceStorageBuilder githubSourceStorageBuilder = new GithubSourceStorageBuilder();

    /**
     * ProjectDtoMerger
     */
    @Mock
    private ProjectConfigDtoMerger projectConfigDtoMerger = new ProjectConfigDtoMerger();

    /**
     * Parser which will allow to check validity of URLs and create objects.
     */
    @Mock
    private URLFactoryBuilder urlFactoryBuilder;

    /**
     * Capturing the project config DTO parameter.
     */
    @Captor
    private ArgumentCaptor<ProjectConfigDto> projectConfigDtoArgumentCaptor;

    /**
     * Capturing the parameter when calling {@link URLFactoryBuilder#createFactory(CreateFactoryParams)}
     */
    @Captor
    private ArgumentCaptor<CreateFactoryParams> createFactoryParamsArgumentCaptor;

    /**
     * Instance of resolver that will be tested.
     */
    @InjectMocks
    private GithubFactoryParametersResolver githubFactoryParametersResolver;


    /**
     * Check missing parameter name can't be accepted by this resolver
     */
    @Test
    public void checkMissingParameter() throws BadRequestException {
        Map<String, String> parameters = singletonMap("foo", "this is a foo bar");
        boolean accept = githubFactoryParametersResolver.accept(parameters);
        // shouldn't be accepted
        assertFalse(accept);
    }

    /**
     * Check url which is not a github url can't be accepted by this resolver
     */
    @Test
    public void checkInvalidAcceptUrl() throws BadRequestException {
        Map<String, String> parameters = singletonMap(URL_PARAMETER_NAME, "http://www.eclipse.org/che");
        boolean accept = githubFactoryParametersResolver.accept(parameters);
        // shouldn't be accepted
        assertFalse(accept);
    }

    /**
     * Check github url will be be accepted by this resolver
     */
    @Test
    public void checkValidAcceptUrl() throws BadRequestException {
        Map<String, String> parameters = singletonMap(URL_PARAMETER_NAME, "https://github.com/codenvy/codenvy.git");
        boolean accept = githubFactoryParametersResolver.accept(parameters);
        // shouldn't be accepted
        assertTrue(accept);
    }


    /**
     * Check that with a simple valid URL github url it works
     */
    @Test
    public void shouldReturnGitHubSimpleFactory() throws Exception {

        String githubUrl = "https://github.com/eclipse/che";

        FactoryDto computedFactory = newDto(FactoryDto.class).withV("4.0");
        when(urlFactoryBuilder.createFactory(any(CreateFactoryParams.class))).thenReturn(computedFactory);

        githubFactoryParametersResolver.createFactory(singletonMap(URL_PARAMETER_NAME, githubUrl));

        // check we called the builder with the following codenvy json file
        verify(urlFactoryBuilder).createFactory(createFactoryParamsArgumentCaptor.capture());
        assertEquals(createFactoryParamsArgumentCaptor.getValue().codenvyJsonFileLocation(), "https://raw.githubusercontent.com/eclipse/che/master/.codenvy.json");


        // check we provide dockerfile and correct env
        verify(urlFactoryBuilder).buildWorkspaceConfig(eq("che"), eq("eclipse"), eq("https://raw.githubusercontent.com/eclipse/che/master/.codenvy.dockerfile"));

        // check project config built
        verify(projectConfigDtoMerger).merge(any(FactoryDto.class), projectConfigDtoArgumentCaptor.capture());

        ProjectConfigDto projectConfigDto = projectConfigDtoArgumentCaptor.getValue();

        SourceStorageDto sourceStorageDto = projectConfigDto.getSource();
        assertNotNull(sourceStorageDto);
        assertEquals(sourceStorageDto.getType(), "git");
        assertEquals(sourceStorageDto.getLocation(), githubUrl);
        Map<String, String> sourceParameters = sourceStorageDto.getParameters();
        assertEquals(sourceParameters.size(), 1);
        assertEquals(sourceParameters.get("branch"), "master");
    }

    /**
     * Check that we've expected branch when url contains a branch name
     */
    @Test
    public void shouldReturnGitHubBranchFactory() throws Exception {

        String githubUrl = "https://github.com/eclipse/che/tree/4.2.x";
        String githubCloneUrl = "https://github.com/eclipse/che";
        String githubBranch = "4.2.x";

        FactoryDto computedFactory = newDto(FactoryDto.class).withV("4.0");
        when(urlFactoryBuilder.createFactory(any(CreateFactoryParams.class))).thenReturn(computedFactory);

        githubFactoryParametersResolver.createFactory(singletonMap(URL_PARAMETER_NAME, githubUrl));

        // check we called the builder with the following codenvy json file
        verify(urlFactoryBuilder).createFactory(createFactoryParamsArgumentCaptor.capture());
        assertEquals(createFactoryParamsArgumentCaptor.getValue().codenvyJsonFileLocation(), "https://raw.githubusercontent.com/eclipse/che/4.2.x/.codenvy.json");

        // check we provide dockerfile and correct env
        verify(urlFactoryBuilder).buildWorkspaceConfig(eq("che"), eq("eclipse"), eq("https://raw.githubusercontent.com/eclipse/che/4.2.x/.codenvy.dockerfile"));

        // check project config built
        verify(projectConfigDtoMerger).merge(any(FactoryDto.class), projectConfigDtoArgumentCaptor.capture());

        ProjectConfigDto projectConfigDto = projectConfigDtoArgumentCaptor.getValue();
        SourceStorageDto sourceStorageDto = projectConfigDto.getSource();
        assertNotNull(sourceStorageDto);
        assertEquals(sourceStorageDto.getType(), "git");
        assertEquals(sourceStorageDto.getLocation(), githubCloneUrl);
        Map<String, String> sourceParameters = sourceStorageDto.getParameters();
        assertEquals(sourceParameters.size(), 1);
        assertEquals(sourceParameters.get("branch"), githubBranch);

    }

    /**
     * Check that we have a sparse checkout "keepDir" if url contains branch and subtree.
     */
    @Test
    public void shouldReturnGitHubBranchAndKeepdirFactory() throws Exception {

        String githubUrl = "https://github.com/eclipse/che/tree/4.2.x/dashboard";
        String githubCloneUrl = "https://github.com/eclipse/che";
        String githubBranch = "4.2.x";
        String githubKeepdir = "dashboard";

        FactoryDto computedFactory = newDto(FactoryDto.class).withV("4.0");
        when(urlFactoryBuilder.createFactory(any(CreateFactoryParams.class))).thenReturn(computedFactory);

        githubFactoryParametersResolver.createFactory(singletonMap(URL_PARAMETER_NAME, githubUrl));

        // check we called the builder with the following codenvy json file
        verify(urlFactoryBuilder).createFactory(createFactoryParamsArgumentCaptor.capture());
        assertEquals(createFactoryParamsArgumentCaptor.getValue().codenvyJsonFileLocation(), "https://raw.githubusercontent.com/eclipse/che/4.2.x/.codenvy.json");

        // check we provide dockerfile and correct env
        verify(urlFactoryBuilder).buildWorkspaceConfig(eq("che"), eq("eclipse"), eq("https://raw.githubusercontent.com/eclipse/che/4.2.x/.codenvy.dockerfile"));

        // check project config built
        verify(projectConfigDtoMerger).merge(any(FactoryDto.class), projectConfigDtoArgumentCaptor.capture());

        ProjectConfigDto projectConfigDto = projectConfigDtoArgumentCaptor.getValue();
        SourceStorageDto sourceStorageDto = projectConfigDto.getSource();
        assertNotNull(sourceStorageDto);
        assertEquals(sourceStorageDto.getType(), "git");
        assertEquals(sourceStorageDto.getLocation(), githubCloneUrl);
        Map<String, String> sourceParameters = sourceStorageDto.getParameters();
        assertEquals(sourceParameters.size(), 2);
        assertEquals(sourceParameters.get("branch"), githubBranch);
        assertEquals(sourceParameters.get("keepDir"), githubKeepdir);

    }


}
