package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.model.PlatformResourceRequestJson;
import com.sequenceiq.cloudbreak.api.model.PlatformSecurityGroupResponse;

import java.util.Map;
import java.util.Set;

public class SecurityGroupEntity extends AbstractCloudbreakEntity<PlatformResourceRequestJson, Map<String, Set<PlatformSecurityGroupResponse>>> {
    public static final String SECURITY_GROUP = "SECURITY_GROUP";

    SecurityGroupEntity(String newId) {
        super(newId);
        setRequest(new PlatformResourceRequestJson());
    }

    SecurityGroupEntity() {
        this(SECURITY_GROUP);
    }

    public SecurityGroupEntity withCredentialId(Long id) {
        getRequest().setCredentialId(id);
        return this;
    }

    public SecurityGroupEntity withCredentialName(String name) {
        getRequest().setCredentialName(name);
        return this;
    }

    public SecurityGroupEntity withAvailabilityZone(String availabilityZone) {
        getRequest().setAvailabilityZone(availabilityZone);
        return this;
    }

    public SecurityGroupEntity withRegion(String region) {
        getRequest().setRegion(region);
        return this;
    }
}
