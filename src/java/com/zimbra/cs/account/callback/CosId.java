package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapProvisioning;

public class CosId extends AttributeCallback {

    @Override
    public void preModify(Map context, String attrName, Object attrValue,
            Map attrsToModify, Entry entry, boolean isCreate)
            throws ServiceException {
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

    @Override
    public void postModify(Map context, String attrName, Entry entry,
            boolean isCreate) {
        // TODO Auto-generated method stub

    }
}
