/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.account.ldap.custom;

import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import com.zimbra.common.localconfig.KnownKey;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapDIT;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.account.ldap.SpecialAttrs;

/*
 * This class and CustomerLdapProvisioning should really go under the 
 * Velodrome extension.  But there is an initializing sequence problem 
 * that the class specified in the zimbra_class_provisioning key cannot 
 * be in an extension.  Currently it just calls Class.forName, which does 
 * not take into account that the class could be in an extension.  
 * However even if it does and uses ExtensionUtil.loadClass 
 * (we can use {ext-name}:{class-name} format for the zimbra_class_provisioning key),
 * the class still cannot be loaded because:
 * 
 * when server is starting up, Provisioning.getInstance is first called from 
 * PrivilegedServlet.init, then called from SoapServlet.init() ... -> Config.init(), 
 * but ExtensionUtil.loadAll is not called until SoapServlet.init -> Zimbra.startup(), 
 * which happens later.
 * 
 * For now the Velodrome Provisioning impl will have to reside in the core product.
 * 
 * While CustomLdapProvisioning and CustomLdapDIT try to make DIT layout as generic
 * as possible, it was implemented to meet the requirement from Velodrome.  It provides
 * flexibility and differs from the default DIT via localconfig keys as follows:
 * 
 * See ZimbraCustomerServices/velodrome/ZimbraServer/docs/custom-DIT.txt for detailed
 * descrittion of the functionalities of the custom DIT.
 * 
 */

public class CustomLdapDIT extends LdapDIT {
    
    private final String DEFAULT_BASE_RDN_DOMAIN = "cn=domains";
    private String BASE_DN_DOMAIN;
    
    public CustomLdapDIT(LdapProvisioning prov) {
        super(prov);
    }
    
    /*
     * We do not, yet, want to put the following keys in LC.  
     * We put them here to suppress WARMs spit out by LC.get if the key is not a KnownKey nor is it set in localconfig.xml.
     */
    static class CustomLdapDITLC {
        public static final KnownKey ldap_dit_base_dn_admin         = new KnownKey("ldap_dit_base_dn_admin",         "", "LDAP Custom DIT base DN for LDAP admin entries");
        public static final KnownKey ldap_dit_base_dn_appadmin      = new KnownKey("ldap_dit_base_dn_appadmin",      "", "LDAP Custom DIT base DN for LDAP app admin entries");
        public static final KnownKey ldap_dit_base_dn_config        = new KnownKey("ldap_dit_base_dn_config",        "", "LDAP Custom DIT base DN for config branch");
        public static final KnownKey ldap_dit_base_dn_cos           = new KnownKey("ldap_dit_base_dn_cos",           "", "LDAP Custom DIT base DN for cos entries");
        public static final KnownKey ldap_dit_base_dn_domain        = new KnownKey("ldap_dit_base_dn_domain",        "", "LDAP Custom DIT base DN for domain entries");
        public static final KnownKey ldap_dit_base_dn_mail          = new KnownKey("ldap_dit_base_dn_mail",          "", "LDAP Custom DIT base DN for mail(accounts, aliases, DLs, resources) entries");
        public static final KnownKey ldap_dit_base_dn_mime          = new KnownKey("ldap_dit_base_dn_mime",          "", "LDAP Custom DIT base DN for mime entries");
        public static final KnownKey ldap_dit_base_dn_server        = new KnownKey("ldap_dit_base_dn_server",        "", "LDAP Custom DIT base DN for server entries");
        public static final KnownKey ldap_dit_base_dn_xmppcomponent = new KnownKey("ldap_dit_base_dn_xmppcomponent", "", "LDAP Custom DIT base DN for xmpp component entries");
        public static final KnownKey ldap_dit_base_dn_zimlet        = new KnownKey("ldap_dit_base_dn_zimlet",        "", "LDAP Custom DIT base DN for zimlet entries");
        
