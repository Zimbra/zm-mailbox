package com.zimbra.cs.ldap;

public abstract class ZEntry extends ZLdapElement {
    public abstract String getDN();
    
    public abstract ZAttributes getAttributes();
}
