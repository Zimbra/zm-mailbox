package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapProvisioning;

public class AdminGroup extends AttributeCallback {

    @Override
    public void preModify(Map context, String attrName, Object attrValue,
            Map attrsToModify, Entry entry, boolean isCreate)
            throws ServiceException {
        // TODO Auto-generated method stub

    }
    
    @Override
    public void postModify(Map context, String attrName, Entry entry,
            boolean isCreate) {
        
        if (!(entry instanceof DistributionList))
            return;
             
        Provisioning prov = Provisioning.getInstance();
        if (!(prov instanceof LdapProvisioning))
            return;
        
        DistributionList group = (DistributionList)entry;
        LdapProvisioning ldapProv = (LdapProvisioning)prov;
        ldapProv.removeFromCache(group);
    }
}
