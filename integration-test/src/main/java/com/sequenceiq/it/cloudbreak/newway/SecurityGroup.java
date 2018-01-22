package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class SecurityGroup extends SecurityGroupEntity {

    static Function<IntegrationTestContext, SecurityGroup> getTestContext(String key) {
        return (testContext)->testContext.getContextParam(key, SecurityGroup.class);
    }

    static Function<IntegrationTestContext, SecurityGroup> getNew() {
        return (testContext)->new SecurityGroup();
    }

    public static SecurityGroup request() {
        return new SecurityGroup();
    }

    public static SecurityGroup isCreated() {
        SecurityGroup securityGroup = new SecurityGroup();
        securityGroup.setCreationStrategy(SecurityGroupAction::createInGiven);
        return securityGroup;
    }

    public static Action<SecurityGroup> post(String key) {
        return new Action<SecurityGroup>(getTestContext(key), SecurityGroupAction::post);
    }

    public static Action<SecurityGroup> post() {
        return post(SECURITY_GROUP);
    }

    public static Assertion<SecurityGroup> assertThis(BiConsumer<SecurityGroup, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }
}
