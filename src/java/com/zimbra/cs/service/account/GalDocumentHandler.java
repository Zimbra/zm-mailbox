/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.account;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.soap.ZimbraSoapContext;

public abstract class GalDocumentHandler extends AccountDocumentHandler {

    private static final String[] TARGET_ACCOUNT_PATH = new String[] { AccountConstants.A_GAL_ACCOUNT_ID };
    
    protected String[] getProxiedAccountPath() { 
        return TARGET_ACCOUNT_PATH;
    }
    
    @Override
    protected Element proxyIfNecessary(Element request, Map<String, Object> context) 
    throws ServiceException {
        try {
            ZimbraSoapContext zsc = getZimbraSoapContext(context);
            
            Provisioning prov = Provisioning.getInstance();
            
            // check whether we need to proxy to the home server of the GAL sync account
            String[] xpath = getProxiedAccountPath();
            String acctId = (xpath != null ? getXPath(request, xpath) : null);
            if (acctId != null) {
                Account acct = prov.get(AccountBy.id, acctId, zsc.getAuthToken());
                if (acct != null) {
                     if (!Provisioning.onLocalServer(acct)) {
                        /*
                         * bug 69805
                         * 
                         * This is *not* the home server of the galsync account.
                         * 
                         * In a rolling upgrade env, when galsync account resides on a 6.x 
                         * server and user resides on a 7.x server, 6.x server proxies on 
                         * the requested account, not galAccountId.  If we get here, and 
                         * galAcctProxied is true, it means this request was first proxied 
                         * by GalSearchControl to the home server of the galsync account, 
                         * which must be a pre-7.x server, otherwise the request would have 
                         * been handled on that server and would not be proxied back to 
                         * this server again).  The pre-7.x server proxied the request 
                         * back to this server based on the requested account, which is 
                         * the users home server.
                         * 
                         * If we just proxy back to the pre-7.x server, it will get into 
                         * a proxy loop (or PERM_DENIED if the requested account is altered 
                         * to the galsync account).
                         * 
                         * If we are in this situation, just use the 6.x behavior of setting 
                         * <authToken> to the global admin, and setting <account> (i.e. the 
                         * requested account) to the galsync account.
                         */
                        boolean proxied = request.getAttributeBool(AccountConstants.A_GAL_ACCOUNT_PROXIED, false);
                        if (proxied) {
                            /*
                             * This is the rolling upgrade path, when galsync account is 
                             * on a pre-7.x server and this is a 7.x-or-later server.
                             * Just do the pre-7.x behavior for <authToken> and <account> 
                             * in soap header.
                             *
                             * Note: we should never get here from another 7.x-or-later server/client.
                             *
                             * This proxyRequest call will alter the auth token to the 
                             * global admin and alter the requested account to the galsync 
                             * account; and proxy this request to the home server of 
                             * acctId(i.e. the galsync account), 
                             */
                            return proxyRequest(request, context, AuthProvider.getAdminAuthToken(), acctId);
                        } else {
                            /*
                             * normal path
                             *
                             * The original request has the galAcctId attr on soap body, 
                             * just proxy to the home server of the galAcctId server.
                             *
                             * note: do *not* pass acctId to proxyRequest! Doing that will 
                             * change the requested account to the account by that acctId, 
                             * which is the galsync account.  We should preserve those in 
                             * the original request when proxying.
                             */
                            Server server = acct.getServer();
                            return proxyRequest(request, context, server);
                        }
                    } else {
                        // galAcctId is on local server
                        return null;
                    }
                }
            }
            
            // galAcctId is not present, or is present but not found(should throw?)
            return super.proxyIfNecessary(request, context);
        } catch (ServiceException e) {
            // if something went wrong proxying the request, just execute it locally
            if (ServiceException.PROXY_ERROR.equals(e.getCode())) {
                return null;
            }
            // but if it's a real error, it's a real error
            throw e;
        }
    }

}
