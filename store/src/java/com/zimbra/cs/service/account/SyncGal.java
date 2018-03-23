/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.account;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.gal.GalSearchControl;
import com.zimbra.cs.gal.GalSearchParams;
import com.zimbra.cs.gal.GalSearchResultCallback;
import com.zimbra.cs.servlet.util.JettyUtil;
import com.zimbra.soap.SoapServlet;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.type.GalSearchType;

/**
 * @author schemers
 */
public class SyncGal extends GalDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        disableJettyTimeout(context);
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(getZimbraSoapContext(context));

        if (!canAccessAccount(zsc, account))
            throw ServiceException.PERM_DENIED("can not access account");

        String tokenAttr = request.getAttribute(MailConstants.A_TOKEN, "");
        String galAcctId = request.getAttribute(AccountConstants.A_GAL_ACCOUNT_ID, null);
        boolean idOnly   = request.getAttributeBool(AccountConstants.A_ID_ONLY, false);
        boolean getCount   = request.getAttributeBool(AccountConstants.A_GET_COUNT, false);
        int limit = request.getAttributeInt(MailConstants.A_LIMIT, 0);

        GalSearchParams params = new GalSearchParams(account, zsc);
        params.setType(GalSearchType.all);
        ZimbraLog.gal.debug("SyncGalRequest token: %s  limit: %d", tokenAttr, limit);
        params.setToken(tokenAttr);
        params.setRequest(request);
        params.setResponseName(AccountConstants.SYNC_GAL_RESPONSE);
        params.setIdOnly(idOnly);
        params.setGetCount(getCount);
        params.setUserAgent(zsc.getUserAgent());
        params.setLimit(limit);
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


    /**
     * Implemented for bug 56458..
     *
     * Disable the Jetty timeout for the SelectChannelConnector and the SSLSelectChannelConnector
     * for this request.
     *
     * By default (and our normal configuration) Jetty has a 30 second idle timeout (10 if the server is busy) for
     * connection endpoints. There's another task that keeps track of what connections have timeouts and periodically
     * works over a queue and closes endpoints that have been timed out. This plays havoc with downloads to slow connections
     * and whenever we have a long pause while working to create an archive.
     *
     * This method instructs Jetty not to close the connection when the idle time is reached. Given that we don't send a content-length
     * down to the browser for archive responses, we have to close the socket to tell the browser its done. Since we have to do that..
     * leaving this endpoint without a timeout is safe. If the connection was being reused (ie keep-alive) this could have issues, but its not
     * in this case.
     */
    private void disableJettyTimeout(Map<String, Object> context) {
        if (LC.zimbra_gal_sync_disable_timeout.booleanValue()) {
            Object request = context.get(SoapServlet.SERVLET_REQUEST);
            if (request instanceof HttpServletRequest) {
                JettyUtil.setIdleTimeout(0, (HttpServletRequest) request);
            } else {
                ZimbraLog.misc.warn("no request in context map, cannot disable timeout");
            }
        }
    }
}
