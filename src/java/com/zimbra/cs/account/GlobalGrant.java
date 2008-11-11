package com.zimbra.cs.account;

import java.util.Map;

public class GlobalGrant extends Entry {
    
    public GlobalGrant(Map<String, Object> attrs, Provisioning provisioning) {
        super(attrs, null, provisioning);
        resetData();
    }
    
    public String getLabel() {
        return "globalacltarget";
    }
}
