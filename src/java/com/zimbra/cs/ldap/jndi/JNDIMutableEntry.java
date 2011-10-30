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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;

import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZMutableEntry;

public class JNDIMutableEntry extends ZMutableEntry {

    private String dn;
    private Attributes jndiAttrs;
    private ZAttributes zAttrs;
    
    @Override
    public void debug() {
        // TODO Auto-generated method stub
        
    }
    
    public JNDIMutableEntry() {
        jndiAttrs = new BasicAttributes(true);
        zAttrs = new JNDIAttributes(jndiAttrs);
    }
    
    @Override  // ZEntry
    public ZAttributes getAttributes() {
        return zAttrs;
    }
    
    Attributes getNativeAttributes() {
        return jndiAttrs;
    }

    @Override  // ZEntry
    public String getDN() {
        return dn;
    }
    

    @Override  // ZMutableEntry
    public void setAttr(String attrName, String value) {
        BasicAttribute attr = new BasicAttribute(attrName);
        attr.add(value);
        jndiAttrs.put(attr);
    }
    
    @Override  // ZMutableEntry
    public void addAttr(String attrName, Set<String> values) {
        Attribute attr = jndiAttrs.get(attrName);
        if (attr == null) {
            attr = new BasicAttribute(attrName);
            jndiAttrs.put(attr);
        }
        
        for (String value : values) {
            attr.add(value);
        }
    }
    

    @Override  // ZMutableEntry
    public String getAttrString(String attrName) throws LdapException {
        Attribute attr = jndiAttrs.get(attrName);
        if (attr == null) {
            return null;
        } else {
            try {
                return (String) attr.get();
            } catch (NamingException e) {
                throw JNDILdapException.mapToLdapException(e);
            }
        }
    }
    
    @Override  // ZMutableEntry
    public boolean hasAttribute(String attrName) {
        return jndiAttrs.get(attrName) != null;
    }

    @Override  // ZMutableEntry
    public void mapToAttrs(Map<String, Object> mapAttrs) {
        AttributeManager attrMgr = AttributeManager.getInst();
        
        for (Map.Entry<String, Object> me : mapAttrs.entrySet()) {
                        
            String attrName = me.getKey();
            Object v = me.getValue();
            
            boolean containsBinaryData = attrMgr == null ? false : attrMgr.containsBinaryData(attrName);
            boolean isBinaryTransfer = attrMgr == null ? false : attrMgr.isBinaryTransfer(attrName);
            
            if (v instanceof String) {
                BasicAttribute a = JNDIUtil.newAttribute(isBinaryTransfer, attrName);
                a.add(LdapUtil.decodeBase64IfBinary(containsBinaryData, (String)v));
                jndiAttrs.put(a);
            } else if (v instanceof String[]) {
                String[] sa = (String[]) v;
                BasicAttribute a = JNDIUtil.newAttribute(isBinaryTransfer, attrName);
                for (int i=0; i < sa.length; i++) {
                    a.add(LdapUtil.decodeBase64IfBinary(containsBinaryData, sa[i]));
                }
                jndiAttrs.put(a);
            } else if (v instanceof Collection) {
                Collection c = (Collection) v;
                BasicAttribute a = JNDIUtil.newAttribute(isBinaryTransfer, attrName);
                for (Object o : c) {
                    a.add(LdapUtil.decodeBase64IfBinary(containsBinaryData, o.toString()));
                }
                jndiAttrs.put(a);
            }
        }
    }

    @Override
    public void setDN(String dn) {
        this.dn = dn;
    }


}
