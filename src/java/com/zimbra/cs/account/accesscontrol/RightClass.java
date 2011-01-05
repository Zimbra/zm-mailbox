package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.service.ServiceException;

public enum RightClass {
    ALL,
    ADMIN,
    USER;
    
    public static RightClass fromString(String s) throws ServiceException {
        try {
            return RightClass.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw ServiceException.INVALID_REQUEST("unknown right category: " + s, e);
        }
    }
    
}
