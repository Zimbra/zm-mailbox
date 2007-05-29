package com.zimbra.cs.account.ldap.custom;

import java.util.Map;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.ldap.LdapDIT;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.account.ldap.SpecialAttrs;

public class CustomLdapDIT extends LdapDIT {
    
    private String getLC(String key, String defaultValue) {
        String lcValue = LC.get(key);
        
        if (StringUtil.isNullOrEmpty(lcValue))
            return defaultValue;
        else
            return lcValue;
        
    }
    
    protected void init() {
        /*
         * If a LDAP design desires that all the config branch entries are based directly on the 
         * "" dn, it can do so by setting the ldap_config_base_dn to empty.
         * The default value in LC for this known key is "cn=zimbra"
         */
       String configBase = LC.get("ldap_config_base_dn");

        /*
         * If a LDAP design does not need a base rdn for accounts, the ldap_account_base_rdn
         * should be set to empty.
         * The default value in LC for this known key is "ou=people"
         */ 
        BASE_RDN_ACCOUNT  = LC.get("ldap_account_base_rdn");

        NAMING_RDN_ATTR_ACCOUNT       = getLC("ldap_account_naming_rdn_attr",      DEFAULT_NAMING_RDN_ATTR_ACCOUNT);
        NAMING_RDN_ATTR_ADMIN         = getLC("ldap_admin_naming_rdn_attr",        DEFAULT_NAMING_RDN_ATTR_ADMIN);
        NAMING_RDN_ATTR_COS           = getLC("ldap_cos_naming_rdn_attr",          DEFAULT_NAMING_RDN_ATTR_COS);
        NAMING_RDN_ATTR_GLOBALCONFIG  = getLC("ldap_globalconfig_naming_rdn_attr", DEFAULT_NAMING_RDN_ATTR_GLOBALCONFIG);
        NAMING_RDN_ATTR_MIME          = getLC("ldap_mime_naming_rdn_attr",         DEFAULT_NAMING_RDN_ATTR_MIME);
        NAMING_RDN_ATTR_SERVER        = getLC("ldap_server_naming_rdn_attr",       DEFAULT_NAMING_RDN_ATTR_SERVER);
        NAMING_RDN_ATTR_ZIMLET        = getLC("ldap_zimlet_naming_rdn_attr",       DEFAULT_NAMING_RDN_ATTR_ZIMLET);
       
        DN_GLOBALCONFIG   = NAMING_RDN_ATTR_GLOBALCONFIG + "=config" + "," + configBase; 

        BASE_DN_ADMIN        = getLC("ldap_admin_base_dn",  DEFAULT_BASE_RDN_ADMIN  + "," + configBase);
        BASE_DN_COS          = getLC("ldap_cos_base_dn",    DEFAULT_BASE_RDN_COS    + "," + configBase); 
        BASE_DN_MIME         = getLC("ldap_mime_base_dn",   DEFAULT_BASE_RDN_MIME   + "," + DN_GLOBALCONFIG);
        BASE_DN_SERVER       = getLC("ldap_server_base_dn", DEFAULT_BASE_RDN_SERVER + "," + configBase);
        BASE_DN_ZIMLET       = getLC("ldap_zimlet_base_dn", DEFAULT_BASE_RDN_ZIMLET + "," + configBase);
    }
    
    /*
     * ==========
     *   alias
     * ==========
     */
    public String aliasDN(String targetEntryDn, String aliasLocalPart, String aliasDomain) {
        String[] parts = LdapUtil.dnToRdnAndBaseDn(targetEntryDn);
        return NAMING_RDN_ATTR_ACCOUNT + "=" + LdapUtil.escapeRDNValue(aliasLocalPart) + "," + parts[1];
    }
    
    protected SpecialAttrs handleSpecialAttrs(Map<String, Object> attrs) throws ServiceException {
        SpecialAttrs specialAttrs = new SpecialAttrs();
        if (attrs != null) {
            specialAttrs.handleZimbraId(attrs);
            specialAttrs.handleLdapBaseDn(attrs);
            specialAttrs.handleLdapRdnAttr(attrs);
        }
        return specialAttrs;
    }
}
