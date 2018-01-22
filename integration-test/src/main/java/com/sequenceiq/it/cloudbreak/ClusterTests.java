package com.sequenceiq.it.cloudbreak;

import com.sequenceiq.it.cloudbreak.newway.AwsCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Cluster;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import org.testng.annotations.Test;

public class ClusterTests extends CloudbreakTest {

    public static final int BLUEPRINT_ID = 5;

    private CloudProvider cloudProvider;

    public ClusterTests() {
        this.cloudProvider = new AwsCloudProvider();
    }

    public ClusterTests(CloudProvider cp) {
        this.cloudProvider = cp;
    }

    @Test
    public void createCluster() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(Cluster.request()
                .withAmbariRequest(cloudProvider.ambariRequestWithBlueprintId((long) BLUEPRINT_ID)));
        given(cloudProvider.aValidStackRequest());
        when(Stack.post());
        then(Stack.waitAndCheckClusterAvailability());
    }

    /*
    @Test
    scaleUp scaleDown sync stop start
     */

    @Test
    public void terminateCluster() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackIsCreated());
        when(Stack.delete());
        then(Stack.waitAndCheckClusterDeleted());
    }
}
