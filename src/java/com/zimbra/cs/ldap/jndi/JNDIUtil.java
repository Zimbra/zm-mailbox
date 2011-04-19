package com.zimbra.cs.ldap.jndi;

import javax.naming.directory.BasicAttribute;

import com.zimbra.cs.ldap.LdapUtilCommon;

public class JNDIUtil {
    
    static BasicAttribute newAttribute(boolean isBinaryTransfer, String attrName) {
        String transferAttrName = LdapUtilCommon.attrNameToBinaryTransferAttrName(isBinaryTransfer, attrName);
        return new BasicAttribute(transferAttrName);
    }
}
