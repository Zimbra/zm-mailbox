package com.zimbra.cs.prov.ldap;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapDIT;

public abstract class LdapProv extends Provisioning {
    
    protected LdapDIT mDIT;
    
    protected void setDIT() {
        mDIT = new LdapDIT(this);
    }

    public LdapDIT getDIT() {
        return mDIT;
    }

}
