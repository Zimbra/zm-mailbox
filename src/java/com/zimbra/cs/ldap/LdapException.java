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
    
    public static final String CONTEXT_NOT_EMPTY        = "ldap.CONTEXT_NOT_EMPTY";
    public static final String ENTRY_ALREADY_EXIST      = "ldap.ENTRY_ALREADY_EXIST";
    public static final String ENTRY_NOT_FOUND          = "ldap.ENTRY_NOT_FOUND";
    public static final String INVALID_ATTR_NAME        = "ldap.INVALID_ATTR_NAME";
    public static final String INVALID_ATTR_VALUE       = "ldap.INVALID_ATTR_VALUE";
    public static final String INVALID_NAME             = "ldap.INVALID_NAME";
    public static final String INVALID_SEARCH_FILTER    = "ldap.INVALID_SEARCH_FILTER";
    public static final String MULTIPLE_ENTRIES_MATCHED = "ldap.MULTIPLE_ENTRIES_MATCHED";
    public static final String SIZE_LIMIT_EXCEEDED      = "ldap.SIZE_LIMIT_EXCEEDED";
        
    // in addition to getCause(), a more exception for callsites to relate 
    // the exception to a user message.
    private Throwable detail;
    
    private static String format(String msg1, String msg2) {
        if (msg2 == null) {
            return msg1;
        } else {
            return msg1 + " - " + msg2;
        }
    }
    
    public void setDetail(Throwable detail) {
        this.detail = detail;
    }
    
    public Throwable getDetail() {
        return detail;
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
    
    public static LdapException INVALID_SEARCH_FILTER(String message, Throwable cause) {
        return new LdapInvalidSearchFilterException(message, cause);
    }
    
    public static LdapException INVALID_ATTR_NAME(String message, Throwable cause) {
        return new LdapInvalidAttrNameException(message, cause);
    }
    
    public static LdapException INVALID_ATTR_VALUE(String message, Throwable cause) {
        return new LdapInvalidAttrValueException(message, cause);
    }
    
    public static LdapException ENTRY_ALREADY_EXIST(String message, Throwable cause) {
        return new LdapEntryAlreadyExistException(message, cause);
    }
    
    public static LdapException ENTRY_NOT_FOUND(String message, Throwable cause) {
        return new LdapEntryNotFoundException(message, cause);
    }
    
    public static LdapException MULTIPLE_ENTRIES_MATCHED(String base, 
            String query, String dups) {
        return new LdapMultipleEntriesMatchedException(base, query, dups);
    }
    
    public static LdapException SIZE_LIMIT_EXCEEDED(String message, Throwable cause) {
        return new LdapSizeLimitExceededException(message, cause);
    }
    
    
    //
    // Subclasses mapped to native(JNDI/UBID) ldap exceptions
    //
    
    public static class LdapContextNotEmptyException extends LdapException {
        private LdapContextNotEmptyException(String message, Throwable cause) {
            super(format("context not empty", message), CONTEXT_NOT_EMPTY, cause);
        }
    }
    
    public static class LdapEntryAlreadyExistException extends LdapException {
        private LdapEntryAlreadyExistException(String message, Throwable cause) {
            super(format("entry already exist", message), ENTRY_ALREADY_EXIST, cause);
        }
    }
    
    public static class LdapEntryNotFoundException extends LdapException {
        private LdapEntryNotFoundException(String message, Throwable cause) {
            super(format("entry not found", message), ENTRY_NOT_FOUND, cause);
        }
    }
    
    public static class LdapInvalidAttrNameException extends LdapException {
        private LdapInvalidAttrNameException(String message, Throwable cause) {
            super(format("invalid attr name", message), INVALID_ATTR_NAME, cause);
        }
    }
    
    public static class LdapInvalidAttrValueException extends LdapException {
        private LdapInvalidAttrValueException(String message, Throwable cause) {
            super(format("invalid attr value", message), INVALID_ATTR_VALUE, cause);
        }
    }
    
    public static class LdapInvalidNameException extends LdapException {
        private LdapInvalidNameException(String message, Throwable cause) {
            super(format("invalid name", message), INVALID_NAME, cause);
        }
    }
    
    public static class LdapInvalidSearchFilterException extends LdapException {
        private LdapInvalidSearchFilterException(String message, Throwable cause) {
            super(format("invalid search filter", message), INVALID_SEARCH_FILTER, cause);
        }
    }
    
    public static class LdapMultipleEntriesMatchedException extends LdapException {
        private LdapMultipleEntriesMatchedException(String base, String query, String dups) {
            super(String.format("multiple entries matched: base=%s, query=%s, entries=%s",
                    base, query, dups), MULTIPLE_ENTRIES_MATCHED, null);
        }
    }
    
    public static class LdapSizeLimitExceededException extends LdapException {
        private LdapSizeLimitExceededException(String message, Throwable cause) {
            super(format("size limit exceeded", message), SIZE_LIMIT_EXCEEDED, cause);
        }
    }
}
