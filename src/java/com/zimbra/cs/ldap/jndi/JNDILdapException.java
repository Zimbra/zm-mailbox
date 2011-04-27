package com.zimbra.cs.ldap.jndi;

import javax.naming.ContextNotEmptyException;
import javax.naming.InvalidNameException;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;

import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapTODO;
import com.zimbra.cs.ldap.LdapTODO.FailCode;
import com.zimbra.cs.ldap.LdapTODO.TODO;

public class JNDILdapException {
    
    static LdapException mapToLdapException(Throwable e) {
        if (e instanceof NameNotFoundException) {
            LdapTODO.FAIL(FailCode.NameNotFoundExceptionShouldNeverBeThrown);
            return LdapException.NAME_NOT_FOUND(null, e);
        } else if (e instanceof InvalidNameException) {
            LdapTODO.FAIL(FailCode.LdapInvalidNameExceptionShouldNeverBeThrown);
            return LdapException.INVALID_NAME(null, e);
        } else if (e instanceof NameAlreadyBoundException) {
            return LdapException.ENTRY_ALREADY_EXIST(null, e);
        } else if (e instanceof ContextNotEmptyException) {
            return LdapException.CONTEXT_NOT_EMPTY(null, e);
        } else {
            // anything else is mapped to a generic LDAP error
            return LdapException.LDAP_ERROR(e);
        }
    }
}
