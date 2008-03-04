package com.zimbra.cs.account.callback;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapProvisioning;


public class ForeignPrincipal extends AttributeCallback {
    
    public void preModify(Map context, String attrName, Object value,
            Map attrsToModify, Entry entry, boolean isCreate) throws ServiceException {
        
        if (entry == null || isCreate)
            return;
        
        if (!(entry instanceof Account))
            return;
            
        Provisioning prov = Provisioning.getInstance();
        if (!(prov instanceof LdapProvisioning))
            return;
        
        Account acct = (Account)entry;
        LdapProvisioning ldapProv = (LdapProvisioning)prov;
        ldapProv.removeFromCache(acct);
    }
    
    public void postModify(Map context, String attrName, Entry entry, boolean isCreate) {
        

    }
    
}
