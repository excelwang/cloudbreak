package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.model.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.TemplateV2Request;

import java.util.HashMap;
import java.util.Map;

public class AzureCloudProvider extends CloudProviderHelper {
    public static final String AZURE = "azure";

    public static final String AZURE_CAPITAL = "AZURE";

    static final String CREDNAME = "testazurecred";

    static final String CREDDESC = "test credential";

    public AzureCloudProvider() {
    }

    @Override
    public CredentialEntity aValidCredential() {
        return Credential.isCreated()
                .withName(CREDNAME)
                .withDescription(CREDDESC)
                .withCloudPlatform(AZURE_CAPITAL)
                .withParameters(azureCredentialDetails());
    }

    @Override
    String availabilityZone() {
        return null;
    }

    @Override
    String region() {
        String region = "North Europe";
        String regionParam = TestParameter.get("azureRegion");

        return regionParam == null ? region : regionParam;
    }

    @Override
    StackAuthenticationRequest stackauth() {
        StackAuthenticationRequest stackauth = new StackAuthenticationRequest();
        stackauth.setPublicKey(TestParameter.get("integrationtest.publicKeyFile"));
        return stackauth;
    }

    @Override
    TemplateV2Request template() {
        TemplateV2Request t = new TemplateV2Request();
        String instanceTypeDefaultValue = "Standard_D3_v2";
        String instanceTypeParam = TestParameter.get("azureInstanceType");
        t.setInstanceType(instanceTypeParam == null ? instanceTypeDefaultValue : instanceTypeParam);

        int volumeCountDefault = 1;
        String volumeCountParam = TestParameter.get("azureInstanceVolumeCount");
        t.setVolumeCount(volumeCountParam == null ? volumeCountDefault : Integer.parseInt(volumeCountParam));

        int volumeSizeDefault = 10;
        String volumeSizeParam = TestParameter.get("azureInstanceVolumeSize");
        t.setVolumeSize(volumeSizeParam == null ? volumeSizeDefault : Integer.parseInt(volumeSizeParam));

        String volumeTypeDefault = "Standard_LRS";
        String volumeTypeParam = TestParameter.get("azureInstanceVolumeType");
        t.setVolumeType(volumeTypeParam == null ? volumeTypeDefault : volumeTypeParam);

        Map<String, Object> params = new HashMap<>();
        params.put("encrypted", "false");
        params.put("managedDisk", "true");
        t.setParameters(params);
        return t;
    }

    @Override
    public String getProviderName() {
        return AZURE;
    }

    Map<String, Object> azureCredentialDetails() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("accessKey", TestParameter.get("integrationtest.azurermcredential.accessKey"));
        map.put("secretKey", TestParameter.get("integrationtest.azurermcredential.secretKey"));
        map.put("subscriptionId", TestParameter.get("integrationtest.azurermcredential.subscriptionId"));
        map.put("tenantId", TestParameter.get("integrationtest.azurermcredential.tenantId"));

        return map;
    }

    @Override
    NetworkV2Request network() {
        NetworkV2Request network = new NetworkV2Request();
        network.setSubnetCIDR("10.0.0.0/16");
        return network;
    }
}