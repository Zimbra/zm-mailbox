/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index;

import java.util.ArrayList;
import java.util.List;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.SoapProtocol;

class RemoteQueryOperation extends FilterQueryOperation {
    
    RemoteQueryOperation() {}

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

        for (QueryTarget t : targets) {
            assert(t != QueryTarget.LOCAL);
            if (t != QueryTarget.UNSPECIFIED) {
                if (mTarget == null) 
                    mTarget = t; 
                else 
                    if (!mTarget.equals(t))
                        return false;
            }
        }

        assert(mTarget != null);

        if (mOp == null)
            mOp = new UnionQueryOperation();

        ((UnionQueryOperation)mOp).add(op);
        return true;
    }

    public String toString() {
        return "REMOTE["+mTarget.toString()+"]:"+mOp.toString();
    }

    protected void setup(SoapProtocol proto, AuthToken authToken, SearchParams params) 
    throws ServiceException {
        Provisioning prov  = Provisioning.getInstance();
        Account acct = prov.get(AccountBy.id, mTarget.toString(), authToken);
        if (acct == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(mTarget.toString());

        Server remoteServer = prov.getServer(acct);

        if (ZimbraLog.index.isDebugEnabled()) 
            ZimbraLog.index.debug("RemoteQuery of \""+mOp.toQueryString()+"\" sent to "+mTarget.toString()+" on server "+remoteServer.getName());

        params.setQueryStr(mOp.toQueryString());
        mResults = new ProxiedQueryResults(proto, authToken, mTarget.toString(), remoteServer.getName(), params, params.getMode());
    }
    
    public void resetIterator() throws ServiceException {
        if (mResults != null)
            mResults.resetIterator();
    }

    public ZimbraHit getNext() throws ServiceException {
        if (mResults != null)
            return mResults.getNext();
        else
            return null;
    }

    public ZimbraHit peekNext() throws ServiceException {
        if (mResults != null)
            return mResults.peekNext();
        else
            return null;
    }

    public void doneWithSearchResults() throws ServiceException {
        if (mResults != null)
            mResults.doneWithSearchResults();
    }
    public List<QueryInfo> getResultInfo() {
        if (mResults != null)
            return mResults.getResultInfo();
        else
            return new ArrayList<QueryInfo>();
    }
    
    public int estimateResultSize() throws ServiceException {
        return mOp.estimateResultSize();
    }
}
