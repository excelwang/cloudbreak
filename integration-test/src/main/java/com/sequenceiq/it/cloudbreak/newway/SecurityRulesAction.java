package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;

public class SecurityRulesAction {

    private SecurityRulesAction() {
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) {
        SecurityRulesEntity securityGroupEntity = (SecurityRulesEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        securityGroupEntity.setResponse(client.getCloudbreakClient().securityRuleEndpoint().getDefaultSecurityRules());
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) {
        get(integrationTestContext, entity);
    }
}
