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
package com.zimbra.cs.account.ldap;

import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.EmailUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Alias;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.GlobalGrant;
import com.zimbra.cs.account.Identity;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.util.Zimbra;

public class LdapDIT {
    /*
     * This is our default ldap DIT.  All DNs/RDNs location is hardcoded to avoid 
     * mis-configuration.  
     * 
     * To customize the DIT to a different layout, set the zimbra_class_provisioning
     * localconfig key to com.zimbra.cs.account.ldap.custom.CustomLdapProvisioning, 
     * which will use the CustomLdapDIT class that can be customized by a set of
     * localconfig keys.
     * 
     */
    
    /*
     * the provisioning instance that uses thie DIT
     */
    protected Provisioning mProv;
    
    /*
     * Variable Naming Conventions:
     * 
     *              RDN : {attr-name}={attr-value}
     *               DN : List of comma (,) seperated RDNs
     *          DEFAULT : Means the variable has a hardcoded value, which can be referred to 
     *                    from subclasses, but cannot be changed.  If a subclass need to use 
     *                    different values it has to define it's own variables.
     * 
     *         BASE_RDN : A relative RDN under that entries of the same kind reside.
     * DEFAULT_BASE_RDN : A hardcoded BASE_RDN that cannot be changed in subclasses.
     *  NAMING_RDN_ATTR : The attribute for the left-most RDN of an entry. each entry type must have a NAMING_RDN_ATTR.
     *          BASE_DN : An absolute DN under that a left-most RDN resides.
     */
    
    protected static final String ROOT_DN = "";
    
    
    /*
     * Defaults tht can be used in subclasses but cannot be changed in subclasses.
     * If a subclass need to use different values it has to define it's own variables.
     */
    protected final String DEFAULT_CONFIG_BASE_DN        = "cn=zimbra";
    protected final String DEFAULT_MAIL_BASE_DN          = ROOT_DN;
    
    protected final String DEFAULT_BASE_RDN_ADMIN        = "cn=admins";
    protected final String DEFAULT_BASE_RDN_APPADMIN     = "cn=appaccts";
    protected final String DEFAULT_BASE_RDN_ACCOUNT      = "ou=people";
    protected final String DEFAULT_BASE_RDN_COS          = "cn=cos";
    protected final String DEFAULT_BASE_RDN_MIME         = "cn=mime";
    protected final String DEFAULT_BASE_RDN_SERVER       = "cn=servers";
    protected final String DEFAULT_BASE_RDN_XMPPCOMPONENT= "cn=xmppcomponents";
    protected final String DEFAULT_BASE_RDN_ZIMLET       = "cn=zimlets";
    
    
    protected final String DEFAULT_NAMING_RDN_ATTR_USER             = "uid";
    protected final String DEFAULT_NAMING_RDN_ATTR_COS              = "cn";
    protected final String DEFAULT_NAMING_RDN_ATTR_GLOBALCONFIG     = "cn";
    protected final String DEFAULT_NAMING_RDN_ATTR_GLOBALGRANT      = "cn";
    protected final String DEFAULT_NAMING_RDN_ATTR_MIME             = "cn";
    protected final String DEFAULT_NAMING_RDN_ATTR_SERVER           = "cn";
    protected final String DEFAULT_NAMING_RDN_ATTR_XMPPCOMPONENT    = "cn";
    protected final String DEFAULT_NAMING_RDN_ATTR_ZIMLET           = "cn";
    
    /*
     * Variables that has to be set in the init method
     */
    protected String BASE_DN_ZIMBRA;
    protected String BASE_DN_CONFIG_BRANCH;
    protected String BASE_DN_MAIL_BRANCH;

    protected String BASE_RDN_ACCOUNT;
    
    protected String BASE_DN_ADMIN;
    protected String BASE_DN_APPADMIN;
    protected String BASE_DN_ACCOUNT;
    protected String BASE_DN_COS; 
    protected String BASE_DN_MIME;
    protected String BASE_DN_SERVER;
    protected String BASE_DN_XMPPCOMPONENT;
    protected String BASE_DN_ZIMLET;
     
