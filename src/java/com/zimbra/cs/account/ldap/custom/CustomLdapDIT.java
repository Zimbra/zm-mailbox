package com.zimbra.cs.account.ldap.custom;

import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
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
 * 1. Allows a base DN that is common for all the config branch entries:
 *        LDAP bind admin user
 *        cos
 *        global config
 *        mime
 *        server
 *        zimlet
 *        
 *    localconfig key: 
 *        ldap_config_base_dn
 *        
 *        e.g. 
 *            <key name="ldap_config_base_dn">
 *                <value>ou=HPCFG,o=Comcast</value>
 *            </key>
 *    
 *        Entries for each entry type will be created under the ldap_config_base_dn and the 
 *        default RDN under this base dn.  The default RDN for each entry type:
 *        LDAP bind admin user: cn=admins
 *                         cos: cn=cos
 *                        mime: cn=mime
 *                      server: cn=servers
 *                      zimlet: cn=zimlets
 *    
 *        Unlike the above entries, the global config entry has only 1 entry, it is placed 
 *        directly under ldap_config_base_dn.
 *
 * 
 * 2. Allows a base DN for each type of the following entry types:
 *        LDAP bind admin user
 *        cos
 *        mime
 *        server
 *        zimlet
 *        
 *    localconfig keys: 
 *        ldap_admin_base_dn (Note this is for the ldap bind admin only, not for the "Zimbra" admins or domain admins)
 *        ldap_cos_base_dn
 *        ldap_mime_base_dn
 *        ldap_server_base_dn
 *        ldap_zimlet_base_dn
 *        
 *        e.g.
 *            <key name="ldap_admin_base_dn">
 *                <value>cn=users,ou=HPCFG,o=Comcast</value>
 *            </key>
 *                      
 *        - if any of the keys is set, system uses the key for the base DN for that entry type.
 *        - for entry types that don't have a base DN set, system uses the ldap_config_base_dn
 *          and default RDN for the entries, as described in 1. 
 *            
 * 3. Allows a "naming RDN attribute" for each type of the following entry types:
 *    (a "naming RDN attribute" is the attribute name for the left-most RDN of an entry
 *     that is populated with Zimbra attributes)
 *        account
 *        cos
 *        globalconfig
 *        mime
 *        server
 *        zimlet
 *      
 *    localconfig keys:
 *        ldap_account_naming_rdn_attr (default: uid) (This is for all the accounts, including Zimbra accounts and ldap bind admin accounts)
 *        ldap_cos_naming_rdn_attr (default: cn)
 *        ldap_globalconfig_naming_rdn_attr (default: cn)
 *        ldap_mime_naming_rdn_attr (default: cn)
 *        ldap_server_naming_rdn_attr (default: cn)
 *        ldap_zimlet_naming_rdn_attr (default: cn)
 *        
 *        e.g. 
 *            <key name="ldap_account_naming_rdn_attr">
 *                <value>cn</value>
 *            </key>
 *        
 *                      
 * 4. Allows(actually requires) accounts, calendar resources and distribution lists to be 
 *    created under a specified base dn.  This is the major difference between the custom DIT and default DIT.  
 *    
 *    In the default DIT, accounts and DLs are created under the DN of the domain they belong to.  
 *    The DN of a domain is cumputes by each subdomain name.  For example, DN for doamin 
 *    test.mydomain.com will is "dc=test,dc=mydomain,dc=com".  Accounts, calendar resources, DLs,
 *    and their aliases of the domain are placed under a "ou=people" RDN under the domain DN.
 *    For example: "uid=user1,ou=people,dc=test,dc=mydomain,dc=com".
 *    
 *    In the custom DIT:
 *    
 *        Entry create:
 *        - when an account, calendar resource, or DL is begin created, the client *must* provide 
 *          a base DN for the entry begin created.  The provided base DN has to pre-exist.
 *          If the provided DN does not exist, system will throw a ServiceException.
 *        - the "naming RDN attribute" for accounts, calendar resources, and DLs can be specified 
 *          in the ldap_account_naming_rdn_attr localconfig key, as described in 3.  If 
 *          ldap_account_naming_rdn_attr is configured with a value, client *must* provide a 
 *          value for the naming RDN attribute.  For example, if ldap_account_naming_rdn_attr is 
 *          set to "cn", then the value of cn must be included in the create request.  It is client's
 *          responsibility to guarantee the uniqueness of the naming attribute, otherwise a NamingException
 *          will be thrown.  (For now ldap_account_naming_rdn_attr is key for accounts, calendar resources 
 *          and DLs, as well as LDAP bind admin users, if there is a need to distinguish them we can make 
 *          that change later.)
 *            
 *        Aliases: 
 *        - Aliases for accounts, calendar resources, and DLs will be created under the same DN of 
 *          the target entry.
 *        - Aliases has to be in the same domain as the target entry, and the domain has to be the default 
 *          domain(config.zimbraDefaultDomainName).  For example, if the entry is foo@domain1.com, 
 *          it *cannot* have foo-alias@domain2.com, and in for foo@domain1.com to have alias, domain1.com 
 *          has to be the default domain.  This is because alias entries do not carry the domain information.  
 *          In our default DIT, aliases are created under the DN which is computed from the domain, so 
 *          given a dn, we can compute it back to the domain of the alias.  But in the custom DIT, 
 *          aliases are always created under the same DN of the target entry; the dnToEmail function 
 *          always uses zimbraDefaultDomainName for the domain, which is still wrong and will break when 
 *          zimbraDefaultDomainName is changed!  Until we have a real sulotion for that, for now aliases 
 *          in the custom DIT is flakey and this is the best we can do.  Maybe should should just disallow
 *          aliases in the custom DIT.
 *          
 *        Entry rename:   
 *        - When accounts, calendar resources, and DLs are renamed to a different domain, unlike the 
 *          default DIT, the entries will *not* be moved(i.e. deleted then created).  The entries will
 *          stay at the same DIT location, only the related attributes of the entry will be modified.
 *
 */


