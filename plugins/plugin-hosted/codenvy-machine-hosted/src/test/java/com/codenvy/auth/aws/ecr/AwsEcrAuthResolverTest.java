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
package com.codenvy.auth.aws.ecr;

import org.eclipse.che.plugin.docker.client.dto.AuthConfig;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Map;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Mykola Morhun
 */
@Listeners(MockitoTestNGListener.class)
public class AwsEcrAuthResolverTest {

    private static final String AWS_ID                = "123456789012";
    private static final String AWS_REGION            = "ua-north-1";
    private static final String AWS_ACCESS_KEY_ID     = "ABCDEFGHIJKLMNOPQRST";
    private static final String AWS_SECRET_ACCESS_KEY = "vERYverY+veRy+VeRySEcRETkEYfOrACCEss4YOu";

    private static final String AWS_ECR = AWS_ID + ".dkr.ecr." + AWS_REGION + ".amazonaws.com";

    private static final String USERNAME = "AWS";
    private static final String PASSWORD = "dynamicTokenIsHere";
    private static final String AUTHORIZATION_TOKEN = "QVdTOmR5bmFtaWNUb2tlbklzSGVyZQ==";

    @Mock
    private AwsInitialAuthConfig awsInitialAuthConfig;

    private AwsEcrAuthResolver awsEcrAuthResolver;

    @BeforeMethod
    private void setup() {
        when(awsInitialAuthConfig.getAwsAccountId()).thenReturn(AWS_ID);
        when(awsInitialAuthConfig.getRegion()).thenReturn(AWS_REGION);
        when(awsInitialAuthConfig.getAccessKeyId()).thenReturn(AWS_ACCESS_KEY_ID);
        when(awsInitialAuthConfig.getSecretAccessKey()).thenReturn(AWS_SECRET_ACCESS_KEY);
        when(awsInitialAuthConfig.getEcrHostname()).thenReturn(AWS_ECR);

        awsEcrAuthResolver = spy(new AwsEcrAuthResolver(awsInitialAuthConfig));
        doReturn(AUTHORIZATION_TOKEN).when(awsEcrAuthResolver).getAwsAuthorizationToken();
    }

    @Test
    public void shouldGetDynamicXRegistryAuth() {
        AuthConfig authConfig =
                awsEcrAuthResolver.getXRegistryAuth(AWS_ECR);

        verify(awsEcrAuthResolver).getAwsAuthorizationToken();

        assertEquals(authConfig.getUsername(), USERNAME);
        assertEquals(authConfig.getPassword(), PASSWORD);
    }

    @Test
    public void shouldReturnNullWhenGetDynamicXRegistryAuthIfRegistryIsNull() {
        assertNull(awsEcrAuthResolver.getXRegistryAuth(null));
    }

    @Test
    public void shouldReturnNullWhenGetDynamicXRegistryAuthIfCredentialsForSpecifiedRegistryIsNotConfigured() {
        assertNull(awsEcrAuthResolver.getXRegistryAuth("my.registry.com"));
    }

    @Test
    public void shouldReturnNullWhenGetDynamicXRegistryAuthIfCredentialsIsNotConfigured() {
        awsEcrAuthResolver = spy(new AwsEcrAuthResolver(new AwsInitialAuthConfig(null, null, null, null)));

        assertNull(awsEcrAuthResolver.getXRegistryAuth(AWS_ECR));
    }

    @Test
    public void shouldReturnNullWhenGetDynamicXRegistryAuthIfRetrievedAuthTokenCannotBeDecodedAsBase64() {
        doReturn("QVdTOmR5SomEvRoNgBaSe64VaLuE+=").when(awsEcrAuthResolver).getAwsAuthorizationToken();

        assertNull(awsEcrAuthResolver.getXRegistryAuth(AWS_ECR));
    }

    @Test
    public void shouldReturnNullWhenGetDynamicXRegistryAuthIfFailedToRetrieveAuthToken() {
        doReturn(null).when(awsEcrAuthResolver).getAwsAuthorizationToken();

        assertNull(awsEcrAuthResolver.getXRegistryAuth(AWS_ECR));
    }

    @Test
    public void shouldReturnNullWhenGetDynamicXRegistryAuthIfRetrievedAuthTokenHasWrongFormat() {
        doReturn("J7sLO0KsMwKKy7h1").when(awsEcrAuthResolver).getAwsAuthorizationToken();

        assertNull(awsEcrAuthResolver.getXRegistryAuth(AWS_ECR));
    }

    @Test
    public void shouldGetDynamicXRegistryConfig() {
        awsEcrAuthResolver = spy(new AwsEcrAuthResolver(new AwsInitialAuthConfig(AWS_ID,
                                                                                 AWS_REGION,
                                                                                 AWS_ACCESS_KEY_ID,
                                                                                 AWS_SECRET_ACCESS_KEY)));
        doReturn(AUTHORIZATION_TOKEN).when(awsEcrAuthResolver).getAwsAuthorizationToken();

        Map<String, AuthConfig> authConfigsMap =
                awsEcrAuthResolver.getXRegistryConfig();

        verify(awsEcrAuthResolver, atLeastOnce()).getXRegistryAuth(anyString());
        AuthConfig authConfig = authConfigsMap.get(AWS_ECR);
        assertEquals(authConfig.getUsername(), USERNAME);
        assertEquals(authConfig.getPassword(), PASSWORD);
    }

    @Test
    public void shouldReturnEmptyMapWhenGetDynamicXRegistryConfigIfCredentialsIsNotConfigured() {
        awsEcrAuthResolver = spy(new AwsEcrAuthResolver(new AwsInitialAuthConfig(null, null, null, null)));

        Map<String, AuthConfig> authConfigsMap =
                awsEcrAuthResolver.getXRegistryConfig();

        assertTrue(authConfigsMap.size() == 0);
    }

}