    protected String NAMING_RDN_ATTR_USER;
    protected String NAMING_RDN_ATTR_COS;
    
    protected String NAMING_RDN_ATTR_GLOBALCONFIG;
    protected String NAMING_RDN_ATTR_GLOBALGRANT;
    protected String NAMING_RDN_ATTR_MIME;
    protected String NAMING_RDN_ATTR_SERVER;
    protected String NAMING_RDN_ATTR_XMPPCOMPONENT;
    protected String NAMING_RDN_ATTR_ZIMLET;    

    protected String DN_GLOBALCONFIG;
    protected String DN_GLOBALGRANT;
    
    public LdapDIT(LdapProvisioning prov) {
        // our Provisioning instance
        mProv = prov;  
        
        init();
        verify();
    }
    
    protected void init() {
        BASE_DN_CONFIG_BRANCH = DEFAULT_CONFIG_BASE_DN;
        BASE_DN_MAIL_BRANCH = ROOT_DN;

        BASE_RDN_ACCOUNT  = DEFAULT_BASE_RDN_ACCOUNT;

        NAMING_RDN_ATTR_USER          = DEFAULT_NAMING_RDN_ATTR_USER;
        NAMING_RDN_ATTR_COS           = DEFAULT_NAMING_RDN_ATTR_COS;
        NAMING_RDN_ATTR_GLOBALCONFIG  = DEFAULT_NAMING_RDN_ATTR_GLOBALCONFIG;
        NAMING_RDN_ATTR_GLOBALGRANT   = DEFAULT_NAMING_RDN_ATTR_GLOBALGRANT;
        NAMING_RDN_ATTR_MIME          = DEFAULT_NAMING_RDN_ATTR_MIME;
        NAMING_RDN_ATTR_SERVER        = DEFAULT_NAMING_RDN_ATTR_SERVER;
        NAMING_RDN_ATTR_XMPPCOMPONENT = DEFAULT_NAMING_RDN_ATTR_XMPPCOMPONENT;
        NAMING_RDN_ATTR_ZIMLET        = DEFAULT_NAMING_RDN_ATTR_ZIMLET;
        
        DN_GLOBALCONFIG      = NAMING_RDN_ATTR_GLOBALCONFIG + "=config" + "," + BASE_DN_CONFIG_BRANCH; 
        DN_GLOBALGRANT       = NAMING_RDN_ATTR_GLOBALGRANT  + "=globalgrant" + "," + BASE_DN_CONFIG_BRANCH; 
       
        BASE_DN_ADMIN        = DEFAULT_BASE_RDN_ADMIN         + "," + BASE_DN_CONFIG_BRANCH;
        BASE_DN_APPADMIN     = DEFAULT_BASE_RDN_APPADMIN      + "," + BASE_DN_CONFIG_BRANCH;
        BASE_DN_COS          = DEFAULT_BASE_RDN_COS           + "," + BASE_DN_CONFIG_BRANCH; 
        BASE_DN_MIME         = DEFAULT_BASE_RDN_MIME          + "," + DN_GLOBALCONFIG;
        BASE_DN_SERVER       = DEFAULT_BASE_RDN_SERVER        + "," + BASE_DN_CONFIG_BRANCH;
        BASE_DN_XMPPCOMPONENT= DEFAULT_BASE_RDN_XMPPCOMPONENT + "," + BASE_DN_CONFIG_BRANCH;
        BASE_DN_ZIMLET       = DEFAULT_BASE_RDN_ZIMLET        + "," + BASE_DN_CONFIG_BRANCH;
        
        BASE_DN_ZIMBRA       = ROOT_DN;
        
    }
    
