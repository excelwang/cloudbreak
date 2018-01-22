package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;

public class SecurityGroupAction {

    private SecurityGroupAction() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) {
        SecurityGroupEntity securityGroupEntity = (SecurityGroupEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        if (securityGroupEntity.getRequest().getCredentialId() == null
                && securityGroupEntity.getRequest().getCredentialName() == null) {
            Credential credential = integrationTestContext
                    .getContextParam(Credential.CREDENTIAL, Credential.class);
            if (credential != null
                    && credential.getResponse() != null
                    && credential.getResponse().getId() != null) {
                securityGroupEntity
                        .getRequest()
                        .setCredentialId(credential.getResponse().getId());
            }
        }
        securityGroupEntity.setResponse(
                client.getCloudbreakClient()
                        .connectorV1Endpoint()
                        .getSecurityGroups(securityGroupEntity.getRequest()));
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) {
        post(integrationTestContext, entity);
    }
}
