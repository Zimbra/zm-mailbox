package com.zimbra.cs.account.ldap;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Right;
import com.zimbra.cs.account.Provisioning;

class LdapRight extends Right implements LdapEntry {

    private String mDn;

    LdapRight(String dn, Attributes attrs, Provisioning prov) throws NamingException, ServiceException {
        super(LdapUtil.getAttrString(attrs, Provisioning.A_cn), LdapUtil.getAttrString(attrs, Provisioning.A_zimbraId), LdapUtil.getAttrs(attrs), prov);
        mDn = dn;
    }

    public String getDN() {
        return mDn; 
    }
}

