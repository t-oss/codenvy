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

import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.eclipse.che.commons.annotation.Nullable;

import javax.inject.Inject;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Keeps auth configurations for AWS Elastic Container Registry.
 * Credential might be configured in .properties files.
 *
 * @author Mykola Morhun
 */
@Singleton
public class AwsInitialAuthConfig {

    private final String awsAccountId;
    private final String region;
    private final String accessKeyId;
    private final String secretAccessKey;
    private final String ecr;

    private boolean isConfigValid;

    @Inject
    public AwsInitialAuthConfig(@Nullable @Named("docker.registry.aws.id") String awsId,
                                @Nullable @Named("docker.registry.aws.region") String awsRegion,
                                @Nullable @Named("docker.registry.aws.access-key-id") String accessKeyId,
                                @Nullable @Named("docker.registry.aws.secret-access-key") String secretAccessKey) {
        isConfigValid = validateConfig(awsId, awsRegion, accessKeyId, secretAccessKey);
        this.awsAccountId = validateAwsProperty(awsId, "AWS Account Id is not configured");
        this.region = validateAwsProperty(awsRegion, "AWS Region is not configured");
        this.accessKeyId = validateAwsProperty(accessKeyId, "AWS Access Key Id is not configured");
        this.secretAccessKey = validateAwsProperty(secretAccessKey, "AWS Secret Access Key is not configured");
        this.ecr = calculateEcrHostname(awsId, awsRegion);
    }

    private boolean validateConfig(String awsId, String awsRegion, String accessKeyId, String secretAccessKey) {
        boolean isAwsAccountIdConfigured = isNullOrEmpty(awsId);
        boolean isAwsRegionConfigured = isNullOrEmpty(awsRegion);
        boolean isAccessKeyIdConfigured = isNullOrEmpty(accessKeyId);
        boolean isSecretAccessKeyConfigured = isNullOrEmpty(secretAccessKey);

        return isAwsAccountIdConfigured == isAwsRegionConfigured
               && isAccessKeyIdConfigured == isSecretAccessKeyConfigured
               && isAwsAccountIdConfigured == isAccessKeyIdConfigured;
    }

    private String validateAwsProperty(String propertyValue, String errorMessage) {
        if (isConfigValid) {
            if (isNullOrEmpty(propertyValue)) {
                propertyValue = null;
            }
            return propertyValue;
        }
        throw new IllegalArgumentException(errorMessage);
    }

    private String calculateEcrHostname(String awsAccountId, String awsRegion) {
        if (!isNullOrEmpty(awsAccountId) && !isNullOrEmpty(awsRegion)) {
            return awsAccountId + ".dkr.ecr." + awsRegion + ".amazonaws.com";
        }
        return null;
    }

    @Nullable
    public String getAwsAccountId() {
        return awsAccountId;
    }

    @Nullable
    public String getRegion() {
        return region;
    }

    @Nullable
    public String getAccessKeyId() {
        return accessKeyId;
    }

    @Nullable
    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    @Nullable
    public String getEcrHostname() {
        return ecr;
    }

}
