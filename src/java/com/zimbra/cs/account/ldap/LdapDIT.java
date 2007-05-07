package com.zimbra.cs.account.ldap;

import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.EmailUtil;

public class LdapDIT {
    protected String ACCOUNT_REL_BASE;
    protected String ACCOUNT_RDN_ATTR;
    
    public LdapDIT() {
        init();
    }
    
    protected void init() {
        ACCOUNT_REL_BASE = "ou=people";
        ACCOUNT_RDN_ATTR = "uid";
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
    
    public String accountDn(String baseDn, String rdnAttr, Attributes attrs, String domain) throws ServiceException, NamingException {
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
     * ===========
     *   domain
     * ===========
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
