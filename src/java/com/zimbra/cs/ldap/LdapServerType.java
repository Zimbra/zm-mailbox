package com.zimbra.cs.ldap;

public enum LdapServerType {
    MASTER,
    REPLICA;
    
    public boolean isMaster() {
        return this == MASTER;
    }
}
