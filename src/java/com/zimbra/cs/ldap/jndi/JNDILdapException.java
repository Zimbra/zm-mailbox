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
package com.zimbra.cs.ldap.jndi;

import javax.naming.ContextNotEmptyException;
import javax.naming.InvalidNameException;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.SizeLimitExceededException;

import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapTODO;
import com.zimbra.cs.ldap.LdapTODO.*;

public class JNDILdapException {
    
    static LdapException mapToLdapException(Throwable e) {
        
        if (e instanceof NameAlreadyBoundException) {
            return LdapException.ENTRY_ALREADY_EXIST(null, e);
        } else if (e instanceof ContextNotEmptyException) {
            return LdapException.CONTEXT_NOT_EMPTY(null, e);
        } else if (e instanceof SizeLimitExceededException) {
            return LdapException.SIZE_LIMIT_EXCEEDED(null, e);
        } else if (e instanceof NameNotFoundException) {
            return LdapException.ENTRY_NOT_FOUND(null, e);
        } else if (e instanceof InvalidNameException) {
            LdapTODO.FAIL(FailCode.LdapInvalidNameExceptionShouldNeverBeThrown);
            return LdapException.INVALID_NAME(null, e);
        }
        
        // anything else is mapped to a generic LDAP error
        return LdapException.LDAP_ERROR(e);
        
    }
}
