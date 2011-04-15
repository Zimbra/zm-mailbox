package com.zimbra.cs.ldap;

public abstract class ZSearchResultEntry extends ZLdapElement {

    public abstract String getDN();
    
    public abstract ZAttributes getAttributes();

}
