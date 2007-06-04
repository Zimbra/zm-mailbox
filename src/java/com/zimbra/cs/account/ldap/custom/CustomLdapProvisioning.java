package com.zimbra.cs.account.ldap.custom;

import java.util.HashMap;
import java.util.Map;

import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.EmailUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.ldap.LdapUtil;

public class CustomLdapProvisioning extends LdapProvisioning {
    
    protected void setDIT() {
        mDIT = new CustomLdapDIT(this);
    }
    
    /*
     * For velodrome, accounts and calander resources are created under CC provided based dn 
     * and rdn.  Until further requirements are given, for now renameAccount for velodrome will 
     * just repalce all the email addresses of the account and replace the addresses in all
     * distribution lists.  The account entry and all its aliases will remain at the same DIT 
     * location.
     */
    /*
    public void renameAccount(String zimbraId, String newName) throws ServiceException {
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext(true);
            
            Account acct = getAccountById(zimbraId);
            if (acct == null)
                throw AccountServiceException.NO_SUCH_ACCOUNT(zimbraId);
        
            String oldEmail = acct.getName();
            String oldDomain = EmailUtil.getValidDomainPart(oldEmail);

            newName = newName.toLowerCase().trim();
            String[] parts = EmailUtil.getLocalPartAndDomain(newName);
            if (parts == null)
                throw ServiceException.INVALID_REQUEST("bad value for newName", null);
            String newLocal = parts[0];
            String newDomain = parts[1];

        
            HashMap<String, Object> newAttrs = new HashMap<String, Object>();
            newAttrs.put(Provisioning.A_uid, newLocal);
            newAttrs.put(Provisioning.A_zimbraMailDeliveryAddress, newName);
        
            ReplaceAddressResult replacedMails = replaceMailAddresses(acct, Provisioning.A_mail, oldEmail, newName);
            if (replacedMails.newAddrs().length == 0) {
                // Set mail to newName if the account currently does not have a mail
                newAttrs.put(Provisioning.A_mail, newName);
            } else {
                newAttrs.put(Provisioning.A_mail, replacedMails.newAddrs());
            }
        
            boolean domainChanged = !oldDomain.equals(newDomain);
            ReplaceAddressResult replacedAliases = replaceMailAddresses(acct, Provisioning.A_zimbraMailAlias, oldEmail, newName);
            if (replacedAliases.newAddrs().length > 0) {
                newAttrs.put(Provisioning.A_zimbraMailAlias, replacedAliases.newAddrs());
            
                String newDomainDN = mDIT.domainToAccountBaseDN(newDomain);
            
                // check up front if any of renamed aliases already exists in the new domain (if domain also got changed)
                if (domainChanged && addressExists(ctxt, newDomainDN, replacedAliases.newAddrs()))
                    throw AccountServiceException.ACCOUNT_EXISTS(newName);    
            }
        
            modifyAttrs(acct, newAttrs);
            
            // rename the distribution list and all it's renamed aliases to the new name in all distribution lists
            // doesn't throw exceptions, just logs
            renameAddressesInAllDistributionLists(oldEmail, newName, replacedAliases);
            } finally {
                LdapUtil.closeContext(ctxt);
            }
    }
    */
         

}
