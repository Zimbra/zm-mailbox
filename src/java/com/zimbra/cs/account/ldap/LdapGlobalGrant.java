package com.zimbra.cs.account.ldap;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import com.zimbra.cs.account.GlobalGrant;
import com.zimbra.cs.account.Provisioning;

class LdapGlobalGrant extends GlobalGrant implements LdapEntry {
    
    private String mDn;
    
    LdapGlobalGrant(String dn, Attributes attrs, Provisioning provisioning) throws NamingException {
        super(LdapUtil.getAttrs(attrs), provisioning);
        mDn = dn;
    }

    public String getDN() {
        return mDn;
    }
}