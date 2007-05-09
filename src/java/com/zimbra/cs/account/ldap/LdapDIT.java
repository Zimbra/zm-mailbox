package com.zimbra.cs.account.ldap;

import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.EmailUtil;

public class LdapDIT {
    
    protected String ACCOUNT_REL_BASE;
    protected String ACCOUNT_RDN_ATTR;
    protected String ADMIN_BASE;
    protected String CONFIG_BASE;     
    protected String COS_BASE; 
    protected String SERVER_BASE;
    protected String ZIMLET_BASE;
    
    public LdapDIT() {
        init();
    }
    
    protected void init() {
        ACCOUNT_REL_BASE = "ou=people";
        ACCOUNT_RDN_ATTR = "uid";
        ADMIN_BASE = "cn=admins,cn=zimbra";
        CONFIG_BASE = "cn=config,cn=zimbra";     
        COS_BASE = "cn=cos,cn=zimbra"; 
        SERVER_BASE = "cn=servers,cn=zimbra";
        ZIMLET_BASE = "cn=zimlets,cn=zimbra";
    }
    
    /*
     * ===========
     *   account
     * ===========
     */

    // TODO deprecate and fix all call points that still use it
    public String emailToDN(String localPart, String domain) {
        return ACCOUNT_RDN_ATTR + "=" + LdapUtil.escapeRDNValue(localPart) + "," + domainToAccountBaseDN(domain);
    }
    
    // TODO deprecate and fix all call points that still use it
    public String emailToDN(String email) {
        String[] parts = EmailUtil.getLocalPartAndDomain(email);
        return emailToDN(parts[0], parts[1]);
    }
    
    public String accountDN(String baseDn, String rdnAttr, Attributes attrs, String domain) throws ServiceException, NamingException {
        if (baseDn == null)
            baseDn = domainToAccountBaseDN(domain);
        
        if (rdnAttr == null)
            rdnAttr = ACCOUNT_RDN_ATTR;
        
        String rdnValue = LdapUtil.getAttrString(attrs, rdnAttr);
        
        if (rdnValue == null)
            throw ServiceException.FAILURE("missing rdn attribute" + rdnAttr, null);

        return rdnAttr  + "=" + LdapUtil.escapeRDNValue(rdnValue) + "," + baseDn;
    }
    
   
    /*
     * =================
     *   admin account
     * =================
     */
    public String adminBaseDN() {
        return ADMIN_BASE;
    }
    
    public String adminNameToDN(String name) {
        return ACCOUNT_RDN_ATTR + "=" + LdapUtil.escapeRDNValue(name) + ","+ADMIN_BASE;
    }
    
    
    /*
     * ==========
     *   alias
     * ==========
     */
    public String aliasDN(String entryDn, String aliasLocalPart, String aliasDomain) {
        return emailToDN(aliasLocalPart, aliasDomain);
    }
    
    
    /*
     * ==========
     *   config
     * ==========
     */
    public String configDN() {
        return CONFIG_BASE;
    }
    
    
    /*
     * =======
     *   COS
     * =======
     */
    public String cosBaseDN() {
        return COS_BASE;
    }
    
    public String cosNametoDN(String name) {
        return "cn=" + LdapUtil.escapeRDNValue(name) + ","+COS_BASE;
    }
    
    /*
     * =====================
     *   distribution list 
     * =====================
     */
    
    
    /*
     * ==========
     *   domain
     * ==========
     */
    // TODO deprecate and fix all call points that still use it
    public String domainToAccountBaseDN(String domain) {
        return ACCOUNT_REL_BASE +","+LdapUtil.domainToDN(domain);
    }

    // TODO deprecate and fix all call points that still use it
    public String domainToAccountBaseDN(LdapDomain domain) {
        return ACCOUNT_REL_BASE +","+domain.getDN();
    }

    // TODO deprecate and fix all call points that still use it
    public String domainDNToAccountBaseDN(String domainDN) {
        return ACCOUNT_REL_BASE +","+domainDN;
    }

   
    /*
     * ========
     *   mime
     * ========
     */
    public String mimeConfigToDN(String name) {
        name = LdapUtil.escapeRDNValue(name);
        return "cn=" + name + ",cn=mime," + CONFIG_BASE;
    }
    
    
    /*
     * ==========
     *   server
     * ==========
     */
    public String serverBaseDN() {
        return SERVER_BASE;
    }
    
    public String serverNametoDN(String name) {
        return "cn=" + LdapUtil.escapeRDNValue(name) + ","+SERVER_BASE;
    }
    
    
    /*
     * ==========
     *   zimlet
     * ==========
     */    
    public String zimletNameToDN(String name) {
        return "cn=" + LdapUtil.escapeRDNValue(name) + ","+ZIMLET_BASE;
    }
    
    
    
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

}
