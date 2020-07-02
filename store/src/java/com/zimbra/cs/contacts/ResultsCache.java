package com.zimbra.cs.contacts;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;

/**
 * Implementations of this class are used for storing/retrieving known contact affinity results.
 */
public abstract class ResultsCache {

    protected abstract RelatedContactsResults get(RelatedContactsParams params) throws ServiceException;

    public RelatedContactsResults getFromCache(RelatedContactsParams params) throws ServiceException {
        RelatedContactsResults results = get(params);
        if (results == null || isExpired(results)) {
            return null;
        } else {
            return results;
        }
    }

    public abstract void storeInCache(RelatedContactsResults contacts) throws ServiceException;

    protected boolean isExpired(RelatedContactsResults results) {
        Account acct;
        try {
            acct = Provisioning.getInstance().getAccountById(results.getQueryParams().getAccountId());
        } catch (ServiceException e) {
            ZimbraLog.contact.error("unable to determing zimbraRelatedContactsMaxAge, keeping cache entry");
            return false;
        }
        long maxResultsAge = acct.getRelatedContactsMaxAge();
        return System.currentTimeMillis() - maxResultsAge > results.getDate();
    }
}
