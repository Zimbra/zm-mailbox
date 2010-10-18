/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.account;

import java.util.Map;

import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.gal.GalSearchControl;
import com.zimbra.cs.gal.GalSearchParams;
import com.zimbra.cs.gal.GalSearchResultCallback;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class SyncGal extends AccountDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(getZimbraSoapContext(context));

        String tokenAttr = request.getAttribute(MailConstants.A_TOKEN, "");
        String galAcctId = request.getAttribute(AccountConstants.A_ID, null);
        boolean idOnly   = request.getAttributeBool(AccountConstants.A_ID_ONLY, false);

        GalSearchParams params = new GalSearchParams(account, zsc);
        params.setType(Provisioning.GalSearchType.all);
        params.setToken(tokenAttr);
        params.setRequest(request);
        params.setResponseName(AccountConstants.SYNC_GAL_RESPONSE);
        params.setIdOnly(idOnly);
        if (galAcctId != null)
        	params.setGalSyncAccount(Provisioning.getInstance().getAccountById(galAcctId));
        params.setResultCallback(new SyncGalCallback(params));
        
        GalSearchControl gal = new GalSearchControl(params);
        gal.sync();
        return params.getResultCallback().getResponse();
    }

    @Override
    public boolean needsAuth(Map<String, Object> context) {
        return true;
    }
    
    // bug 51189, return zimbraMailForwardingAddress for groups members
    // for pre 7.0 ZCO/ZCB clients
    private static class SyncGalCallback extends GalSearchResultCallback {
        private static final String UA_ZCO = "ZimbraConnectorForOutlook";
        private static final String UA_ZCB = "ZimbraConnectorForBES";
        
        boolean mNeedPreHelixCompatibility;
        
        private SyncGalCallback(GalSearchParams params) {
            super(params);
            
            ZimbraSoapContext zsc = params.getSoapContext();
            if (zsc != null) {
                String ua = zsc.getUserAgent();
                
                // user agent is in the format of: name + "/" + version;
                // ZCO: ZimbraConnectorForOutlook/7.0.0.0
                // ZCB: ZimbraConnectorForBES/7.0.0.0
                if (ua != null) {
                    String[] parts = ua.split("/");
                    if (parts.length == 2) {
                        String app = parts[0];
                        String version = parts[1];
                        
                        if (UA_ZCO.equalsIgnoreCase(app) || UA_ZCB.equalsIgnoreCase(app)) {
                            String[] release = version.split("\\.");
                            if (release.length >= 1) {
                                try {
                                    int major = Integer.parseInt(release[0]);
                                    if (major < 7)
                                        mNeedPreHelixCompatibility = true;
                                } catch (NumberFormatException e) {
                                    ZimbraLog.gal.debug("unable to parse user agent version " + version, e);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        /*
         * no need to fixup for the gal sync accont path, pre-helix ZCO/ZCB clients has not 
         * correctly adapted the sync token format that will lead to gal sync account search.
         * 
        @Override
        public Element handleContact(Contact c) throws ServiceException {
        }
        */
        
        @Override
        public void handleContact(GalContact c) throws ServiceException {
            if (mNeedPreHelixCompatibility && c.isGroup()) {
                boolean isZimbraGroup = c.getSingleAttr(Provisioning.A_zimbraId) != null;
                if (isZimbraGroup) {
                    Map<String, Object> attrs = c.getAttrs();
                    Object member = attrs.get(ContactConstants.A_member);
                    Object mailForwardingAddress = attrs.get(Provisioning.A_zimbraMailForwardingAddress);
                    if (member != null && mailForwardingAddress == null) {
                        attrs.put(Provisioning.A_zimbraMailForwardingAddress, member);
                        attrs.remove(ContactConstants.A_member);
                    }
                }
            }
            super.handleContact(c);
        }
    }
}
