package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class SshKey extends SshKeyEntity {

    static Function<IntegrationTestContext, SshKey> getTestContext(String key) {
        return (testContext)->testContext.getContextParam(key, SshKey.class);
    }

    static Function<IntegrationTestContext, SshKey> getNew() {
        return (testContext)->new SshKey();
    }

    public static SshKey request() {
        return new SshKey();
    }

    public static SshKey isCreated() {
        SshKey sshKey = new SshKey();
        sshKey.setCreationStrategy(SshKeyAction::createInGiven);
        return sshKey;
    }

    public static Action<SshKey> post(String key) {
        return new Action<SshKey>(getTestContext(key), SshKeyAction::post);
    }

    public static Action<SshKey> post() {
        return post(CREDENTIAL);
    }

    public static Assertion<SshKey> assertThis(BiConsumer<SshKey, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }
}