public class CustomLdapDIT extends LdapDIT {
    zimbra12
    public CustomLdapDIT(LdapProvisioning prov) {
        super(prov);
    }
    
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
        BASE_DN_CONFIG_BRANCH = LC.get("ldap_config_base_dn");

        BASE_RDN_ACCOUNT  = "";

        NAMING_RDN_ATTR_ACCOUNT       = getLC("ldap_account_naming_rdn_attr",      DEFAULT_NAMING_RDN_ATTR_ACCOUNT);
        NAMING_RDN_ATTR_COS           = getLC("ldap_cos_naming_rdn_attr",          DEFAULT_NAMING_RDN_ATTR_COS);
        NAMING_RDN_ATTR_GLOBALCONFIG  = getLC("ldap_globalconfig_naming_rdn_attr", DEFAULT_NAMING_RDN_ATTR_GLOBALCONFIG);
        NAMING_RDN_ATTR_MIME          = getLC("ldap_mime_naming_rdn_attr",         DEFAULT_NAMING_RDN_ATTR_MIME);
        NAMING_RDN_ATTR_SERVER        = getLC("ldap_server_naming_rdn_attr",       DEFAULT_NAMING_RDN_ATTR_SERVER);
        NAMING_RDN_ATTR_ZIMLET        = getLC("ldap_zimlet_naming_rdn_attr",       DEFAULT_NAMING_RDN_ATTR_ZIMLET);
       
        DN_GLOBALCONFIG   = NAMING_RDN_ATTR_GLOBALCONFIG + "=config" + "," + BASE_DN_CONFIG_BRANCH; 

