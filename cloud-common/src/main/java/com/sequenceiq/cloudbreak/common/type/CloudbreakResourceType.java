package com.sequenceiq.cloudbreak.common.type;

import java.util.HashMap;
import java.util.Map;

public enum CloudbreakResourceType {

    NETWORK("network_resource", "network"),
    INSTANCE("instance_resource", "instance"),
    SECURITY("securitygroup_resource", "securitygroup"),
    IP("ipaddress_resource", "ipaddress"),
    DISK("disk_resource", "disk"),
    STORAGE("storage_resource", "storage");

    private final String key;
    private final String templateVariable;

    CloudbreakResourceType(String templateVariable, String key) {
        this.key = key;
        this.templateVariable = templateVariable;
    }

    public String key() {
        return key;
    }

    public String templateVariable() {
        return templateVariable;
    }

    public static Map<String, Object> cloudbreakResourceTypes() {
        Map<String, Object> values = new HashMap<>();
        for (CloudbreakResourceType cloudbreakResourceType : values()) {
            values.put(cloudbreakResourceType.templateVariable(), cloudbreakResourceType.key());
        }
        return values;
    }
}
