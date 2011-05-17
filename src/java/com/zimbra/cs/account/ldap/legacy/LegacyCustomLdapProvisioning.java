package com.zimbra.cs.account.ldap.legacy;

import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.ldap.custom.CustomLdapDIT;

public class LegacyCustomLdapProvisioning extends LdapProvisioning {
    
    protected void setDIT() {
        mDIT = new CustomLdapDIT(this);
    }

}
