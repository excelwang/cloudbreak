package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.model.PlatformNetworkResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformResourceRequestJson;
import com.sequenceiq.it.IntegrationTestContext;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class Network extends Entity {
    public static final String NETWORK = "NETWORK";

    private PlatformResourceRequestJson platformResourceRequestJson;

    private Map<String, Set<PlatformNetworkResponse>> response;

    public Network(String id) {
        super(id);
    }

    public Network() {
        this(NETWORK);
    }

    public PlatformResourceRequestJson getRegionsResponse() {
        return platformResourceRequestJson;
    }

    public void setResponse(Map<String, Set<PlatformNetworkResponse>> response) {
        this.response = response;
    }

    public void setPlatformResourceRequestJson(PlatformResourceRequestJson platformResourceRequestJson) {
        this.platformResourceRequestJson = platformResourceRequestJson;
    }

    public Map<String, Set<PlatformNetworkResponse>> getResponse() {
        return response;
    }

    public Network withCredentialId(Long id) {
        platformResourceRequestJson.setCredentialId(id);
        return this;
    }

    public Network withCredentialName(String name) {
        platformResourceRequestJson.setCredentialName(name);
        return this;
    }

    public Network withRegion(String region) {
        platformResourceRequestJson.setRegion(region);
        return this;
    }

    static Function<IntegrationTestContext, Network> getTestContext(String key) {
        return (testContext) -> testContext.getContextParam(key, Network.class);
    }

    static Function<IntegrationTestContext, Network> getNew() {
        return (testContext) -> new Network();
    }

    public static Network request() {
        return new Network();
    }

    public static Action<Network> get(String key) {
        return new Action<>(getTestContext(key), RegionAction::getPlatformRegions);
    }

    public static Action<Network> get() {
        return get(NETWORK);
    }

    public static Assertion<Network> assertThis(BiConsumer<Network, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }
}
