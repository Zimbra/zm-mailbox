package com.zimbra.cs.ldap;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.cs.account.AttributeManager;

public abstract class ZAttributes extends ZLdapElement implements IAttributes {
    
    /**
     * - if a method does not have a CheckBinary parameter, it will check for binary data 
     *   and binary transfer based on AttributeManager.
     *   
     * - if a method has a CheckBinary parameter, it will check for binary data 
     *   and binary transfer based on AttributeManager if CheckBinary is CHECK.
     *   It will assume all attributes are *not* binary if CheckBinary is NOCHECK.
     *   
     */
    
    @Override
    public String getAttrString(String attrName) throws LdapException {
        AttributeManager attrMgr = AttributeManager.getInst();
        boolean containsBinaryData = attrMgr == null ? false : attrMgr.containsBinaryData(attrName);
        boolean isBinaryTransfer = attrMgr == null ? false : attrMgr.isBinaryTransfer(attrName);
        
        String transferAttrName = LdapUtilCommon.attrNameToBinaryTransferAttrName(isBinaryTransfer, attrName);
        
        return getAttrString(transferAttrName, containsBinaryData);
    }
    
    @Override
    public String[] getMultiAttrString(String attrName) throws LdapException {
        AttributeManager attrMgr = AttributeManager.getInst();
        boolean containsBinaryData = attrMgr == null ? false : attrMgr.containsBinaryData(attrName);
        boolean isBinaryTransfer = attrMgr == null ? false : attrMgr.isBinaryTransfer(attrName);
        return getMultiAttrString(attrName, containsBinaryData, isBinaryTransfer);
    }
    
    @Override
    public String[] getMultiAttrString(String attrName, boolean containsBinaryData, boolean isBinaryTransfer) 
    throws LdapException {
        String transferAttrName = LdapUtilCommon.attrNameToBinaryTransferAttrName(isBinaryTransfer, attrName);
        return getMultiAttrString(transferAttrName, containsBinaryData);
    }
    
    @Override
    public List<String> getMultiAttrStringAsList(String attrName, CheckBinary checkBinary) throws LdapException {
        if (checkBinary == CheckBinary.NOCHECK) {
            return Arrays.asList(getMultiAttrString(attrName, false));
        } else {
            return Arrays.asList(getMultiAttrString(attrName));
        }
    }
    
    /**
     * Enumerates over the specified attributes and populates the specified map. The key in the map is the
     * attribute ID. For attrs with a single value, the value is a String, and for attrs with multiple values
     * the value is an array of Strings.
     */
    public Map<String, Object> getAttrs() throws LdapException {
        return getAttrs(null);
    }
    
    /**
     * extraBinaryAttrs: if not null, attrs in the set are treated as binary attrs, in addition to 
     * those marked binary in Zimbra's AttributeManager.
     */
    public abstract Map<String, Object> getAttrs(Set<String> extraBinaryAttrs) throws LdapException;
    

    
    /**
     * Retrieves the value for this attribute as a string. 
     * If this attribute has multiple values, then the first value will be returned.
     */
    protected abstract String getAttrString(String transferAttrName, boolean containsBinaryData) throws LdapException;
    
    protected abstract String[] getMultiAttrString(String transferAttrName, boolean containsBinaryData) throws LdapException;
    
}
