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
import com.zimbra.cs.ldap.LdapUtilCommon;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZTransientEntry;

public class JNDITransientEntry extends ZTransientEntry {

    private Attributes attributes;
    
    @Override
    public void debug() {
        // TODO Auto-generated method stub
        
    }
    
    public JNDITransientEntry() {
        attributes = new BasicAttributes(true);
    }
    
    @Override  // ZEntry
    public ZAttributes getAttributes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override  // ZEntry
    public String getDN() {
        // TODO Auto-generated method stub
        return null;
    }
    

    @Override  // ZTransientEntry
    public void addAttr(String attrName, String value) {
        BasicAttribute attr = new BasicAttribute(attrName);
        attr.add(value);
        attributes.put(attr);
    }
    
    @Override  // ZTransientEntry
    public void addAttr(String attrName, Set<String> values) {
        Attribute attr = attributes.get(attrName);
        if (attr == null) {
            attr = new BasicAttribute(attrName);
            attributes.put(attr);
        }
        
        for (String value : values) {
            attr.add(value);
        }
    }
    

    @Override  // ZTransientEntry
    public String getAttrString(String attrName) throws LdapException {
        Attribute attr = attributes.get(attrName);
        if (attr == null) {
            return null;
        } else {
            try {
                return (String) attr.get();
            } catch (NamingException e) {
                throw LdapException.LDAP_ERROR(e);
            }
        }
    }
    
    @Override  // ZTransientEntry
    public boolean hasAttribute(String attrName) {
        return attributes.get(attrName) != null;
    }

    @Override  // ZTransientEntry
    public void mapToAttrs(Map<String, Object> mapAttrs) {
        AttributeManager attrMgr = AttributeManager.getInst();
        
        for (Map.Entry<String, Object> me : mapAttrs.entrySet()) {
                        
            String attrName = me.getKey();
            Object v = me.getValue();
            
            boolean containsBinaryData = attrMgr == null ? false : attrMgr.containsBinaryData(attrName);
            boolean isBinaryTransfer = attrMgr == null ? false : attrMgr.isBinaryTransfer(attrName);
            
            if (v instanceof String) {
                BasicAttribute a = JNDIUtil.newAttribute(isBinaryTransfer, attrName);
                a.add(LdapUtilCommon.decodeBase64IfBinary(containsBinaryData, (String)v));
                attributes.put(a);
            } else if (v instanceof String[]) {
                String[] sa = (String[]) v;
                BasicAttribute a = JNDIUtil.newAttribute(isBinaryTransfer, attrName);
                for (int i=0; i < sa.length; i++) {
                    a.add(LdapUtilCommon.decodeBase64IfBinary(containsBinaryData, sa[i]));
                }
                attributes.put(a);
            } else if (v instanceof Collection) {
                Collection c = (Collection) v;
                BasicAttribute a = JNDIUtil.newAttribute(isBinaryTransfer, attrName);
                for (Object o : c) {
                    a.add(LdapUtilCommon.decodeBase64IfBinary(containsBinaryData, o.toString()));
                }
                attributes.put(a);
            }
        }
    }


}