        public static final KnownKey ldap_dit_naming_rdn_attr_cos          = new KnownKey("ldap_dit_naming_rdn_attr_cos",          "", "LDAP Custom DIT RDN attr for cos entries");
        public static final KnownKey ldap_dit_naming_rdn_attr_globalconfig = new KnownKey("ldap_dit_naming_rdn_attr_globalconfig", "", "LDAP Custom DIT RDN attr for globalconfig entry");
        public static final KnownKey ldap_dit_naming_rdn_attr_globalgrant  = new KnownKey("ldap_dit_naming_rdn_attr_globalgrant",  "", "LDAP Custom DIT RDN attr for globalgrant entry");
        public static final KnownKey ldap_dit_naming_rdn_attr_mime         = new KnownKey("ldap_dit_naming_rdn_attr_mime",         "", "LDAP Custom DIT RDN attr for mime entries");
        public static final KnownKey ldap_dit_naming_rdn_attr_server       = new KnownKey("ldap_dit_naming_rdn_attr_server",       "", "LDAP Custom DIT RDN attr for server entries");
        public static final KnownKey ldap_dit_naming_rdn_attr_user         = new KnownKey("ldap_dit_naming_rdn_attr_user",         "", "LDAP Custom DIT RDN attr for account, calendar resource, and distribution list entries");
        public static final KnownKey ldap_dit_naming_rdn_attr_xmppcomponent= new KnownKey("ldap_dit_naming_rdn_attr_xmppcomponent","", "LDAP Custom DIT RDN attr for xmpp component entries");
        public static final KnownKey ldap_dit_naming_rdn_attr_zimlet       = new KnownKey("ldap_dit_naming_rdn_attr_zimlet",       "", "LDAP Custom DIT RDN attr for zimlet entries");
    }
    
    private String getLC(KnownKey key, String defaultValue) {
        String lcValue = key.value();
        
        if (StringUtil.isNullOrEmpty(lcValue))
            return defaultValue;
        else
            return lcValue;
    }
    
    private String getLCAndValidateUnderConfigBranchDN(KnownKey key, String defaultValue) {
        String dn = getLC(key, defaultValue);
        
        if (!validateUnderDN(BASE_DN_CONFIG_BRANCH, dn)) {
            ZimbraLog.account.warn("dn " + dn + " must be under " + BASE_DN_CONFIG_BRANCH + ", localconfig value " + dn + " ignored, using default value " + defaultValue);
            dn = defaultValue;
        }
        
        return dn;
    }
    
    protected void init() {
       
        BASE_DN_CONFIG_BRANCH = getLC(CustomLdapDITLC.ldap_dit_base_dn_config, DEFAULT_CONFIG_BASE_DN);
        BASE_DN_MAIL_BRANCH   = getLC(CustomLdapDITLC.ldap_dit_base_dn_mail, DEFAULT_MAIL_BASE_DN).toLowerCase();

        BASE_RDN_ACCOUNT  = "";

        NAMING_RDN_ATTR_USER          = getLC(CustomLdapDITLC.ldap_dit_naming_rdn_attr_user,         DEFAULT_NAMING_RDN_ATTR_USER);
        NAMING_RDN_ATTR_COS           = getLC(CustomLdapDITLC.ldap_dit_naming_rdn_attr_cos,          DEFAULT_NAMING_RDN_ATTR_COS);
        NAMING_RDN_ATTR_GLOBALCONFIG  = getLC(CustomLdapDITLC.ldap_dit_naming_rdn_attr_globalconfig, DEFAULT_NAMING_RDN_ATTR_GLOBALCONFIG);
        NAMING_RDN_ATTR_GLOBALGRANT   = getLC(CustomLdapDITLC.ldap_dit_naming_rdn_attr_globalgrant,  DEFAULT_NAMING_RDN_ATTR_GLOBALGRANT);
        NAMING_RDN_ATTR_MIME          = getLC(CustomLdapDITLC.ldap_dit_naming_rdn_attr_mime,         DEFAULT_NAMING_RDN_ATTR_MIME);
        NAMING_RDN_ATTR_SERVER        = getLC(CustomLdapDITLC.ldap_dit_naming_rdn_attr_server,       DEFAULT_NAMING_RDN_ATTR_SERVER);
        NAMING_RDN_ATTR_XMPPCOMPONENT = getLC(CustomLdapDITLC.ldap_dit_naming_rdn_attr_xmppcomponent,DEFAULT_NAMING_RDN_ATTR_XMPPCOMPONENT);
        NAMING_RDN_ATTR_ZIMLET        = getLC(CustomLdapDITLC.ldap_dit_naming_rdn_attr_zimlet,       DEFAULT_NAMING_RDN_ATTR_ZIMLET);
       
        DN_GLOBALCONFIG    = NAMING_RDN_ATTR_GLOBALCONFIG + "=config" + "," + BASE_DN_CONFIG_BRANCH;
        DN_GLOBALGRANT     = NAMING_RDN_ATTR_GLOBALGRANT  + "=globalgrant" + "," + BASE_DN_CONFIG_BRANCH;

        BASE_DN_ADMIN         = getLCAndValidateUnderConfigBranchDN(CustomLdapDITLC.ldap_dit_base_dn_admin,         DEFAULT_BASE_RDN_ADMIN         + "," + BASE_DN_CONFIG_BRANCH);
        BASE_DN_APPADMIN      = getLCAndValidateUnderConfigBranchDN(CustomLdapDITLC.ldap_dit_base_dn_appadmin,      DEFAULT_BASE_RDN_APPADMIN      + "," + BASE_DN_CONFIG_BRANCH);
        BASE_DN_COS           = getLCAndValidateUnderConfigBranchDN(CustomLdapDITLC.ldap_dit_base_dn_cos,           DEFAULT_BASE_RDN_COS           + "," + BASE_DN_CONFIG_BRANCH); 
        BASE_DN_MIME          = getLCAndValidateUnderConfigBranchDN(CustomLdapDITLC.ldap_dit_base_dn_mime,          DEFAULT_BASE_RDN_MIME          + "," + DN_GLOBALCONFIG);
        BASE_DN_SERVER        = getLCAndValidateUnderConfigBranchDN(CustomLdapDITLC.ldap_dit_base_dn_server,        DEFAULT_BASE_RDN_SERVER        + "," + BASE_DN_CONFIG_BRANCH);
        BASE_DN_XMPPCOMPONENT = getLCAndValidateUnderConfigBranchDN(CustomLdapDITLC.ldap_dit_base_dn_xmppcomponent, DEFAULT_BASE_RDN_XMPPCOMPONENT + "," + BASE_DN_CONFIG_BRANCH);
        BASE_DN_ZIMLET        = getLCAndValidateUnderConfigBranchDN(CustomLdapDITLC.ldap_dit_base_dn_zimlet,        DEFAULT_BASE_RDN_ZIMLET        + "," + BASE_DN_CONFIG_BRANCH);
    
        BASE_DN_DOMAIN        = getLCAndValidateUnderConfigBranchDN(CustomLdapDITLC.ldap_dit_base_dn_domain, DEFAULT_BASE_RDN_DOMAIN + "," + BASE_DN_CONFIG_BRANCH);
    
        BASE_DN_ZIMBRA        = computeZimbraBaseDN();
    }
    
