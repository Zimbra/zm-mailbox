package com.zimbra.cs.ldap.jndi;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

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
}
