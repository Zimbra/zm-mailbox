package com.zimbra.cs.account.gal;

import com.zimbra.common.service.ServiceException;

public enum GalOp {
    autocomplete,
    search,
    sync;
    
    public static GalOp fromString(String s) throws ServiceException {
        try {
            return GalOp.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw ServiceException.INVALID_REQUEST("invalid GAL op: "+s, e);
        }
    }
}
