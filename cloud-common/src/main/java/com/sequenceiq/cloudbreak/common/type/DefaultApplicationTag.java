package com.sequenceiq.cloudbreak.common.type;

public enum DefaultApplicationTag {

    CB_USER_NAME("cb_user_name"),
    CB_ACOUNT_NAME("cb_account_name"),
    CB_RESOURCE_TYPE("cb_resource_type");

    private final String key;

    DefaultApplicationTag(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
