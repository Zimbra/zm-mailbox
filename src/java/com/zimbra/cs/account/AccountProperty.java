package com.zimbra.cs.account;

import java.util.Map;

import com.zimbra.common.service.ServiceException;

public abstract class AccountProperty extends NamedEntry {
    private final String mAcctId;
    
    AccountProperty(Account acct, String name, String id, Map<String, Object> attrs, Map<String, Object> defaults) {
        super(name, id, attrs, null);
        mAcctId = acct.getId();
    }
    
    public String getAccountId() {
        return mAcctId;
    }
    
    public Account getAccount() throws ServiceException{
        return Provisioning.getInstance().get(Provisioning.AccountBy.id, mAcctId);
    }
    
    public Account getAccount(Provisioning prov) throws ServiceException{
        return prov.get(Provisioning.AccountBy.id, mAcctId);
    }
}
