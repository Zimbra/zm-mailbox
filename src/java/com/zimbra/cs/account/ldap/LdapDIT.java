package com.zimbra.cs.account.ldap;

import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.EmailUtil;
import com.zimbra.cs.util.Zimbra;

public class LdapDIT {
    
    /*
     * Vatiable Naming Conventions:
     * 
     *              RDN : {attr-name}={attr-value}
     *               DN : List of comma (,) seperated RDNs
     *          DEFAULT : Means the variable has a hardcoded value, which can be referred to 
     *                    from subclasses, but cannot be changed.  If a subclass need to use 
     *                    different values it has to define it's own variables.
     * 
     *         BASE_RDN : A relative RDN under that entries of the same kind reside.
     * DEFAULT_BASE_RDN : A hardcoded BASE_RDN that cannot be changed in subclasses.
     *  NAMING_RDN_ATTR : The attribute for the left-most RDN of an entry.
     *          BASE_DN : An absolute DN under that a left-most RDN resides.
     */
    
    /*
     * Defaults tht can be used in subclasses but cannot be changed in subclasses.
     * If a subclass need to use different values it has to define it's own variables.
     */
    protected final String DEFAULT_BASE_RDN_ADMIN        = "cn=admins";
    protected final String DEFAULT_BASE_RDN_COS          = "cn=cos";
    protected final String DEFAULT_BASE_RDN_GLOBALCONFIG = "cn=config";
    protected final String DEFAULT_BASE_RDN_MIME         = "cn=mime";
    protected final String DEFAULT_BASE_RDN_SERVER       = "cn=servers";
    protected final String DEFAULT_BASE_RDN_ZIMLET       = "cn=zimlets";
    
    /*
     * Variables that has to be set in the init method
     */
    protected String BASE_RDN_ACCOUNT;
    
    protected String NAMING_RDN_ATTR_ACCOUNT;
    protected String NAMING_RDN_ATTR_COS;
    protected String NAMING_RDN_ATTR_MIME;
    protected String NAMING_RDN_ATTR_SERVER;
    protected String NAMING_RDN_ATTR_ZIMLET;
    
    protected String BASE_DN_ADMIN;
    protected String BASE_DN_COS; 
    protected String BASE_DN_GLOBALCONFIG;     
    protected String BASE_DN_MIME;
    protected String BASE_DN_SERVER;
    protected String BASE_DN_ZIMLET;
    
    public LdapDIT() {
        init();
        verify();
    }
    
    protected void init() {
        BASE_RDN_ACCOUNT = "ou=people";

        NAMING_RDN_ATTR_ACCOUNT = "uid";
        NAMING_RDN_ATTR_COS     = "cn";
        NAMING_RDN_ATTR_MIME    = "cn";
        NAMING_RDN_ATTR_SERVER  = "cn";
        NAMING_RDN_ATTR_ZIMLET  = "cn";
        
        String configBase = "cn=zimbra";
       
        BASE_DN_ADMIN        = DEFAULT_BASE_RDN_ADMIN        + "," + configBase;
        BASE_DN_COS          = DEFAULT_BASE_RDN_COS          + "," + configBase; 
        BASE_DN_GLOBALCONFIG = DEFAULT_BASE_RDN_GLOBALCONFIG + "," + configBase;     
        BASE_DN_MIME         = DEFAULT_BASE_RDN_MIME         + "," + BASE_DN_GLOBALCONFIG;
        BASE_DN_SERVER       = DEFAULT_BASE_RDN_SERVER       + "," + configBase;
        BASE_DN_ZIMLET       = DEFAULT_BASE_RDN_ZIMLET       + "," + configBase;
    }
    
    private final void verify() {
        if (BASE_RDN_ACCOUNT == null ||
            NAMING_RDN_ATTR_ACCOUNT == null ||
            NAMING_RDN_ATTR_COS == null ||
            NAMING_RDN_ATTR_MIME == null ||
            NAMING_RDN_ATTR_SERVER == null ||
            NAMING_RDN_ATTR_ZIMLET == null ||
            BASE_DN_ADMIN == null ||
            BASE_DN_COS == null ||
            BASE_DN_GLOBALCONFIG == null ||  
            BASE_DN_MIME == null ||
            BASE_DN_SERVER == null ||
            BASE_DN_ZIMLET == null)
            Zimbra.halt("Unable to initialize LDAP DIT");
    }
    
    /*
     * ===========
     *   account
     * ===========
     */

    // TODO deprecate and fix all call points that still use it
    public String emailToDN(String localPart, String domain) {
        return NAMING_RDN_ATTR_ACCOUNT + "=" + LdapUtil.escapeRDNValue(localPart) + "," + domainToAccountBaseDN(domain);
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
            rdnAttr = NAMING_RDN_ATTR_ACCOUNT;
        
        String rdnValue = LdapUtil.getAttrString(attrs, rdnAttr);
        
        if (rdnValue == null)
            throw ServiceException.FAILURE("missing rdn attribute" + rdnAttr, null);

        return rdnAttr + "=" + LdapUtil.escapeRDNValue(rdnValue) + "," + baseDn;
    }
    
   
    /*
     * =================
     *   admin account
     * =================
     */
    public String adminBaseDN() {
        return BASE_DN_ADMIN;
    }
    
    public String adminNameToDN(String name) {
        return NAMING_RDN_ATTR_ACCOUNT + "=" + LdapUtil.escapeRDNValue(name) + "," + BASE_DN_ADMIN;
    }
    
    
    /*
     * ==========
     *   alias
     * ==========
     */
    public String aliasDN(String targetEntryDn, String aliasLocalPart, String aliasDomain) {
        return emailToDN(aliasLocalPart, aliasDomain);
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
    
    
    /*
     * ==========
     *   domain
     * ==========
     */
    // TODO deprecate and fix all call points that still use it
    public String domainToAccountBaseDN(String domain) {
        return BASE_RDN_ACCOUNT + "," + LdapUtil.domainToDN(domain);
    }

    // TODO deprecate and fix all call points that still use it
    public String domainToAccountBaseDN(LdapDomain domain) {
        return BASE_RDN_ACCOUNT + "," + domain.getDN();
    }

    // TODO deprecate and fix all call points that still use it
    public String domainDNToAccountBaseDN(String domainDN) {
        return BASE_RDN_ACCOUNT + "," + domainDN;
    }
    
    
    /*
     * ==============
     *   globalconfig
     * ==============
     */
    public String configDN() {
        return BASE_DN_GLOBALCONFIG;
    }

   
    /*
     * ========
     *   mime
     * ========
     */
    public String mimeConfigToDN(String name) {
        name = LdapUtil.escapeRDNValue(name);
        return NAMING_RDN_ATTR_MIME + "=" + name + "," + BASE_DN_MIME;
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
     *   zimlet
     * ==========
     */    
    public String zimletNameToDN(String name) {
        return NAMING_RDN_ATTR_ZIMLET + "=" + LdapUtil.escapeRDNValue(name) + "," + BASE_DN_ZIMLET;
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
