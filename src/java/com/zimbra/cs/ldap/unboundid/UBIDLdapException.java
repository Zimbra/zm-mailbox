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
package com.zimbra.cs.ldap.unboundid;

import java.io.IOException;

import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.zimbra.cs.ldap.LdapException;

class UBIDLdapException {
    
    static LdapException mapToLdapException(Throwable e) {
        return mapToLdapException(null, e);
    }
    
    static LdapException mapToLdapException(String message, Throwable e) {
        if (e instanceof LDAPException) {
            return mapToLdapException(message, (LDAPException) e);
        } else {
            return LdapException.LDAP_ERROR(message, e);
        }
    }

    static LdapException mapToLdapException(LDAPException e) {
        return mapToLdapException(null, e);
    }

    static LdapException mapToLdapException(String message, LDAPException e) {
        ResultCode rc = e.getResultCode();
        
        if (ResultCode.ENTRY_ALREADY_EXISTS == rc) {
            return LdapException.ENTRY_ALREADY_EXIST(message, e);
            
        } else if (ResultCode.NOT_ALLOWED_ON_NONLEAF == rc) {
            return LdapException.CONTEXT_NOT_EMPTY(message, e);
            
        } else if (ResultCode.UNDEFINED_ATTRIBUTE_TYPE == rc) { 
            return LdapException.INVALID_ATTR_NAME(message, e);
            
        } else if (ResultCode.CONSTRAINT_VIOLATION == rc ||
                ResultCode.INVALID_ATTRIBUTE_SYNTAX == rc) {
            return LdapException.INVALID_ATTR_VALUE(message, e);
            
        } else if (ResultCode.OBJECT_CLASS_VIOLATION == rc) {
            return LdapException.OBJECT_CLASS_VIOLATION(message, e);
            
        } else if (ResultCode.SIZE_LIMIT_EXCEEDED == rc) {
            return LdapException.SIZE_LIMIT_EXCEEDED(message, e);
            
        } else if (ResultCode.NO_SUCH_OBJECT == rc) { 
            // mostly when the search base DB does not exist in the DIT
            return LdapException.ENTRY_NOT_FOUND(message, e);
            
        } else if (ResultCode.FILTER_ERROR == rc) { 
            return LdapException.INVALID_SEARCH_FILTER(message, e);
            
        }
        
        return LdapException.LDAP_ERROR(message, e);
    }
    
    // need more precise mapping for external LDAP exceptions so we
    // can report config error better
    static LdapException mapToExternalLdapException(String message, LDAPException e) {
        Throwable cause = e.getCause();
        ResultCode rc = e.getResultCode();
        
        // the LdapException instance to return
        LdapException ldapException = mapToLdapException(message, e);
        
        if (cause instanceof IOException) {
            // Unboundid hides the original IOException and throws a 
            // generic IOException.  This doesn't work with check.toResult(IOException).
            // Do our best to figure out the original IOException and set it 
            // in the detail field.  Very hacky!
            //
            // Seems the root exception can be found in the message of the IOException
            // thrown by Unboundi.
            // 
            // e.g. An error occurred while attempting to establish a connection to server bogus:389:  java.net.UnknownHostException: bogus 
            IOException ioException = (IOException) cause;
            String causeMsg = ioException.getMessage();
            IOException rootException = null;
            if (causeMsg != null) {
                //
                // try to match IoExceptions examined in check.toResult(IOException).
                //
                if (causeMsg.contains("java.net.UnknownHostException")) {
                    rootException = new java.net.UnknownHostException(causeMsg);
                } else if (causeMsg.contains("java.net.ConnectException")) {
                    rootException = new java.net.ConnectException(causeMsg);
                } else if (causeMsg.contains("javax.net.ssl.SSLHandshakeException")) {
                    rootException = new javax.net.ssl.SSLHandshakeException(causeMsg);
                }
            }
            if (rootException != null) {
                ldapException.setDetail(rootException);
            } else {
                ldapException.setDetail(cause);
            }
        } else {
            String causeMsg = e.getMessage();
            
            Throwable rootException = null;
            if (causeMsg.contains("unsupported extended operation")) {
                // most likely startTLS failed, for backward compatibility with check.toResult,
                // return a generic IOException
                rootException = new IOException(causeMsg);
            } else {
                // just map using the regular mapping
                rootException = mapToLdapException(message, e);
            }
            
            ldapException.setDetail(rootException);
        }
        
        return ldapException;
    }

}
