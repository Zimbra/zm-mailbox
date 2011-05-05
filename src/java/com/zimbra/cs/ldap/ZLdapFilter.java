package com.zimbra.cs.ldap;


public abstract class ZLdapFilter extends ZLdapElement {
    
    public abstract String toFilterString();
}
