package com.zimbra.cs.ldap.jndi;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapTODO.TODO;
import com.zimbra.cs.ldap.LdapUtilCommon;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZSearchResultEntry;

public class JNDISearchResultEntry extends ZSearchResultEntry {

    SearchResult wrapped;
    JNDIAttributes zAttributes;
    
    JNDISearchResultEntry(SearchResult searchResult) {
        wrapped = searchResult;
        zAttributes = new JNDIAttributes(wrapped.getAttributes());
    }

    private Attributes getNativeAttributes() {
        return wrapped.getAttributes();
    }
    
    @Override
    @TODO
    public void debug() {
        println(wrapped.toString());
    }
    
    // attr must not be null
    private String getAttrStringInternal(Attribute attr, boolean containsBinaryData) throws NamingException {
        Object o = attr.get();
        if (o instanceof String) {
            return (String) o;
        } else if (containsBinaryData) {
            return ByteUtil.encodeLDAPBase64((byte[])o);
        } else {
            return new String((byte[])o);
        }
    }
    
    // attr must not be null
    private String[] getMultiAttrStringInternal(Attribute attr, boolean containsBinaryData) throws NamingException {
        String result[] = new String[attr.size()];
        for (int i=0; i < attr.size(); i++) {
            Object o = attr.get(i);
            if (o instanceof String) {
                result[i] = (String) o;
            } else if (containsBinaryData) {
                result[i] = ByteUtil.encodeLDAPBase64((byte[])o);
            } else {
                result[i] = new String((byte[])o);
            }
        }
        return result;
    }
    
    private Map<String, Object> getAttrsInternal(Set<String> binaryAttrs) throws NamingException {
        Map<String,Object> map = new HashMap<String,Object>();  
        
        AttributeManager attrMgr = AttributeManager.getInst();
        
        for (NamingEnumeration ne = getNativeAttributes().getAll(); ne.hasMore(); ) {
            Attribute attr = (Attribute) ne.next();
            String transferAttrName = attr.getID();
            
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
    
    @Override
    public ZAttributes getAttributes() {
        return zAttributes;
    }

    @Override
    public String getDN() {
        return wrapped.getNameInNamespace();
    }
    
    @Override
    public String getAttrString(String attrName, boolean containsBinaryData) throws LdapException {
        try {
            Attribute attr = getNativeAttributes().get(attrName);
            if (attr != null) {
                return getAttrStringInternal(attr, containsBinaryData);
            } else {
                return null;
            }
        } catch (NamingException e) {
            throw LdapException.LDAP_ERROR(e);
        }
    }
    
    @Override
    public List<String> getMultiAttrString(String attrName) throws LdapException {
        try {
            Attribute attr = getNativeAttributes().get(attrName);
            if (attr != null) {
                // Note: this call does not handle binary data
                String[] values = getMultiAttrStringInternal(attr, false); 
                return Arrays.asList(values);
            } else {
                return null;
            }
        } catch (NamingException e) {
            throw LdapException.LDAP_ERROR(e);
        }
    }

    @Override
    public Map<String, Object> getAttrs(Set<String> binaryAttrs) throws LdapException {
        // wrapper to convert NamingException to LdapException
        try {
            return getAttrsInternal(binaryAttrs);
        } catch (NamingException e) {
            throw LdapException.LDAP_ERROR(e);
        }
    }


}
