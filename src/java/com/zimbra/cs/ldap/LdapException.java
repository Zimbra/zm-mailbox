package com.zimbra.cs.ldap;

import com.zimbra.common.service.ServiceException;

public class LdapException extends ServiceException {

    public static final String TODO = "ldap.TODO";
    
    public static final String INVALID_CONFIG = "ldap.INVALID_CONFIG";
    
    // generic LDAP error that is not mapped to a subclass of LdapException
    public static final String LDAP_ERROR = "ldap.LDAP_ERROR";
    
    public static final String CONTEXT_NOT_EMPTY = LdapContextNotEmptyException.CONTEXT_NOT_EMPTY;
    public static final String INVALID_NAME = LdapInvalidNameException.INVALID_NAME;
    public static final String NAME_ALREADY_EXIST = LdapNameAlreadyExistException.NAME_ALREADY_EXIST;
    public static final String NAME_NOT_FOUND = LdapNameNotFoundException.NAME_NOT_FOUND;
    
    protected LdapException(String message, String code, Throwable cause) {
        super(message, code, RECEIVERS_FAULT, cause);
    }
    
    public static LdapException TODO() {
        return new LdapException("TODO", TODO, null);
    }
    
    public static LdapException INVALID_CONFIG(Throwable cause) {
        return new LdapException("config error", INVALID_CONFIG,  cause);
    }
    
    // generic LDAP error
    public static LdapException LDAP_ERROR(Throwable cause) {
        return new LdapException("LDAP error", LDAP_ERROR,  cause);
    }
    
    // generic LDAP error
    public static LdapException LDAP_ERROR(String message, Throwable cause) {
        return new LdapException("LDAP error: " + message, LDAP_ERROR, cause);
    }
    
    //
    // Specific LDAP errors needs handling
    //
    public static LdapException CONTEXT_NOT_EMPTY(Throwable cause) {
        return new LdapContextNotEmptyException(cause);
    }
    
    public static LdapException INVALID_NAME(Throwable cause) {
        return new LdapInvalidNameException(cause);
    }
    
    public static LdapException NAME_ALREADY_EXIST(Throwable cause) {
        return new LdapNameAlreadyExistException(cause);
    }
    
    public static LdapException NAME_NOT_FOUND(Throwable cause) {
        return new LdapNameNotFoundException(cause);
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

    public static class LdapNameAlreadyExistException extends LdapException {
        public static final String NAME_ALREADY_EXIST = "ldap.NAME_ALREADY_EXIST";
        
        public LdapNameAlreadyExistException(Throwable cause) {
            super("name already exist", NAME_ALREADY_EXIST, cause);
        }
    }
    
    public static class LdapContextNotEmptyException extends LdapException {
        public static final String CONTEXT_NOT_EMPTY = "ldap.CONTEXT_NOT_EMPTY";
        
        public LdapContextNotEmptyException(Throwable cause) {
            super("context not empty", CONTEXT_NOT_EMPTY, cause);
        }
    }
    
    
    
}
