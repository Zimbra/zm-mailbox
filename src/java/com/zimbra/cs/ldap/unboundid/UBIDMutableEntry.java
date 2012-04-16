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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;

import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZMutableEntry;

/**
 * @author pshao
  */
public class UBIDMutableEntry extends ZMutableEntry {

    private Entry entry;
    
    public UBIDMutableEntry() {
        // give a bogus dn ("") since unboundid requires it
        // call site should set a real DN later
        this.entry = new Entry("");
    }

    @Override
    public void debug(ZLdapElementDebugListener debugListener) {
        print(debugListener, entry.toString());
    }
    
    Entry getNative() {
        return entry;
    }
    
    @Override  // ZEntry
    public ZAttributes getAttributes() {
        return new UBIDAttributes(entry);
    }

    @Override  // ZEntry
    public String getDN() {
        return entry.getDN();
    }
    
    @Override  // ZMutableEntry
    public void setAttr(String attrName, String value) {
        if (hasAttribute(attrName)) {
            entry.removeAttribute(attrName);
        }
        entry.addAttribute(attrName, value);
    }

    @Override  // ZMutableEntry
    public void addAttr(String attrName, Set<String> values) {
        for (String value : values) {
            entry.addAttribute(attrName, value);
        }
    }
    
    @Override  // ZMutableEntry
    public void addAttr(String attrName, String value) {
        entry.addAttribute(attrName, value);
    }
    
    @Override  // ZMutableEntry
    public String getAttrString(String attrName)  throws LdapException {
        return entry.getAttributeValue(attrName);
    }
    
    @Override  // ZMutableEntry
    public boolean hasAttribute(String attrName) {
        return entry.hasAttribute(attrName);
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
                ASN1OctetString value = UBIDUtil.newASN1OctetString(containsBinaryData, (String) v);
                Attribute a = UBIDUtil.newAttribute(isBinaryTransfer, attrName, value);
                entry.addAttribute(a);
            } else if (v instanceof String[]) {
                String[] sa = (String[]) v;
                ASN1OctetString[] values = new ASN1OctetString[sa.length];
                for (int i=0; i < sa.length; i++) {
                    values[i] = UBIDUtil.newASN1OctetString(containsBinaryData, sa[i]);
                }
                Attribute a = UBIDUtil.newAttribute(isBinaryTransfer, attrName, values);
                entry.addAttribute(a);
            } else if (v instanceof Collection) {
                Collection c = (Collection) v;
                ASN1OctetString[] values = new ASN1OctetString[c.size()];
                int i = 0;
                for (Object o : c) {
                    values[i] = UBIDUtil.newASN1OctetString(containsBinaryData, o.toString());
                    i++;
                }
                Attribute a = UBIDUtil.newAttribute(isBinaryTransfer, attrName, values);
                entry.addAttribute(a);
            }
        }
    }

    @Override
    public void setDN(String dn) {
        entry.setDN(dn);
    }

}