    /*
     * take the common base for config branch and mail branch
     */
    private String computeZimbraBaseDN() {
        String[] rdns1 = BASE_DN_CONFIG_BRANCH.split(",");
        String[] rdns2 = BASE_DN_MAIL_BRANCH.split(",");
        
        int idx1 = rdns1.length - 1;
        int idx2 = rdns2.length - 1;
        
        int shorter;
        
        if (rdns1.length < rdns2.length)
            shorter = rdns1.length;
        else
            shorter = rdns2.length;
        
        String commonDn = null;
        for (int i=0; i<shorter; i++, idx1--, idx2--) {
            if (rdns1[idx1].equalsIgnoreCase(rdns2[idx2])) {
                if (commonDn == null)
                    commonDn = rdns1[idx1];
                else
                    commonDn = rdns1[idx1] + "," + commonDn;
            } else
                break;
        }
        
        return commonDn;
    }
    
    private ServiceException UNSUPPORTED(String msg) {
        return ServiceException.FAILURE(msg + " unsupported in " + getClass().getCanonicalName(), null);
    }
    
    /*
     * verify that dn is under parentDn
     */
    private boolean validateUnderDN(String parentDn, String dn) {
        return isUnder(parentDn, dn);
    }
    
    private void validateMailBranchEntryDN(String dn) throws ServiceException {
        if (!validateUnderDN(BASE_DN_MAIL_BRANCH, dn))
            throw ServiceException.INVALID_REQUEST("dn " + dn + " must be under " + BASE_DN_MAIL_BRANCH, null);
    }
    
    private String defaultDomain() throws ServiceException {
        String defaultDomain = mProv.getConfig().getAttr(Provisioning.A_zimbraDefaultDomainName, null);
        if (StringUtil.isNullOrEmpty(defaultDomain))
           throw UNSUPPORTED("default domain is empty");
        
        return defaultDomain;
    }
    
