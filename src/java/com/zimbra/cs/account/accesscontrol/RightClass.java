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
            throw ServiceException.INVALID_REQUEST("unknown right class: " + s, e);
        }
    }
    
    public static String allValuesInString(String delimiter) {
        StringBuffer sb = new StringBuffer();
        
        for (RightClass value : RightClass.values()) {
            if (sb.length() > 0) {
                sb.append(delimiter);
            }
            sb.append(value);
        }
        return sb.toString();
    }
    
}
