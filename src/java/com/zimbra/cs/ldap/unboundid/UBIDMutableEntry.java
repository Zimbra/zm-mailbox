package com.zimbra.cs.ldap.unboundid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;

import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.ldap.LdapTODO.*;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapUtilCommon;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZMutableEntry;


public class UBIDMutableEntry extends ZMutableEntry {

    private Entry entry;
    
    public UBIDMutableEntry() {
    }
    
    @Override
    public void debug() {
        // TODO Auto-generated method stub
        
    }
    
    Entry get() {
        return entry;
    }
    
    @Override  // ZEntry
    public ZAttributes getAttributes() {
        return new UBIDAttributes(entry);
    }

    @Override  // ZEntry
    public String getDN() {
     // TODO Auto-generated method stub
        return null;
    }
    
    @Override  // ZMutableEntry
    @TODO // verify replacement
    public void addAttr(String attrName, String value) {
        if (hasAttribute(attrName)) {
            entry.removeAttribute(attrName);
        }
        entry.addAttribute(attrName, value);
    }

    @Override  // ZMutableEntry
    @TODO // verify adding(merging)
    public void addAttr(String attrName, Set<String> values) {
        for (String value : values) {
            entry.addAttribute(attrName, value);
        }
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
