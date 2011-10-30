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
package com.zimbra.cs.ldap.unboundid;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.SearchResultEntry;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.ldap.ZAttributes;

public class UBIDAttributes extends ZAttributes {

    private static String[] EMPTY_STRING_ARRAY = new String[0];
    
    //
    // The wrapped object here is actually the SearchResultEntry or Entry object.
    // Unlike JNDI, unboundid handles attributes on the (SearchResult)Entry object.
    // For consistency with our existing coding pattern, the UBIDAttributes 
    // implementation just delegate all operations on the wrapped (SearchResult)Entry 
    // object.
    //
    private Entry entry;
    
    UBIDAttributes(SearchResultEntry entry) {
        this.entry = entry;
    }
    
    UBIDAttributes(Entry entry) {
        this.entry = entry;
    }
    
    @Override
    public void debug() {
        for (Attribute attr : entry.getAttributes()) {
            println(attr.toString());
        }
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

    @Override
    protected String getAttrString(String transferAttrName, boolean containsBinaryData) 
    throws LdapException {
        Attribute attr = entry.getAttribute(transferAttrName);
        
        if (attr != null) {
            return getAttrStringInternal(attr, containsBinaryData);
        } else {
            return null;
        }
    }
    
    @Override
    protected String[] getMultiAttrString(String transferAttrName, boolean containsBinaryData) 
    throws LdapException {
        Attribute attr = entry.getAttribute(transferAttrName);
        
        if (attr != null) {
            return getMultiAttrStringInternal(attr, containsBinaryData);
        } else {
            return EMPTY_STRING_ARRAY;
        }
    }

    @Override
    public Map<String, Object> getAttrs(Set<String> extraBinaryAttrs)
            throws LdapException {
        Map<String,Object> map = new HashMap<String,Object>();  
        
        AttributeManager attrMgr = AttributeManager.getInst();
        
        for (Attribute attr : entry.getAttributes()) {
            String transferAttrName = attr.getName();
            
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

    @Override
    public boolean hasAttribute(String attrName) {
        return entry.hasAttribute(attrName);
    }

    @Override
    public boolean hasAttributeValue(String attrName, String value) {
        return entry.hasAttributeValue(attrName, value);
    }



}
