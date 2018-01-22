package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.model.PlatformResourceRequestJson;
import com.sequenceiq.cloudbreak.api.model.PlatformSshKeyResponse;

import java.util.Map;
import java.util.Set;

public class SshKeyEntity extends AbstractCloudbreakEntity<PlatformResourceRequestJson, Map<String, Set<PlatformSshKeyResponse>>> {
    public static final String CREDENTIAL = "CREDENTIAL";

    SshKeyEntity(String newId) {
        super(newId);
        setRequest(new PlatformResourceRequestJson());
    }

    SshKeyEntity() {
        this(CREDENTIAL);
    }

    public SshKeyEntity withCredentialId(Long id) {
        getRequest().setCredentialId(id);
        return this;
    }

    public SshKeyEntity withCredentialName(String name) {
        getRequest().setCredentialName(name);
        return this;
    }

    public SshKeyEntity withAvailabilityZone(String availabilityZone) {
        getRequest().setAvailabilityZone(availabilityZone);
        return this;
    }

    public SshKeyEntity withRegion(String region) {
        getRequest().setRegion(region);
        return this;
    }
}
