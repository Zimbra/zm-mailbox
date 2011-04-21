package com.zimbra.cs.ldap;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.gal.GalOp;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.ldap.legacy.LegacyLdapUtil;
import com.zimbra.cs.account.ldap.legacy.entry.LdapDomain;

/**
 * Utils methods shared by both the legacy com.zimbra.cs.account.ldap.LdapUtil
 * and the new com.zimbra.cs.ldap.LdapUtil
 */
public class LdapUtilCommon {
    
    public final static String LDAP_TRUE  = "TRUE";
    public final static String LDAP_FALSE = "FALSE";
    
    public final static String EARLIEST_SYNC_TOKEN = "19700101000000Z";

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
    
    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }
    
    /*
     * we want to throw the IllegalArgumentException instead of catching it so the cause
     * can be logged with the callers catcher.
     */
    public static boolean isValidUUID(String strRep) throws IllegalArgumentException {
        /*
        if (strRep.length() > 36)
            throw new IllegalArgumentException("uuid must be no longer than 36 characters");
        
        UUID uuid = UUID.fromString(strRep);
        return (uuid != null);   
        */
        
        if (strRep.length() > Provisioning.MAX_ZIMBRA_ID_LEN)
            throw new IllegalArgumentException("uuid must be no longer than " + Provisioning.MAX_ZIMBRA_ID_LEN + " characters");
        
        if (strRep.contains(":"))
            throw new IllegalArgumentException("uuid must not contain ':'");
        
        return true;
    }

    /**
     * Return the later (more recent) of two LDAP timestamps.  Timestamp
     * format is YYYYMMDDhhmmssZ. (e.g. 20060315023000Z)
     * @param timeA
     * @param timeB
     * @return later of the two timestamps; a non-null timestamp is considered
     *         later than a null timestamp; null is returned if both timestamps
     *         are null
     */
    public static String getLaterTimestamp(String timeA, String timeB) {
        if (timeA == null) {
            return timeB;
        } else if (timeB == null) {
            return timeA;
        }
        return timeA.compareTo(timeB) > 0 ? timeA : timeB;
    }

    public static String getEarlierTimestamp(String timeA, String timeB) {
        if (timeA == null) {
            return timeB;
        } else if (timeB == null) {
            return timeA;
        }
        return timeA.compareTo(timeB) < 0 ? timeA : timeB;
    }

    /*
      * expansions for bind dn string:
      * 
      * %n = username with @ (or without, if no @ was specified)
      * %u = username with @ removed
      * %d = domain as foo.com
      * %D = domain as dc=foo,dc=com
      * 
      * exchange example, where the exchange domian is different than the zimbra one
      * 
      * zimbraAuthMech      ldap
      * zimbraAuthLdapURL   ldap://exch1/
      * zimbraAuthLdapDn    %n@example.zimbra.com
      * 
      * our own LDAP example:
      * 
      * zimbraAuthMech       ldap
      * zimbraAuthLdapURL    ldap://server.example.zimbra.com/
      * zimbraAuthLdapUserDn uid=%u,ou=people,%D
      */
      public static String computeAuthDn(String name, String bindDnRule) {
         if (bindDnRule == null || bindDnRule.equals("") || bindDnRule.equals("%n"))
             return name;
    
         int at = name.indexOf("@");
    
         Map<String, String> vars = new HashMap<String, String>();
         vars.put("n", name);         
    
         if (at  == -1) {
             vars.put("u", name);
         } else {
             vars.put("u", name.substring(0, at));
             String d = name.substring(at+1);
             vars.put("d", d);
             vars.put("D", LegacyLdapUtil.domainToDN(d));
         }
         
         return LdapProvisioning.expandStr(bindDnRule, vars);
      }

    public static String getBooleanString(boolean b) {
        if (b) {
            return LDAP_TRUE;
        }
        return LDAP_FALSE;
    }

    public static String getZimbraSearchBase(Domain domain, GalOp galOp) {
        String sb;
        if (galOp == GalOp.sync) {
            sb = domain.getAttr(Provisioning.A_zimbraGalSyncInternalSearchBase);
            if (sb == null)
                sb = domain.getAttr(Provisioning.A_zimbraGalInternalSearchBase, "DOMAIN");
        } else {
            sb = domain.getAttr(Provisioning.A_zimbraGalInternalSearchBase, "DOMAIN");
        }
        LdapDomain ld = (LdapDomain) domain;
        if (sb.equalsIgnoreCase("DOMAIN"))
            return ld.getDN();
            //mSearchBase = mDIT.domainDNToAccountSearchDN(ld.getDN());
        else if (sb.equalsIgnoreCase("SUBDOMAINS"))
            return ld.getDN();
        else if (sb.equalsIgnoreCase("ROOT"))
            return "";
        return "";
    }

}
