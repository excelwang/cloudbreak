package com.sequenceiq.it.cloudbreak;

import com.sequenceiq.it.cloudbreak.newway.AwsCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.AzureCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.GcpCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.OpenstackCloudProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

public class ClusterTestFactory extends CloudbreakTest {
    @Factory
    @Parameters({"provider"})
    public Object[] clusterTestFactory(@Optional("gcp") String provider) {
        Object[] results;
        String[] providerArray = provider.split(",");
        results = new Object[providerArray.length];
        for (int i = 0; i < providerArray.length; i++) {
            CloudProvider cloudProvider;
            switch (providerArray[i].toLowerCase()) {
                case AwsCloudProvider.AWS:
                    cloudProvider = new AwsCloudProvider();
                    break;
                case AzureCloudProvider.AZURE:
                    cloudProvider = new AzureCloudProvider();
                    break;
                case GcpCloudProvider.GCP:
                    cloudProvider = new GcpCloudProvider();
                    break;
                case OpenstackCloudProvider.OPENSTACK:
                    cloudProvider = new OpenstackCloudProvider();
                    break;
                default:
                    cloudProvider = new AwsCloudProvider();

            }
            results[i] = new ClusterTests(cloudProvider);
        }
        return results;
    }
}
