package com.zimbra.cs.ldap.unboundid;

import java.util.Collection;

import com.unboundid.ldap.sdk.Attribute;

import com.zimbra.cs.ldap.LdapTODO;
import com.zimbra.cs.ldap.LdapTODO.TODO;
import com.zimbra.cs.ldap.ZAttributes;

public class UBIDAttributes extends ZAttributes {

    private Collection<Attribute> wrapped;
    
    UBIDAttributes(Collection<Attribute> attributes) {
        wrapped = attributes;
    }
    
    @Override
    public void debug() {
        for (Attribute attr : wrapped) {
            println(attr.toString());
        }
    }

}
