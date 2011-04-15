package com.zimbra.cs.ldap;

import java.util.List;

import com.zimbra.common.service.ServiceException;

public interface IAttributes {
    
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
