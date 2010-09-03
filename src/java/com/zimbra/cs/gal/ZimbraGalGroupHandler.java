package com.zimbra.cs.gal;

import java.util.HashMap;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapProvisioning;

public class ZimbraGalGroupHandler implements GalGroupHandler {

    //Override
    public boolean isGroup(Attributes ldapAttrs) {
        Attribute objectclass = ldapAttrs.get(Provisioning.A_objectClass);
        return objectclass.contains(LdapProvisioning.C_zimbraMailList);
    }
}
