package com.zimbra.cs.account.ldap.legacy;

import com.zimbra.cs.account.ldap.custom.CustomLdapDIT;

public class LegacyCustomLdapProvisioning extends LegacyLdapProvisioning {
    
    protected void setDIT() {
        mDIT = new CustomLdapDIT(this);
    }

}