    private final void verify() {
        if (BASE_DN_ZIMBRA == null ||
            BASE_DN_CONFIG_BRANCH == null ||
            BASE_DN_MAIL_BRANCH == null ||
            BASE_RDN_ACCOUNT == null ||
            NAMING_RDN_ATTR_USER == null ||
            NAMING_RDN_ATTR_COS == null ||
            NAMING_RDN_ATTR_GLOBALCONFIG == null ||
            NAMING_RDN_ATTR_GLOBALGRANT == null ||
            NAMING_RDN_ATTR_MIME == null ||
            NAMING_RDN_ATTR_SERVER == null ||
            NAMING_RDN_ATTR_ZIMLET == null ||
            BASE_DN_ADMIN == null ||
            BASE_DN_APPADMIN == null ||
            BASE_DN_COS == null ||
            BASE_DN_MIME == null ||
            BASE_DN_SERVER == null ||
            BASE_DN_XMPPCOMPONENT == null ||
            BASE_DN_ZIMLET == null ||
            DN_GLOBALCONFIG == null ||
            DN_GLOBALGRANT == null)
            Zimbra.halt("Unable to initialize LDAP DIT");
    }
    
    /*
     * Zimbra root
     */
    public String zimbraBaseDN() {
        return BASE_DN_ZIMBRA;
    }
    
    /*
     * config branch
     */
    public String configBranchBaseDN() {
        return BASE_DN_CONFIG_BRANCH;
    }    
    
    /*
     * mail branch
     */
    public String mailBranchBaseDN() {
        return BASE_DN_MAIL_BRANCH;
    }
    
    /*
     * ===========
     *   account
     * ===========
     */    
    public String accountNamingRdnAttr() {
        return NAMING_RDN_ATTR_USER;
    }

    private String emailToDN(String localPart, String domain) throws ServiceException {
        return NAMING_RDN_ATTR_USER + "=" + LdapUtil.escapeRDNValue(localPart) + "," + domainToAccountBaseDN(domain);
    }
    
    private String emailToDN(String email) throws ServiceException {
        String[] parts = EmailUtil.getLocalPartAndDomain(email);
        return emailToDN(parts[0], parts[1]);
    }
    
    public String accountDNCreate(String baseDn, Attributes attrs, String localPart, String domain) throws ServiceException, NamingException {
        // sanity check, the default DIT does not support a supplied base
        if (baseDn != null)
            throw ServiceException.INVALID_REQUEST("base dn is not supported in DIT impl " + getClass().getCanonicalName(), null);

        return emailToDN(localPart, domain);
    }

    
    public String accountDNRename(String oldDn, String newLocalPart, String newDomain) throws ServiceException, NamingException {
        return emailToDN(newLocalPart, newDomain);
    }
    
