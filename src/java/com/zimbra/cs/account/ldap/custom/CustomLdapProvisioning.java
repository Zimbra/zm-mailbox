package com.zimbra.cs.account.ldap.custom;

import com.zimbra.cs.account.ldap.LdapProvisioning;

public class CustomLdapProvisioning extends LdapProvisioning {
    
    protected void setDIT() {
        mDIT = new CustomLdapDIT(this);
    }

}
