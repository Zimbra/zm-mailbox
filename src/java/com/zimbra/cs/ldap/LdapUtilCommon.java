package com.zimbra.cs.ldap;

import com.zimbra.common.util.ByteUtil;

/**
 * Utils methods shared by both the legacy com.zimbra.cs.account.ldap.LdapUtil
 * and the new com.zimbra.cs.ldap.LdapUtil
 */
public class LdapUtilCommon {

    public static boolean contains(String[] values, String val) {
        if (values == null) {
            return false;
        }
        
        for (String s : values) {
            if (s.compareToIgnoreCase(val) == 0) {
                return true;
            }
        }
        return false;
    }
    
    public static Object decodeBase64IfBinary(boolean isBinary, String value) {
        return isBinary ? ByteUtil.decodeLDAPBase64(value) : value;
    }
    
    
    /*
     * convert a real attrName to a binaryTransferAttrName if necessary
     * 
     * e.g. userCertificate => userCertificate;binary
     */
    public static String attrNameToBinaryTransferAttrName(boolean isBinaryTransfer, String attrName) {
        return isBinaryTransfer ? attrName + ";binary" : attrName;
    }
    
    /*
     * convert a binaryTransferAttrName to the real attrName
     * 
     * e.g. userCertificate;binary => userCertificate
     *      zimbraId => zimbraId
     */
    public static String binaryTransferAttrNameToAttrName(String transferAttrName) {
        if (transferAttrName.endsWith(";binary")) {
            String[] parts = transferAttrName.split(";");
            if (parts.length == 2) {
                return parts[0];
            }
        }
        return transferAttrName;
    }
}