    private String acctAndDLDNCreate(String baseDn, Attributes attrs) throws ServiceException, NamingException {
        String rdnAttr = NAMING_RDN_ATTR_USER;
        String rdnValue = LdapUtil.getAttrString(attrs, rdnAttr);
        
        if (rdnValue == null)
            throw ServiceException.FAILURE("missing rdn attribute" + rdnAttr, null);
        
        validateMailBranchEntryDN(baseDn);

        return rdnAttr + "=" + LdapUtil.escapeRDNValue(rdnValue) + "," + baseDn;
    }
    
    /*
     * ===========
     *   account
     * ===========
     */
    public String emailToDN(String localPart, String domain) throws ServiceException{
        throw UNSUPPORTED("function emailToDN");
    }
    
    public String emailToDN(String email) throws ServiceException {
        throw UNSUPPORTED("function emailToDN");
    }
    
    public String accountDNCreate(String baseDn, Attributes attrs, String localPart, String domain) throws ServiceException, NamingException {
        if (baseDn == null)
            throw ServiceException.INVALID_REQUEST("base dn is required in DIT impl " + getClass().getCanonicalName(), null);
       
        return acctAndDLDNCreate(baseDn, attrs);
    }
    
    public String accountDNRename(String oldDn, String newLocalPart, String newDomain) throws ServiceException, NamingException {
        return oldDn;
    }
    
    /*
     * Get email local part attr from attrs and form the email with the default domain
     */
    public String dnToEmail(String dn, Attributes attrs) throws ServiceException {
        String localPart = null;
        try {
            localPart = LdapUtil.getAttrString(attrs, DEFAULT_NAMING_RDN_ATTR_USER);
            
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to map dn [" + dn + "] to email", e);
        }
        
        if (localPart != null)
            return localPart + "@" + defaultDomain();
        else
            throw ServiceException.FAILURE("unable to map dn [" + dn + "] to email", null);
    }
    
    public String filterAccountsByDomain(Domain domain, boolean includeObjectClass) {
        String filter = "(zimbraMailDeliveryAddress=*@" + domain.getName() + ")";
        
        if (includeObjectClass)
            return "(&(objectclass=zimbraAccount)" + filter + ")";
        else
            return filter;
    }

    
    /*
     * ==========
     *   alias
     * ==========
     */
    /* 
     *  - Aliases has to be in the same domain as the target entry, and the domain has to be the default 
     *    domain(config.zimbraDefaultDomainName).  For example, if the entry is foo@domain1.com, 
     *    it *cannot* have foo-alias@domain2.com.  And, in order for foo@domain1.com to have alias, domain1.com 
     *    has to be the default domain.  
     *          
     *  - This is because alias entries do not carry the domain information.  
     *    In our default DIT, aliases are created under the DN which is computed from the domain.
     *    Therefore, given a dn, we can compute it back to the domain of the alias.  
     *    But in the custom DIT, aliases are always created under the same DN of the target entry,
     *    because in the custom DIT there is no co-relation between the email address and the DN. 
     *    As a workaround so we can at least have basic alias functionalities, the dnToEmail function 
     *    always uses zimbraDefaultDomainName for the domain, which is of course still wrong and will break 
     *    when zimbraDefaultDomainName is changed!  Until we have a real solution for that, for now aliases 
     *    in the custom DIT is flakey and this is the best we can do.  Maybe we should just disallow
     *    aliases in the custom DIT?
     */
    public String aliasDN(String targetDn, String targetDomain, String aliasLocalPart, String aliasDomain) throws ServiceException {
        if (targetDn == null || targetDomain == null)
            throw UNSUPPORTED("alias DN without target dn or target domain");
        
        String allowedDomain = defaultDomain();
        if (!aliasDomain.equals(allowedDomain))
            throw UNSUPPORTED("alias DN not in default domain" + "(alias domain=" + aliasDomain + ", default domain=" + allowedDomain + ")");
        
        if (!targetDomain.equals(aliasDomain))
            throw UNSUPPORTED("alias DN with different target domain and alias domain" + "(alias domain=" + aliasDomain + ", target domain=" + targetDomain + ")");
        
        /*
         * alias is placed under the same dn as the target
         */
        String[] parts = LdapUtil.dnToRdnAndBaseDn(targetDn);
        return NAMING_RDN_ATTR_USER + "=" + LdapUtil.escapeRDNValue(aliasLocalPart) + "," + parts[1];
    }
    
