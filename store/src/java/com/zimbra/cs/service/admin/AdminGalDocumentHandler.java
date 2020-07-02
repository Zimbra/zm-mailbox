/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.soap.ZimbraSoapContext;

public abstract class AdminGalDocumentHandler extends AdminDocumentHandler {
    private static final String[] TARGET_ACCOUNT_PATH = new String[] { AccountConstants.A_GAL_ACCOUNT_ID };

    @Override
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
                         * bug 69805, see comments in com.zimbra.cs.service.account.GalDocumentHandler
                         */
                        boolean proxied = request.getAttributeBool(AccountConstants.A_GAL_ACCOUNT_PROXIED, false);
                        if (proxied) {
                            /*
                             * This is the rolling upgrade path, when galsync account is 
                             * on a pre-7.x server and this is a 7.x-or-later server.
                             * Just do the pre-7.x behavior for <authToken> and <account> 
                             * in soap header.
                             */
                            return proxyRequest(request, context, AuthProvider.getAdminAuthToken(), acctId);
                        } else {
                            /*
                             * normal path
                             */
                            return proxyRequest(request, context, acctId);
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
