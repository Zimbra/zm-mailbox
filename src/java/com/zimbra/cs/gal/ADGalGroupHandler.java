package com.zimbra.cs.gal;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import com.zimbra.cs.account.Provisioning;

public class ADGalGroupHandler implements GalGroupHandler {

    //Override
    public boolean isGroup(Attributes ldapAttrs) {
        Attribute objectclass = ldapAttrs.get(Provisioning.A_objectClass);
        return objectclass.contains("group");
    }

}
