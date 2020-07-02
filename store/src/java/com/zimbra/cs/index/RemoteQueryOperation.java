/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.base.Strings;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.util.IOUtil;

/**
 * A wrapper around a remote search (a search on data in another account).
 */
final class RemoteQueryOperation extends FilterQueryOperation {

    private ProxiedQueryResults results = null;
    private QueryTarget queryTarget = null;

    @Override
    public long getCursorOffset() {
        return -1;
    }

    /**
     * Try to OR an operation into this one.  Return FALSE if that isn't possible (incompatible query targets).
     *
     * @return FALSE
     */
    boolean tryAddOredOperation(QueryOperation op) {
        Set<QueryTarget> targets = op.getQueryTargets();
        assert(QueryTarget.getExplicitTargetCount(targets) == 1);
        assert(QueryTarget.hasExternalTarget(targets));

        for (QueryTarget target : targets) {
            assert(target != QueryTarget.LOCAL);
            if (target != QueryTarget.UNSPECIFIED) {
                if (queryTarget == null) {
                    queryTarget = target;
                } else if (!queryTarget.equals(target)) {
                    return false;
                }
            }
        }

        assert(queryTarget != null);

        if (operation == null) {
            operation = new UnionQueryOperation();
        }
        ((UnionQueryOperation) operation).add(op);
        return true;
    }

    @Override
    public String toString() {
        return "REMOTE[" + queryTarget + "]:" + operation;
    }

    protected void setup(SoapProtocol proto, AuthToken authToken, SearchParams params) throws ServiceException {
        Provisioning prov  = Provisioning.getInstance();
        Account acct = prov.get(AccountBy.id, queryTarget.toString(), authToken);
        if (acct == null) {
            throw AccountServiceException.NO_SUCH_ACCOUNT(queryTarget.toString());
        }

        Server remoteServer = prov.getServer(acct);

        String queryString = operation.toQueryString();
        String server = Strings.isNullOrEmpty(LC.zimbra_soap_service.value()) ? remoteServer.getName() : LC.zimbra_soap_service.value();
        if (ZimbraLog.search.isDebugEnabled()) {
            ZimbraLog.search.debug("RemoteQuery=\"%s\" target=%s server=%s",
                    queryString, queryTarget, server);
        }
        results = new ProxiedQueryResults(proto, authToken, queryTarget.toString(),
                server, params, queryString, params.getFetchMode());
    }

    @Override
    public void resetIterator() throws ServiceException {
        if (results != null) {
            results.resetIterator();
        }
    }

    @Override
    public ZimbraHit getNext() throws ServiceException {
        return results != null ? results.getNext() : null;
    }

    @Override
    public ZimbraHit peekNext() throws ServiceException {
        return results != null ? results.peekNext() : null;
    }

    @Override
    public void close() throws IOException {
        IOUtil.closeQuietly(results);
        super.close();
    }

    @Override
    public List<QueryInfo> getResultInfo() {
        if (results != null) {
            return results.getResultInfo();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isPreSorted() {
        return results.isPreSorted();
    }
}
