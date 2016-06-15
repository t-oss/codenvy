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
package com.codenvy.plugin.urlfactory;

import com.google.common.base.Strings;

import org.eclipse.che.api.factory.shared.dto.FactoryDto;
import org.eclipse.che.api.machine.shared.dto.LimitsDto;
import org.eclipse.che.api.machine.shared.dto.MachineConfigDto;
import org.eclipse.che.api.machine.shared.dto.MachineSourceDto;
import org.eclipse.che.api.workspace.shared.dto.EnvironmentDto;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceConfigDto;
import org.eclipse.che.dto.server.DtoFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import static java.util.Collections.singletonList;

/**
 * Handle the creation of some elements used inside a {@link FactoryDto}
 * @author Florent Benoit
 */
@Singleton
public class URLFactoryBuilder {

    /**
     * Default docker image (if repository has no dockerfile)
     */
    protected static final String DEFAULT_DOCKER_IMAGE = "codenvy/ubuntu_jdk8";

    /**
     * Default docker type (if repository has no dockerfile)
     */
    protected static final String DEFAULT_DOCKER_TYPE = "image";

    /**
     * Check if URL is existing or not
     */
    @Inject
    private URLChecker URLChecker;

    /**
     * Grab content of URLs
     */
    @Inject
    private URLFetcher URLFetcher;


    /**
     * Build a default machine source
     * @return machine source.
     */
    protected MachineSourceDto buildDefaultMachineSource() {
        return DtoFactory.newDto(MachineSourceDto.class).withType(DEFAULT_DOCKER_TYPE).withLocation(DEFAULT_DOCKER_IMAGE);
    }

    /**
     * Build a default factory using the provided json file or create default one
     * @param createFactoryParams optional parameters
     * @return a factory
     */
    public FactoryDto createFactory(CreateFactoryParams createFactoryParams) {

        // Check if there is factory json file inside the repository
        if (createFactoryParams != null && createFactoryParams.codenvyJsonFileLocation() != null) {
            String factoryJsonContent = URLFetcher.fetch(createFactoryParams.codenvyJsonFileLocation());
            if (!Strings.isNullOrEmpty(factoryJsonContent)) {
                return DtoFactory.getInstance().createDtoFromJson(factoryJsonContent, FactoryDto.class);
            }
        }

        // else return a default factory
       return DtoFactory.newDto(FactoryDto.class).withV("4.0");
    }


    /**
     * Help to generate default workspace configuration
     * @param environmentName the name of the environment to create
     * @param name the name of the workspace
     * @param dockerFileLocation the optional location for codenvy dockerfileto use
     * @return a workspace configuration
     */
    public WorkspaceConfigDto buildWorkspaceConfig(String environmentName, String name, String dockerFileLocation) {

        // if remote repository contains a codenvy docker file, use it
        // else use the default image.
        final MachineSourceDto machineSourceDto;
        if (dockerFileLocation != null && URLChecker.exists(dockerFileLocation)) {
            machineSourceDto = DtoFactory.newDto(MachineSourceDto.class).withType("dockerfile").withLocation(dockerFileLocation);
        } else {
            machineSourceDto = buildDefaultMachineSource();
        }

        // set the memory limit
        LimitsDto limitsDto = DtoFactory.newDto(LimitsDto.class).withRam(2000);

        // Setup machine configuration
        MachineConfigDto machineConfigDto = DtoFactory.newDto(MachineConfigDto.class)
                                                      .withLimits(limitsDto)
                                                      .withType("docker")
                                                      .withDev(true)
                                                      .withSource(machineSourceDto)
                                                      .withName("ws-machine");



        // setup environment
        EnvironmentDto environmentDto = DtoFactory.newDto(EnvironmentDto.class)
                                                  .withName(environmentName)
                                                  .withMachineConfigs(singletonList(machineConfigDto));

        // workspace configuration using the environment
        return DtoFactory.newDto(WorkspaceConfigDto.class)
                         .withDefaultEnv(environmentName)
                         .withEnvironments(singletonList(environmentDto))
                         .withName(name);
    }
}
