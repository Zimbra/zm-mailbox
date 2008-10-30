package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.service.ServiceException;

public enum TargetType {
    account,
    config,
    resource,
    distributionlist,
    domain,
    cos,
    right,
    server,
    allcos,
    alldomains,
    allrights,
    allservers;
    
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
}
