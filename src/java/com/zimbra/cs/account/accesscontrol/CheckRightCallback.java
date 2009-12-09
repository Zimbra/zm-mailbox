package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Entry;

public abstract class CheckRightCallback {
    protected Right mRight;

    void setRight(Right right) {
        mRight = right;
    }
    
    public Boolean checkRight(Account authedAcct, Entry target, boolean asAdmin) {
        try {
            return doCheckRight(authedAcct, target, asAdmin);
        } catch (ServiceException e) {
            ZimbraLog.acl.warn("caught exception in checkRight callback" +
                    ", checkRight callback for right [" + mRight.getName() +"] skipped", e);
            return null;
        }
    }
    
    protected abstract Boolean doCheckRight(Account grantee, Entry target, boolean asAdmin) throws ServiceException;
    
}
