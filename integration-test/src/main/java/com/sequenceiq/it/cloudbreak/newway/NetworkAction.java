package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;

public class NetworkAction {

    private NetworkAction() {
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        Network network = (Network) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        network.setResponse(client.getCloudbreakClient().connectorV1Endpoint().getCloudNetworks(network.getRegionsResponse()));
    }
}
