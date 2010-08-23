/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index;

import java.util.Collections;
import java.util.List;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.SoapProtocol;

/**
 * A wrapper around a remote search (a search on data in another account).
 */
final class RemoteQueryOperation extends FilterQueryOperation {

    private ProxiedQueryResults mResults = null;
    private QueryTarget mTarget = null;

    /**
     * Try to OR an operation into this one.  Return FALSE if that isn't
     * possible (incompatible query targets)
     *
     * @return FALSE
     */
    boolean tryAddOredOperation(QueryOperation op) {
        QueryTargetSet targets = op.getQueryTargets();
        assert(targets.countExplicitTargets() == 1);
        assert(targets.hasExternalTargets());

        for (QueryTarget target : targets) {
            assert(target != QueryTarget.LOCAL);
            if (target != QueryTarget.UNSPECIFIED) {
                if (mTarget == null) {
                    mTarget = target;
                } else {
                    if (!mTarget.equals(target)) {
                        return false;
                    }
                }
            }
        }

        assert(mTarget != null);

        if (mOp == null) {
            mOp = new UnionQueryOperation();
        }
        ((UnionQueryOperation) mOp).add(op);
        return true;
    }

    @Override
    public String toString() {
        return "REMOTE[" + mTarget + "]:" + mOp;
    }

    protected void setup(SoapProtocol proto, AuthToken authToken, SearchParams params)
        throws ServiceException {

        Provisioning prov  = Provisioning.getInstance();
        Account acct = prov.get(AccountBy.id, mTarget.toString(), authToken);
        if (acct == null) {
            throw AccountServiceException.NO_SUCH_ACCOUNT(mTarget.toString());
        }

        Server remoteServer = prov.getServer(acct);

        if (ZimbraLog.index_search.isDebugEnabled()) {
            ZimbraLog.index_search.debug("RemoteQuery=\"%s\" target=%s server=%s",
                    mOp.toQueryString(), mTarget, remoteServer.getName());
        }

        String queryString = mOp.toQueryString();
        mResults = new ProxiedQueryResults(proto, authToken, mTarget.toString(),
                remoteServer.getName(), params, queryString, params.getMode());
    }

    @Override
    public void resetIterator() throws ServiceException {
        if (mResults != null) {
            mResults.resetIterator();
        }
    }

    @Override
    public ZimbraHit getNext() throws ServiceException {
        return mResults != null ? mResults.getNext() : null;
    }

    @Override
    public ZimbraHit peekNext() throws ServiceException {
        return mResults != null ? mResults.peekNext() : null;
    }

    @Override
    public void doneWithSearchResults() throws ServiceException {
        if (mResults != null) {
            mResults.doneWithSearchResults();
        }
        super.doneWithSearchResults();
    }

    @Override
    public List<QueryInfo> getResultInfo() {
        if (mResults != null) {
            return mResults.getResultInfo();
        } else {
            return Collections.emptyList();
        }
    }
}
