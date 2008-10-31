package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.service.ServiceException;

public enum TargetType {
    account(true),
    resource(true),
    distributionlist(true),
    domain(true),
    cos(true),
    right(true),
    server(true),
    config(false),
    global(false);
    
    private boolean mNeedsTargetIdentity;
    
    TargetType(boolean NeedsTargetIdentity) {
        mNeedsTargetIdentity = NeedsTargetIdentity;
    }
    
    public static TargetType fromString(String s) throws ServiceException {
        try {
            return TargetType.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw ServiceException.INVALID_REQUEST("unknown target type: " + s, e);
        }
    }
    
    public String getCode() {
        return name();
    }
    
    public boolean needsTargetIdentity() {
        return mNeedsTargetIdentity;
    }

}
