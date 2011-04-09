package com.zimbra.cs.ldap;

import java.util.Map;

import com.zimbra.common.util.ByteUtil;

/**
 * Utils methods shared by both the legacy com.zimbra.cs.account.ldap.LdapUtil
 * and the new com.zimbra.cs.ldap.LdapUtil
 */
public class LdapUtilCommon {

    /*
    public static String getAttrString(Map<String, Object> attrs, String name) {
        Object v = attrs.get(name);
        if (v instanceof String) {
            return (String) v;
        } else if (v instanceof String[]) {
            String[] a = (String[]) v;
            return a.length > 0 ? a[0] : null;
        } else {
            return null;
        }
    }
    */
    
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

    /**
     * escape *()\ in specified string to make sure user-supplied string doesn't open a security hole.
     * i.e., if the format string is "(sn=*%s*)", and the user types in "a)(zimbraIsAdminAccount=TRUE)(cn=a",
     * we don't want to search for "(sn=*a)(zimbraIsAdminAccount=TRUE)(cn=a*)".
     * 
     * @param s
     * @return
     */
    public static String escapeSearchFilterArg(String s) {
        if (s == null)
            return null;
        else 
            return s.replaceAll("([\\\\\\*\\(\\)])", "\\\\$0");
    }
}
