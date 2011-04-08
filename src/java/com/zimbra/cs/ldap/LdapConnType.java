package com.zimbra.cs.ldap;

public enum LdapConnType {
    PLAIN,
    LDAPS,
    STARTTLS;
    
    public static LdapConnType getConnType(String urls, boolean wantStartTLS) {
        if (urls.toLowerCase().contains("ldaps://")) {
            return LDAPS;
        } else if (wantStartTLS) {
            return STARTTLS;
        } else {
            return PLAIN;
        }
    }
}
