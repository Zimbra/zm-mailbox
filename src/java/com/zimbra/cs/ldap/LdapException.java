/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.ldap;

import com.zimbra.common.service.ServiceException;

public class LdapException extends ServiceException {

    public static final String TODO = "ldap.TODO";
    
    public static final String INVALID_CONFIG = "ldap.INVALID_CONFIG";
    
    // generic LDAP error that is not mapped to a subclass of LdapException
    public static final String LDAP_ERROR = "ldap.LDAP_ERROR";
    
    public static final String CONTEXT_NOT_EMPTY = 
        LdapContextNotEmptyException.CONTEXT_NOT_EMPTY;
    
    public static final String INVALID_NAME = 
        LdapInvalidNameException.INVALID_NAME;
    
    public static final String ENTRY_ALREADY_EXIST = 
        LdapEntryAlreadyExistException.ENTRY_ALREADY_EXIST;
    
    public static final String NAME_NOT_FOUND = 
        LdapNameNotFoundException.NAME_NOT_FOUND;
    
    public static final String MULTIPLE_ENTRIES_MATCHED = 
        LdapMultipleEntriesMatchedException.MULTIPLE_ENTRIES_MATCHED;
    
    
    private static String format(String msg1, String msg2) {
        if (msg2 == null) {
            return msg1;
        } else {
            return msg1 + " - " + msg2;
        }
    }
    
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
        return new LdapException(format("LDAP error: ", message), LDAP_ERROR, cause);
    }
    
    //
    // Specific LDAP errors needs handling
    //
    public static LdapException CONTEXT_NOT_EMPTY(String message, Throwable cause) {
        return new LdapContextNotEmptyException(message, cause);
    }
    
    public static LdapException INVALID_NAME(String message, Throwable cause) {
        return new LdapInvalidNameException(message, cause);
    }
    
    public static LdapException ENTRY_ALREADY_EXIST(String message, Throwable cause) {
        return new LdapEntryAlreadyExistException(message, cause);
    }
    
    public static LdapException NAME_NOT_FOUND(String message, Throwable cause) {
        return new LdapNameNotFoundException(message, cause);
    }
    
    public static LdapException MULTIPLE_ENTRIES_MATCHED(String base, 
            String query, String dups) {
        return new LdapMultipleEntriesMatchedException(base, query, dups);
    }
    
    //
    // Subclasses mapped to native(JNDI/UBID) ldap exceptions
    //
    public static class LdapInvalidNameException extends LdapException {
        public static final String INVALID_NAME = "ldap.INVALID_NAME";
        
        private LdapInvalidNameException(String message, Throwable cause) {
            super(format("invalid name", message), INVALID_NAME, cause);
        }
    }
    
    public static class LdapNameNotFoundException extends LdapException {
        public static final String NAME_NOT_FOUND = "ldap.NAME_NOT_FOUND";
        
        private LdapNameNotFoundException(String message, Throwable cause) {
            super(format("name not found", message), NAME_NOT_FOUND, cause);
        }
    }

    public static class LdapEntryAlreadyExistException extends LdapException {
        public static final String ENTRY_ALREADY_EXIST = "ldap.ENTRY_ALREADY_EXIST";
        
        private LdapEntryAlreadyExistException(String message, Throwable cause) {
            super(format("entry already exist", message), ENTRY_ALREADY_EXIST, cause);
        }
    }
    
    public static class LdapContextNotEmptyException extends LdapException {
        public static final String CONTEXT_NOT_EMPTY = "ldap.CONTEXT_NOT_EMPTY";
        
        private LdapContextNotEmptyException(String message, Throwable cause) {
            super(format("context not empty", message), CONTEXT_NOT_EMPTY, cause);
        }
    }
    
    public static class LdapMultipleEntriesMatchedException extends LdapException {
        public static final String MULTIPLE_ENTRIES_MATCHED = "ldap.MULTIPLE_ENTRIES_MATCHED";
        
        private LdapMultipleEntriesMatchedException(String base, String query, 
                String dups) {
            super(String.format("multiple entries matched: base=%s, query=%s, entries=%s",
                    base, query, dups), MULTIPLE_ENTRIES_MATCHED, null);
        }
    }    
    
}
