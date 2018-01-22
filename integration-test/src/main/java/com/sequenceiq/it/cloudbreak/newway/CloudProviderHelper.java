package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.OrchestratorRequest;
import com.sequenceiq.cloudbreak.api.model.SecurityRuleRequest;
import com.sequenceiq.cloudbreak.api.model.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.SecurityGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.TemplateV2Request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class CloudProviderHelper extends CloudProvider {
    public static final String TESTCLUSTER = "testcluster";

    public StackEntity aValidStackRequest() {
        return Stack.request()
                .withName(TESTCLUSTER + getProviderName())
                .withRegion(region())
                .withAvailabilityZone(availabilityZone())
                .withInstanceGroups(instanceGroups())
                .withNetwork(network())
                .withOrchestrator(orchestrator())
                .withParameters(parameters())
                .withStackAuthentication(stackauth());
    }

    abstract String availabilityZone();

    abstract String region();

    @Override
    public Entity aValidStackIsCreated() {
        return Stack.isCreated()
                .withName(TESTCLUSTER + getProviderName())
                .withRegion(region())
                .withAvailabilityZone(availabilityZone())
                .withInstanceGroups(instanceGroups())
                .withNetwork(network())
                .withOrchestrator(orchestrator())
                .withParameters(parameters())
                .withStackAuthentication(stackauth());
    }

    abstract StackAuthenticationRequest stackauth();

    Map<String, String> parameters() {
        Map<String, String> params = new HashMap<String, String>();
        params.put("instanceProfileStrategy", "CREATE");
        return params;
    }

    abstract NetworkV2Request network();

    List<InstanceGroupV2Request> instanceGroups() {
        List<InstanceGroupV2Request> requests = new ArrayList<InstanceGroupV2Request>();
        requests.add(master());
        requests.add(compute());
        requests.add(worker());
        return requests;
    }

    OrchestratorRequest orchestrator() {
        OrchestratorRequest request = new OrchestratorRequest();
        request.setType("SALT");
        return request;
    }

    @Override
    public AmbariV2Request ambariRequestWithBlueprintId(Long id) {
        AmbariV2Request req = new AmbariV2Request();
        req.setUserName("admin");
        req.setPassword("password");
        req.setBlueprintId(id);
        return req;
    }

    InstanceGroupV2Request master() {
        InstanceGroupV2Request r = new InstanceGroupV2Request();
        r.setNodeCount(1);
        r.setGroup("master");
        r.setType(InstanceGroupType.GATEWAY);
        SecurityGroupV2Request s = new SecurityGroupV2Request();
        s.setSecurityRules(rules());
        r.setSecurityGroup(s);
        r.setTemplate(template());
        return r;
    }

    InstanceGroupV2Request compute() {
        InstanceGroupV2Request r = new InstanceGroupV2Request();
        r.setNodeCount(1);
        r.setGroup("compute");
        r.setType(InstanceGroupType.CORE);
        SecurityGroupV2Request s = new SecurityGroupV2Request();
        s.setSecurityRules(rules());
        r.setSecurityGroup(s);
        r.setTemplate(template());
        return r;
    }

    InstanceGroupV2Request worker() {
        InstanceGroupV2Request r = new InstanceGroupV2Request();
        r.setNodeCount(1);
        r.setGroup("worker");
        r.setType(InstanceGroupType.CORE);
        SecurityGroupV2Request s = new SecurityGroupV2Request();
        s.setSecurityRules(rules());
        r.setSecurityGroup(s);
        r.setTemplate(template());
        return r;
    }

    abstract TemplateV2Request template();

    List<SecurityRuleRequest> rules() {
        List<SecurityRuleRequest> rules = new ArrayList<SecurityRuleRequest>();
        SecurityRuleRequest a = new SecurityRuleRequest();
        a.setSubnet("0.0.0.0/0");
        a.setProtocol("tcp");
        a.setPorts("22,443,8443,9443");
        rules.add(a);

        return rules;
    }

    public abstract String getProviderName();
}
