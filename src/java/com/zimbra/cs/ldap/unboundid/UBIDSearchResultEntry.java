package com.zimbra.cs.ldap.unboundid;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.SearchResultEntry;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapUtilCommon;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZSearchResultEntry;

/**
 * Represents one LDAP entry in a search result.
 *
 */
public class UBIDSearchResultEntry extends ZSearchResultEntry  {

    private SearchResultEntry wrapped;
    private UBIDAttributes zAttributes;
    
    UBIDSearchResultEntry(SearchResultEntry searchResultEntry) {
        wrapped = searchResultEntry;
        zAttributes = new UBIDAttributes(wrapped.getAttributes());
    }

    @Override
    public void debug() {
        println(wrapped.toString());
    }
    
    private String getAttrStringInternal(Attribute attr, boolean containsBinaryData) {
        if (containsBinaryData) {
            // Retrieves the value for this attribute as a byte array. 
            // If this attribute has multiple values, then the first value will be returned.
            byte[] bytes = attr.getValueByteArray();
            return ByteUtil.encodeLDAPBase64(bytes);
        } else {
            // Retrieves the value for this attribute as a string. 
            // If this attribute has multiple values, then the first value will be returned.
            return attr.getValue();
        }
    }
    
    private String[] getMultiAttrStringInternal(Attribute attr, boolean containsBinaryData) {
        String result[] = new String[attr.size()];
        
        if (containsBinaryData) {
            byte[][] bytesArrays = attr.getValueByteArrays();
            for (int i = 0; i < bytesArrays.length; i++) {
                result[i] = ByteUtil.encodeLDAPBase64(bytesArrays[i]);
            }
        } else {
            String[] values = attr.getValues();
            for (int i = 0; i < values.length; i++) {
                result[i] = values[i];
            }
        }
        return result;
    }
    
    private Collection<Attribute> getNativeAttributes() {
        return wrapped.getAttributes();
    }
    
    @Override
    public ZAttributes getAttributes() {
        return zAttributes;
    }

    @Override
    public String getDN() {
        return wrapped.getDN();
    }

    @Override
    public String getAttrString(String attrName, boolean containsBinaryData)
            throws LdapException {
        Attribute attr = wrapped.getAttribute(attrName);
        
        if (attr != null) {
            return getAttrStringInternal(attr, containsBinaryData);
        } else {
            return null;
        }
    }
    
    @Override
    public List<String> getMultiAttrString(String attrName) throws LdapException {
        Attribute attr = wrapped.getAttribute(attrName);
        
        if (attr != null) {
            // Note: this call does not handle binary data
            String[] values = getMultiAttrStringInternal(attr, false);
            return Arrays.asList(values);
        } else {
            return null;
        }
    }

    @Override
    public Map<String, Object> getAttrs(Set<String> binaryAttrs) throws LdapException {
        Map<String,Object> map = new HashMap<String,Object>();  
        
        AttributeManager attrMgr = AttributeManager.getInst();
        
        for (Attribute attr : getNativeAttributes()) {
            String transferAttrName = attr.getName();
            
            String attrName = LdapUtilCommon.binaryTransferAttrNameToAttrName(transferAttrName);
            
            boolean containsBinaryData = 
                (attrMgr != null && attrMgr.containsBinaryData(attrName)) ||
                (binaryAttrs != null && binaryAttrs.contains(attrName));
            
            if (attr.size() == 1) {
                map.put(attrName, getAttrStringInternal(attr, containsBinaryData));
            } else {
                String result[] = getMultiAttrStringInternal(attr, containsBinaryData);
                map.put(attrName, result);
            }
        }
        return map;
    }


}