    public String aliasDNRename(String targetNewDn, String targetNewDomain, String newAliasEmail) throws ServiceException {
        if (targetNewDn == null || targetNewDomain == null)
            throw UNSUPPORTED("alias DN rename without target dn or target domain");
        
        String allowedDomain = defaultDomain();
        if (!targetNewDomain.equals(allowedDomain))
            throw UNSUPPORTED("alias DN rename not in default domain" + "(alias domain=" + targetNewDomain + ", default domain=" + allowedDomain + ")");

        return targetNewDn;
    }
    
    /* =================
     * calendar resource
     * =================
     */
    public String filterCalendarResourcesByDomain(Domain domain, boolean includeObjectClass) {
        String filter = "(zimbraMailDeliveryAddress=*@" + domain.getName() + ")";
        
        if (includeObjectClass)
            return "(&(objectclass=zimbraCalendarResource)" + filter + ")";
        else
            return filter;
    }
    
    /*
     * =====================
     *   distribution list 
     * =====================
     */
    /*
     * same restrictions on domain as the restrictions for aliases.
     */
    public String distributionListDNCreate(String baseDn, Attributes attrs, String localPart, String domain) throws ServiceException, NamingException {
        if (baseDn == null)
            throw ServiceException.INVALID_REQUEST("base dn is required in DIT impl " + getClass().getCanonicalName(), null);
       
        String allowedDomain = defaultDomain();
        if (!domain.equals(allowedDomain))
            throw UNSUPPORTED("DL DN not in default domain" + "(DL domain=" + domain + ", default domain=" + allowedDomain + ")");

        return acctAndDLDNCreate(baseDn, attrs);
    }
    
    public String distributionListDNRename(String oldDn, String newLocalPart, String newDomain) throws ServiceException, NamingException {
        return oldDn;
    }
    
    /* too bad we can't do anything about DL, we can't tell by mail or zimbraNailAlias
     * which one is an alias and which one is the main email of DL
     * 
     * The one in default DIT is used for DL, that means for custom DIT the getAllDistrubutionLists 
     * function will return DLs in all domains if there are any (although the broken logic 
     * that DL can only belong to the single default domain kind of restricted, but it will 
     * break once the default domain changed)!
     * 
     * mayby we should throw an UNSUPPORTED here.
    public String filterDistributionListsByDomain(Domain domain, boolean includeObjectClass) {
        String filter = "(zimbraMailDeliveryAddress=*@" + domain.getName() + ")";
        
        if (includeObjectClass)
            return "(&(objectclass=zimbraDistributionList)" + filter + ")";
        else
            return filter;
    }
    */
    
    
    /*
     * ==========
     *   domain
     * ==========
     */
    public String domainBaseDN() {
        return BASE_DN_DOMAIN;
    }
    
    public String[] domainToDNs(String[] parts) {
        return domainToDNsInternal(parts, BASE_DN_DOMAIN);
    }
    
    /*
     * account base search dn
     * 
     * In custom DIT, accounts are not located under domain's DN.  
     * Accounts can be created anywhere in the DIT.  We use the mail branch base if 
     * it is configured, the default mail branch base is "".
     */ 
    public String domainToAccountSearchDN(String domain) throws ServiceException {
        return BASE_DN_MAIL_BRANCH;
    }
    
    // account base search dn
    public String domainDNToAccountSearchDN(String domainDN) throws ServiceException {
        return BASE_DN_MAIL_BRANCH;
    }

    
    /*
     * ========================================================================================
     */
    protected SpecialAttrs handleSpecialAttrs(Map<String, Object> attrs) throws ServiceException {
        
        // check for required attrs
        if (SpecialAttrs.getSingleValuedAttr(attrs, SpecialAttrs.PA_ldapBase) == null)
            throw ServiceException.INVALID_REQUEST("missing required attribute " + SpecialAttrs.PA_ldapBase, null);

        if (!NAMING_RDN_ATTR_USER.equals(DEFAULT_NAMING_RDN_ATTR_USER)) {
            if (SpecialAttrs.getSingleValuedAttr(attrs, NAMING_RDN_ATTR_USER) == null)
                throw ServiceException.INVALID_REQUEST("missing required attribute " + NAMING_RDN_ATTR_USER, null);
        }
        
        SpecialAttrs specialAttrs = new SpecialAttrs();
        if (attrs != null) {
            specialAttrs.handleZimbraId(attrs);
            specialAttrs.handleLdapBaseDn(attrs);
        }

            
        return specialAttrs;
    }
}
