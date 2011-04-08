package com.zimbra.cs.ldap.unboundid;

import com.zimbra.cs.ldap.LdapException;

public class UBIDLdapException extends LdapException {

    protected UBIDLdapException(String message, String code, Throwable cause) {
        super(message, code, cause);
    }
    
}
