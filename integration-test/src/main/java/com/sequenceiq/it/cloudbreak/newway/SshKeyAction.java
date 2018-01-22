package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;

public class SshKeyAction {

    private SshKeyAction() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) {
        SshKeyEntity sshKeyEntity = (SshKeyEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        if (sshKeyEntity.getRequest().getCredentialId() == null
                && sshKeyEntity.getRequest().getCredentialName() == null) {
            Credential credential = integrationTestContext
                    .getContextParam(Credential.CREDENTIAL, Credential.class);
            if (credential != null
                    && credential.getResponse() != null
                    && credential.getResponse().getId() != null) {
                sshKeyEntity
                        .getRequest()
                        .setCredentialId(credential.getResponse().getId());
            }
        }
        sshKeyEntity.setResponse(
                client.getCloudbreakClient()
                        .connectorV1Endpoint()
                        .getCloudSshKeys(sshKeyEntity.getRequest()));
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) {
        post(integrationTestContext, entity);
    }
}