    /*
     * Given a dn like "uid=foo,ou=people,dc=widgets,dc=com", return the string "foo@widgets.com".
     * 
     * Param attrs is not used in this implementation of DIT
     */
    public String dnToEmail(String dn, Attributes attrs) throws ServiceException {
        String [] parts = dn.split(",");
        StringBuffer domain = new StringBuffer(dn.length());
        
        String namingAttr = accountNamingRdnAttr() + "=";
        String namingAttrValue = null;
        
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].startsWith("dc=")) {
                if (domain.length() > 0)
                    domain.append(".");
                domain.append(LdapUtil.unescapeRDNValue(parts[i].substring(3)));
            } else if (i==0 && parts[i].startsWith(namingAttr)) {
                namingAttrValue = LdapUtil.unescapeRDNValue(parts[i].substring(namingAttr.length()));
            }
        }
        if (namingAttrValue == null)
            throw ServiceException.FAILURE("unable to map dn [" + dn + "] to email", null);
        if (domain.length() == 0)
            return namingAttrValue;
        return new StringBuffer(namingAttrValue).append('@').append(domain).toString();
    }
    
    /*
     * returns the search filter for get all accounts in a domain
     * 
     * if includeObjectClass is true, the filter will include objectclass,
     * if includeObjectClass is false, the filter will notinclude objectclass
     * 
     * false should be passed for searches that already specifies a flag, so 
     * the objectclass will be automatically computed by getObjectClassQuery 
     * in searchObjects, otherwise it will result in an extra & with the object 
     * class (see LdapProvisioning.searchObjects), which will degrade perf. 
     * 
     * domain parameter is not used in default DIT because the search base is 
     * restricted to the domain dn. 
     */
    public String filterAccountsByDomain(Domain domain, boolean includeObjectClass) {
        if (includeObjectClass)
            return "(objectclass=zimbraAccount)";
        else
            return "";
    }
    
   
    /*
     * =================
     *   admin account
     * =================
     */
    public String adminBaseDN() {
        return BASE_DN_ADMIN;
    }
    
    public String appAdminBaseDN() {
        return BASE_DN_APPADMIN;
    }
    
    /*
    public String adminNameToDN(String name) {
        return NAMING_RDN_ATTR_USER + "=" + LdapUtil.escapeRDNValue(name) + "," + BASE_DN_ADMIN;
    }
    */
    
    /*
     * ==========
     *   alias
     * ==========
     */
    public String aliasDN(String targetDn, String targetDomain, String aliasLocalPart, String aliasDomain) throws ServiceException {
        return emailToDN(aliasLocalPart, aliasDomain);
    }
    
    public String aliasDNRename(String targetNewDn, String targetNewDomain, String newAliasEmail) throws ServiceException {
        return emailToDN(newAliasEmail);
    }
   
    /* =================
     * calendar resource
     * =================
     */
    public String filterCalendarResourcesByDomain(Domain domain, boolean includeObjectClass) {
        if (includeObjectClass)
            return "(objectclass=zimbraCalendarResource)";
        else
            return "";
    }
    
    /*
     * =======
     *   COS
     * =======
     */
    public String cosBaseDN() {
        return BASE_DN_COS;
    }
    
    public String cosNametoDN(String name) {
        return NAMING_RDN_ATTR_COS + "=" + LdapUtil.escapeRDNValue(name) + "," + BASE_DN_COS;
    }
    
    /*
     * =====================
     *   distribution list 
     * =====================
     */
    public String distributionListDNCreate(String baseDn, Attributes attrs, String localPart, String domain) throws ServiceException, NamingException {
        // sanity check, the default DIT does not support a supplied base
        if (baseDn != null)
            throw ServiceException.INVALID_REQUEST("base dn is not supported in DIT impl " + getClass().getCanonicalName(), null);

        return emailToDN(localPart, domain);
    }
    
    public String distributionListDNRename(String oldDn, String newLocalPart, String newDomain) throws ServiceException, NamingException {
        return emailToDN(newLocalPart, newDomain);
    }

    public String filterDistributionListsByDomain(Domain domain, boolean includeObjectClass) {
        if (includeObjectClass)
            return "(objectclass=zimbraDistributionList)";
        else
            return "";
    }
    


    /*
     * ==========
     *   domain
     * ==========
     */
    public String domainBaseDN() {
        return mailBranchBaseDN();
    }
    
    /**
     * Given a domain like foo.com, return an array of dns that work their way up the tree:
     *    [0] = dc=foo,dc=com
     *    [1] = dc=com
     * 
     * @return the array of DNs
     */
    public String[] domainToDNs(String[] parts) {
        return domainToDNsInternal(parts, null);
    }
    
    protected String[] domainToDNsInternal(String[] parts, String base) {
        String dns[] = new String[parts.length];
        for (int i=parts.length-1; i >= 0; i--) {
            dns[i] = LdapUtil.domainToDN(parts, i);
            if (base != null)
                dns[i] = dns[i] + "," + base;
        }
        return dns;
    }
    
    // account base search dn
    public String domainToAccountSearchDN(String domain) throws ServiceException {
        return domainDNToAccountBaseDN(LdapUtil.domainToDN(domain));
    }
    
    // account base search dn
    public String domainDNToAccountSearchDN(String domainDN) throws ServiceException {
        return domainDNToAccountBaseDN(domainDN);
    }
    
    // only used internally 
    private String domainToAccountBaseDN(String domain) throws ServiceException {
        return domainDNToAccountBaseDN(LdapUtil.domainToDN(domain));
    }
    
    // account base dn for create/delete domain
    public String domainDNToAccountBaseDN(String domainDN) throws ServiceException {
        if (BASE_RDN_ACCOUNT.length()==0)
            return domainDN;
        else
            return BASE_RDN_ACCOUNT + "," + domainDN;
    }
    
    /*
     * ==============
     *   globalconfig
     * ==============
     */
    public String configDN() {
        return DN_GLOBALCONFIG;
    }
    
    /*
     * =====================
     *   globalgrant
     * =====================
     */
    public String globalGrantDN() {
        return DN_GLOBALGRANT;
    }

   
    /*
     * ========
     *   mime
     * ========
     */
    /*
    public String mimeConfigToDN(String name) {
        name = LdapUtil.escapeRDNValue(name);
        return NAMING_RDN_ATTR_MIME + "=" + name + "," + BASE_DN_MIME;
    }
    */
    
    public String mimeBaseDN() {
        return BASE_DN_MIME;
    }
    
    /*
     * ==========
     *   server
     * ==========
     */
    public String serverBaseDN() {
        return BASE_DN_SERVER;
    }
    
    public String serverNametoDN(String name) {
        return NAMING_RDN_ATTR_SERVER + "=" + LdapUtil.escapeRDNValue(name) + "," + BASE_DN_SERVER;
    }
    
    
    /*
     * ==========
     *   xmppcomponent
     * ==========
     */    
    public String xmppcomponentBaseDN() {
        return BASE_DN_XMPPCOMPONENT;
    }
    
    public String xmppcomponentNameToDN(String name) {
        return NAMING_RDN_ATTR_XMPPCOMPONENT + "=" + LdapUtil.escapeRDNValue(name) + "," + BASE_DN_XMPPCOMPONENT;
    }

    
    /*
     * ==========
     *   zimlet
     * ==========
     */    
    public String zimletBaseDN() {
        return BASE_DN_ZIMLET;
    }
    
    public String zimletNameToDN(String name) {
        return NAMING_RDN_ATTR_ZIMLET + "=" + LdapUtil.escapeRDNValue(name) + "," + BASE_DN_ZIMLET;
    }
    
    
    /*
     * ========================================================================================
     */
    protected SpecialAttrs handleSpecialAttrs(Map<String, Object> attrs) throws ServiceException {
        SpecialAttrs specialAttrs = new SpecialAttrs();
        if (attrs != null) {
            specialAttrs.handleZimbraId(attrs);
            
            // default is don't support pseudo attrs
            // if the pseudo attr is present and not handled here, a NamingExeption will be thrown 
            // when the entry is being created

        }
        return specialAttrs;
    }
    
    public String getNamingRdnAttr(Entry entry) throws ServiceException {
        if (entry instanceof Account ||
            entry instanceof DistributionList ||
            entry instanceof Alias)
            return NAMING_RDN_ATTR_USER;
        else if (entry instanceof Cos)
            return NAMING_RDN_ATTR_COS;
        else if (entry instanceof Config) 
            return NAMING_RDN_ATTR_GLOBALCONFIG;
        else if (entry instanceof DataSource)
            return Provisioning.A_zimbraDataSourceName;
        else if (entry instanceof Domain)   
            return Provisioning.A_dc;
        else if (entry instanceof Identity)
            return Provisioning.A_zimbraPrefIdentityName;   
        else if (entry instanceof GlobalGrant) 
            return NAMING_RDN_ATTR_GLOBALGRANT;
        else if (entry instanceof Server)
            return NAMING_RDN_ATTR_SERVER;
        else if (entry instanceof Zimlet)
            return NAMING_RDN_ATTR_ZIMLET;
        else
            throw ServiceException.FAILURE("entry type " + entry.getClass().getCanonicalName() + " is not supported by getNamingRdnAttr", null);
    }
    

    /**
     * returns if dn is under parentDn
     * 
     * @param parentDn
     * @param dn
     * @return
     */
    public boolean isUnder(String parentDn, String dn) {
        
        if (!parentDn.equals(ROOT_DN)) {
            if (!dn.toLowerCase().endsWith(parentDn.toLowerCase()))
                return false;
        }
        return true;
    }

}
