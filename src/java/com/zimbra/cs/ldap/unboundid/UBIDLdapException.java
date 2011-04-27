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
        
        if (ResultCode.ENTRY_ALREADY_EXISTS == e.getResultCode()) {
            return LdapException.ENTRY_ALREADY_EXIST(message, e);
        }
        
        return LdapException.LDAP_ERROR(message, e);
    }

}
