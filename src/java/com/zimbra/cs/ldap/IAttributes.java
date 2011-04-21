package com.zimbra.cs.ldap;

import java.util.List;

import com.zimbra.common.service.ServiceException;

public interface IAttributes {
    
    /**
     * - If a method does not have a CheckBinary parameter, it will *not* check 
     *   for binary data and binary transfer based on AttributeManager.
     *   It will assume all attributes are *not* binary.
     *   
     * - If a method has a CheckBinary parameter, it will check for binary data 
     *   and binary transfer based on AttributeManager if CheckBinary is CHECK.
     *   It will assume all attributes are *not* binary if CheckBinary is NOCHECK.
     */
    
    
    public String getAttrString(String attrName) throws ServiceException;
    
    public String[] getMultiAttrString(String attrName) throws ServiceException;
    
    public String[] getMultiAttrString(String attrName, 
            boolean containsBinaryData, boolean isBinaryTransfer) throws ServiceException;
    
    
    public static enum CheckBinary {
        CHECK,
        NOCHECK;
    }
    
    public List<String> getMultiAttrStringAsList(String attrName, CheckBinary checkBinary) throws ServiceException;

}
