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

import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapTODO;

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
        /*
        } else if (ResultCode.??? == rc) {
            return LdapException.ENTRY_NOT_FOUND(message, e);  // TODO: which code should be mapped to this ?
        */    
        } else if (ResultCode.NOT_ALLOWED_ON_NONLEAF == rc) {
            return LdapException.CONTEXT_NOT_EMPTY(message, e);
        }
        
        return LdapException.LDAP_ERROR(message, e);
    }

}
