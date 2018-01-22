package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.model.SecurityRuleRequest;
import com.sequenceiq.cloudbreak.api.model.SecurityRulesResponse;

public class SecurityRulesEntity extends AbstractCloudbreakEntity<SecurityRuleRequest, SecurityRulesResponse> {
    public static final String SECURITY_RULES = "SECURITY_RULES";

    SecurityRulesEntity(String newId) {
        super(newId);
        setRequest(new SecurityRuleRequest());
    }

    SecurityRulesEntity() {
        this(SECURITY_RULES);
    }

    public SecurityRulesEntity withModifiable(boolean mod) {
        getRequest().setModifiable(mod);
        return this;
    }

    public SecurityRulesEntity withPorts(String ports) {
        getRequest().setPorts(ports);
        return this;
    }

    public SecurityRulesEntity withProtocol(String protocol) {
        getRequest().setProtocol(protocol);
        return this;
    }

    public SecurityRulesEntity withSubnet(String subnet) {
        getRequest().setSubnet(subnet);
        return this;
    }
}
