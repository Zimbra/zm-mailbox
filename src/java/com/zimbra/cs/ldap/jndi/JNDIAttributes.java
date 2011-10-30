/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.ldap.jndi;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.ldap.ZAttributes;

public class JNDIAttributes extends ZAttributes {
    
    private Attributes attributes;
    private static String[] EMPTY_STRING_ARRAY = new String[0];
    
    JNDIAttributes(Attributes attributes) {
        this.attributes = attributes;
    }
    
    public void debug() {
        try {
            for (NamingEnumeration ne = attributes.getAll(); ne.hasMore(); ) {
                Attribute attr = (Attribute) ne.next();
                println(attr.toString());
            }
        } catch (NamingException e) {
            printStackTrace(e);
        }
    }
    
    Attributes getNative() {
        return attributes;
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
    
    private Map<String, Object> getAttrsInternal(Set<String> extraBinaryAttrs) throws NamingException {
        Map<String,Object> map = new HashMap<String,Object>();  
        
        AttributeManager attrMgr = AttributeManager.getInst();
        
        for (NamingEnumeration ne = attributes.getAll(); ne.hasMore(); ) {
            Attribute attr = (Attribute) ne.next();
            String transferAttrName = attr.getID();
            
            String attrName = LdapUtil.binaryTransferAttrNameToAttrName(transferAttrName);
            
            boolean containsBinaryData = 
                (attrMgr != null && attrMgr.containsBinaryData(attrName)) ||
                (extraBinaryAttrs != null && extraBinaryAttrs.contains(attrName));
            
            if (attr.size() == 1) {
                map.put(attrName, getAttrStringInternal(attr, containsBinaryData));
            } else {
                String result[] = getMultiAttrStringInternal(attr, containsBinaryData);
                map.put(attrName, result);
            }
        }
        return map;
    }
    
    
    @Override  // IAttributes
    protected String getAttrString(String transferAttrName, boolean containsBinaryData) throws LdapException {
        try {
            Attribute attr = attributes.get(transferAttrName);
            if (attr != null) {
                return getAttrStringInternal(attr, containsBinaryData);
            } else {
                return null;
            }
        } catch (NamingException e) {
            throw JNDILdapException.mapToLdapException(e);
        }
    }
    
    @Override  // IAttributes
    protected String[] getMultiAttrString(String transferAttrName, boolean containsBinaryData) throws LdapException {
        try {
            Attribute attr = attributes.get(transferAttrName);
            if (attr != null) {
                return getMultiAttrStringInternal(attr, containsBinaryData); 
            } else {
                return EMPTY_STRING_ARRAY;
            }
        } catch (NamingException e) {
            throw JNDILdapException.mapToLdapException(e);
        }
    }

    @Override  // ZAttributes
    public Map<String, Object> getAttrs(Set<String> extraBinaryAttrs) throws LdapException {
        try {
            return getAttrsInternal(extraBinaryAttrs);
        } catch (NamingException e) {
            throw JNDILdapException.mapToLdapException(e);
        }
    }

    @Override
    public boolean hasAttribute(String attrName) {
        return attributes.get(attrName) != null;
    }

    @Override
    public boolean hasAttributeValue(String attrName, String value) {
        Attribute attr = attributes.get(attrName);
        return attr.contains(value);
    }
    
}
