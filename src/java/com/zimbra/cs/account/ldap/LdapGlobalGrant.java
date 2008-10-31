package com.zimbra.cs.account.ldap;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import com.zimbra.cs.account.GlobalGrant;

class LdapGlobalGrant extends GlobalGrant implements LdapEntry {
    
    private String mDn;
    
    LdapGlobalGrant(String dn, Attributes attrs) throws NamingException {
        super(LdapUtil.getAttrs(attrs));
        mDn = dn;
    }

    public String getDN() {
        return mDn;
    }
}