        BASE_DN_ADMIN        = getLC("ldap_admin_base_dn",  DEFAULT_BASE_RDN_ADMIN  + "," + BASE_DN_CONFIG_BRANCH);
        BASE_DN_COS          = getLC("ldap_cos_base_dn",    DEFAULT_BASE_RDN_COS    + "," + BASE_DN_CONFIG_BRANCH); 
        BASE_DN_MIME         = getLC("ldap_mime_base_dn",   DEFAULT_BASE_RDN_MIME   + "," + DN_GLOBALCONFIG);
        BASE_DN_SERVER       = getLC("ldap_server_base_dn", DEFAULT_BASE_RDN_SERVER + "," + BASE_DN_CONFIG_BRANCH);
        BASE_DN_ZIMLET       = getLC("ldap_zimlet_base_dn", DEFAULT_BASE_RDN_ZIMLET + "," + BASE_DN_CONFIG_BRANCH);
    }
    
    
    private ServiceException UNSUPPORTED(String msg) {
        return ServiceException.FAILURE(msg + " unsupported in " + getClass().getCanonicalName(), null);
    }
    
    
    private String defaultDomain() throws ServiceException {
        String defaultDomain = mProv.getConfig().getAttr(Provisioning.A_zimbraDefaultDomainName, null);
        if (StringUtil.isNullOrEmpty(defaultDomain))
           throw UNSUPPORTED("dnToEmail without default domain");
        
        return defaultDomain;
    }
    
    private String acctAndDLDNCreate(String baseDn, Attributes attrs) throws ServiceException, NamingException {
        String rdnAttr = NAMING_RDN_ATTR_ACCOUNT;
        String rdnValue = LdapUtil.getAttrString(attrs, rdnAttr);
        
        if (rdnValue == null)
            throw ServiceException.FAILURE("missing rdn attribute" + rdnAttr, null);

        return rdnAttr + "=" + LdapUtil.escapeRDNValue(rdnValue) + "," + baseDn;
    }
    
    /*
     * ===========
     *   account
     * ===========
     */
    public String emailToDN(String localPart, String domain) throws ServiceException{
        throw UNSUPPORTED("emailToDN");
    }
    
    public String emailToDN(String email) throws ServiceException {
        throw UNSUPPORTED("emailToDN");
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
     * Get email user part attr from attrs and form the email with the default domain
     * 
     * Param attrs is not used in this implementation of DIT
     */
    public String dnToEmail(String dn, Attributes attrs) throws ServiceException, NamingException {
        String localPart = LdapUtil.getAttrString(attrs, DEFAULT_NAMING_RDN_ATTR_ACCOUNT); // which is always uid
                
        return localPart + "@" + defaultDomain();
    }

    
    /*
     * ==========
     *   alias
     * ==========
     */
    public String aliasDN(String targetDn, String targetDomain, String aliasLocalPart, String aliasDomain) throws ServiceException {
        if (targetDn == null || targetDomain == null)
            throw UNSUPPORTED("aliasDN without target dn or target domain");
        
        if (!aliasDomain.equals(defaultDomain()))
            throw UNSUPPORTED("aliasDN not in default domain");
        
        if (!targetDomain.equals(aliasDomain))
            throw UNSUPPORTED("aliasDN with different target domain and alias domain");
        
        /*
         * alias is placed under the same dn as the target
         */
        String[] parts = LdapUtil.dnToRdnAndBaseDn(targetDn);
        return NAMING_RDN_ATTR_ACCOUNT + "=" + LdapUtil.escapeRDNValue(aliasLocalPart) + "," + parts[1];
    }
    
    public String aliasDNRename(String targetNewDn, String targetNewDomain, String newAliasEmail) throws ServiceException {
        if (targetNewDn == null || targetNewDomain == null)
            throw UNSUPPORTED("aliasDNRename without target dn or target domain");
        
        if (!targetNewDomain.equals(defaultDomain()))
            throw UNSUPPORTED("aliasDNRename not in default domain");

        return targetNewDn;
    }
    
    
    /*
     * =====================
     *   distribution list 
     * =====================
     */
    public String distributionListDNCreate(String baseDn, Attributes attrs, String localPart, String domain) throws ServiceException, NamingException {
        if (baseDn == null)
            throw ServiceException.INVALID_REQUEST("base dn is required in DIT impl " + getClass().getCanonicalName(), null);
       
        return acctAndDLDNCreate(baseDn, attrs);
    }
    
    public String distributionListDNRename(String oldDn, String newLocalPart, String newDomain) throws ServiceException, NamingException {
        return oldDn;
    }
    
    
    /*
     * ==========
     *   domain
     * ==========
     */
    // account base search dn
    public String domainToAccountSearchDN(String domain) throws ServiceException {
        return "";
    }
    
    // account base search dn
    public String domainDNToAccountSearchDN(String domainDN) throws ServiceException {
        return "";
    }

    
    /*
     * ========================================================================================
     */
    protected SpecialAttrs handleSpecialAttrs(Map<String, Object> attrs) throws ServiceException {
        
        // check for required attrs
        if (SpecialAttrs.getSingleValuedAttr(attrs, SpecialAttrs.PA_ldapBase) == null)
            throw ServiceException.INVALID_REQUEST("missing required attribute " + SpecialAttrs.PA_ldapBase, null);

        if (!NAMING_RDN_ATTR_ACCOUNT.equals(DEFAULT_NAMING_RDN_ATTR_ACCOUNT)) {
            if (SpecialAttrs.getSingleValuedAttr(attrs, NAMING_RDN_ATTR_ACCOUNT) == null)
                throw ServiceException.INVALID_REQUEST("missing required attribute " + NAMING_RDN_ATTR_ACCOUNT, null);
        }
        
        SpecialAttrs specialAttrs = new SpecialAttrs();
        if (attrs != null) {
            specialAttrs.handleZimbraId(attrs);
            specialAttrs.handleLdapBaseDn(attrs);
        }

            
        return specialAttrs;
    }
}
