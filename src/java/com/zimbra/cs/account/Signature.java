package com.zimbra.cs.account;

import java.util.Map;
import com.zimbra.common.service.ServiceException;

public class Signature extends NamedEntry implements Comparable {

    private final String mAcctId;
    
    public Signature(Account acct, String name, String id, Map<String, Object> attrs) {
        super(name, id, attrs, null);
        mAcctId = acct.getId();
    }
    
    /**
     * this should only be used internally by the server. it doesn't modify the real id, just
     * the cached one.
     * @param id
     */
    public void setId(String id) {
        mId = id;
        getRawAttrs().put(Provisioning.A_zimbraPrefIdentityId, id);
    }
    
    /*
     * get account of the identity
     */
    public Account getAccount() throws ServiceException {
        return Provisioning.getInstance().get(Provisioning.AccountBy.id, mAcctId);
    }
    
    public String getValue() { 
        return getAttr(Provisioning.A_zimbraPrefMailSignature); 
    }
}
