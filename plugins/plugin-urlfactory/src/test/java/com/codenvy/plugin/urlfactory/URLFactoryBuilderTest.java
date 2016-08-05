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

import org.eclipse.che.api.factory.shared.dto.FactoryDto;
import org.eclipse.che.api.machine.shared.dto.MachineConfigDto;
import org.eclipse.che.api.machine.shared.dto.MachineSourceDto;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceConfigDto;
import org.eclipse.che.dto.server.DtoFactory;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static com.codenvy.plugin.urlfactory.URLFactoryBuilder.DEFAULT_DOCKER_IMAGE;
import static com.codenvy.plugin.urlfactory.URLFactoryBuilder.DEFAULT_DOCKER_TYPE;
import static java.lang.Boolean.FALSE;
import static org.eclipse.che.dto.server.DtoFactory.newDto;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

/**
 * Testing {@link URLFactoryBuilder}
 *
 * @author Florent Benoit
 */
@Listeners(MockitoTestNGListener.class)
public class URLFactoryBuilderTest {

    /**
     * Check if URL is existing or not
     */
    @Mock
    private URLChecker URLChecker;

    /**
     * Grab content of URLs
     */
    @Mock
    private URLFetcher URLFetcher;

    /**
     * Tested instance.
     */
    @InjectMocks
    private URLFactoryBuilder urlFactoryBuilder;


    /**
     * Check if not specifying a custom docker file we have the default value
     */
    @Test
    public void checkDefaultDockerfile() throws Exception {

        String myLocation = "http://foo-location";
        when(URLChecker.exists(myLocation)).thenReturn(false);

        WorkspaceConfigDto workspaceConfigDto = urlFactoryBuilder.buildWorkspaceConfig("foo", "dumm", myLocation);

        MachineConfigDto machineConfigDto = workspaceConfigDto.getEnvironments().get(0).devMachine();

        assertEquals(machineConfigDto.getType(), "docker");
        MachineSourceDto machineSourceDto = machineConfigDto.getSource();
        assertNotNull(machineSourceDto);
        assertEquals(machineSourceDto.getLocation(), DEFAULT_DOCKER_IMAGE);
        assertEquals(machineSourceDto.getType(), DEFAULT_DOCKER_TYPE);
    }


    /**
     * Check that by specifying a custom dockerfile it's stored in the machine source
     */
    @Test
    public void checkWithCustomDockerfile() throws Exception {

        String myLocation = "http://foo-location";
        when(URLChecker.exists(myLocation)).thenReturn(true);

        WorkspaceConfigDto workspaceConfigDto = urlFactoryBuilder.buildWorkspaceConfig("foo", "dumm", myLocation);

        MachineConfigDto machineConfigDto = workspaceConfigDto.getEnvironments().get(0).devMachine();

        assertEquals(machineConfigDto.getType(), "docker");
        MachineSourceDto machineSourceDto = machineConfigDto.getSource();
        assertNotNull(machineSourceDto);
        assertEquals(machineSourceDto.getLocation(), myLocation);
        assertEquals(machineSourceDto.getType(), "dockerfile");
    }

    /**
     * Check that with a custom factory.json we've this factory being built
     */
    @Test
    public void checkWithCustomFactoryJsonFile() throws Exception {

        WorkspaceConfigDto workspaceConfigDto = newDto(WorkspaceConfigDto.class);
        FactoryDto templateFactory = newDto(FactoryDto.class).withV("4.0").withName("florent").withWorkspace(workspaceConfigDto);
        String jsonFactory = DtoFactory.getInstance().toJson(templateFactory);


        String myLocation = "http://foo-location";
        when(URLChecker.exists(myLocation)).thenReturn(FALSE);
        when(URLFetcher.fetch(myLocation)).thenReturn(jsonFactory);

        FactoryDto factory = urlFactoryBuilder.createFactory(CreateFactoryParams.create().codenvyJsonFileLocation(myLocation));

        assertEquals(templateFactory, factory);

    }


    /**
     * Check that without specifying a custom factory.json we've default factory
     */
    @Test
    public void checkWithDefaultFactoryJsonFile() throws Exception {

        FactoryDto factory = urlFactoryBuilder.createFactory(null);

        assertNull(factory.getWorkspace());
        assertEquals(factory.getV(), "4.0");

    }
}
