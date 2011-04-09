package com.zimbra.cs.ldap.jndi;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.ZAttributes;

public class JNDIAttributes extends ZAttributes {
    
    private Attributes wrapped;
    
    JNDIAttributes(Attributes attributes) {
        wrapped = attributes;
    }
    
    public void debug() {
        try {
            for (NamingEnumeration ne = wrapped.getAll(); ne.hasMore(); ) {
                Attribute attr = (Attribute) ne.next();
                println(attr.toString());
            }
        } catch (NamingException e) {
            printStackTrace(e);
        }
    }
    
    private Object getAttrValue(Attribute attr) throws LdapException {
        try {
            return attr.get();
        } catch (NamingException e) {
            throw LdapException.LDAP_ERROR(e);
        }
    }
    
    String getAttrString(String attrName, boolean containsBinaryData) throws LdapException {
        
        Attribute attr = wrapped.get(attrName);
        
        if (attr == null) {
            return null;
        }
        
        Object o = getAttrValue(attr);
        if (o instanceof String) {
            return (String) o;
        } else if (containsBinaryData) {
            return ByteUtil.encodeLDAPBase64((byte[])o);
        } else {
            return new String((byte[])o);
        }
    }

}
