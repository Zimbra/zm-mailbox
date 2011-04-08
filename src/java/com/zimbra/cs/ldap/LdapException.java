package com.zimbra.cs.ldap;

import com.zimbra.common.service.ServiceException;

public class LdapException extends ServiceException {

    public static final String TODO = "ldap.TODO";
    
    public static final String INVALID_CONFIG = "ldap.INVALID_CONFIG";
    
    // generic LDAP error that is not mapped to a subclass of LdapException
    public static final String LDAP_ERROR = "ldap.LDAP_ERROR";
    
    protected LdapException(String message, String code, Throwable cause) {
        super(message, code, RECEIVERS_FAULT, cause);
    }
    
    public static LdapException TODO() {
        return new LdapException("TODO", TODO, null);
    }
    
    public static LdapException INVALID_CONFIG(Throwable cause) {
        return new LdapException("config error", INVALID_CONFIG,  cause);
    }
    
    public static LdapException LDAP_ERROR(Throwable cause) {
        return new LdapException("LDAP error", LDAP_ERROR,  cause);
    }
    
    public static LdapException LDAP_ERROR(String message, Throwable cause) {
        return new LdapException("LDAP error: " + message, LDAP_ERROR, cause);
    }
    

    //
    // Subclasses mapped to native(JNDI/UBID) ldap exceptions
    //
    public static class LdapInvalidNameException extends LdapException {
        public static final String INVALID_NAME = "ldap.INVALID_NAME";
        
        public LdapInvalidNameException(Throwable cause) {
            super("invalid name", INVALID_NAME, cause);
        }
    }
    
    public static class LdapNameNotFoundException extends LdapException {
        public static final String NAME_NOT_FOUND = "ldap.NAME_NOT_FOUND";
        
        public LdapNameNotFoundException(Throwable cause) {
            super("name not found", NAME_NOT_FOUND, cause);
        }
    }

    
}
