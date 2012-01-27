/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
package com.zimbra.cs.service.account;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
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
                        return proxyRequest(request, context, acctId);
                    } else {
                        return null;
                    }
                }
            }
            
            return super.proxyIfNecessary(request, context);
        } catch (ServiceException e) {
            // if something went wrong proxying the request, just execute it locally
            if (ServiceException.PROXY_ERROR.equals(e.getCode()))
                return null;
            // but if it's a real error, it's a real error
            throw e;
        }
    }

}
