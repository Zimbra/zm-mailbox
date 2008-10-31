package com.zimbra.cs.account;

import java.util.Map;

public class GlobalGrant extends Entry {
    
    public GlobalGrant(Map<String, Object> attrs) {
        super(attrs, null);
        resetData();
    }
    
    public String getLabel() {
        return "globalacltarget";
    }
}
