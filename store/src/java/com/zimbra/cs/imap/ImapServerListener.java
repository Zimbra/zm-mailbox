/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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
package com.zimbra.cs.imap;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.soap.W3cDomUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.admin.message.AdminCreateWaitSetRequest;
import com.zimbra.soap.admin.message.AdminCreateWaitSetResponse;
import com.zimbra.soap.admin.message.AdminDestroyWaitSetRequest;
import com.zimbra.soap.admin.message.AdminWaitSetRequest;
import com.zimbra.soap.admin.message.AdminWaitSetResponse;
import com.zimbra.soap.mail.message.WaitSetRequest;
import com.zimbra.soap.mail.message.WaitSetResponse;
import com.zimbra.soap.type.Id;
import com.zimbra.soap.type.WaitSetAddSpec;

public class ImapServerListener {
    private final String server;
    private volatile String wsID = null;
    private final ConcurrentMap<String, ImapRemoteSession> accountToSessionMap = new ConcurrentHashMap<String, ImapRemoteSession>();
    private final ConcurrentMap<String, Integer> lastSequence = new ConcurrentHashMap<String, Integer>();
    private final SoapProvisioning soapProv = new SoapProvisioning();
    private Future<HttpResponse> pendingRequest;

    ImapServerListener(String svr) throws ServiceException {
        this.server = svr;
        soapProv.soapSetURI(URLUtil.getAdminURL(Provisioning.getInstance().getServerByName(server)));
    }

    private void checkAuth() throws ServiceException {
        if(soapProv.isExpired()) {
            soapProv.soapZimbraAdminAuthenticate();
        }
    }

    public void shutdown() {
        synchronized(soapProv) {
            try {
                deleteWaitSet();
                soapProv.soapLogOut();
            } catch (ServiceException e) {
                ZimbraLog.imap.error("Failed to log out from admin SOAP session", e);
            }
        }
    }

    public void addListener(ImapRemoteSession listener) throws ServiceException {
        accountToSessionMap.put(listener.getTargetAccountId(), listener);
        if(wsID == null) {
            //create a waitset
            initWaitSet(listener.getTargetAccountId());
        } else {
            //add to existing waitset
        }
    }

    public void removeListener(ImapRemoteSession listener) throws ServiceException {
        accountToSessionMap.remove(listener.getTargetAccountId());
        if(accountToSessionMap.isEmpty()) {
            deleteWaitSet();
        } else {
            
        }
    }

    public boolean isListeningOn(String accountId) {
        return accountToSessionMap.containsKey(accountId);
    }

    private void initWaitSet(String accountId) throws ServiceException {
        synchronized(soapProv) {
            if(wsID == null) {
                AdminCreateWaitSetRequest req = new AdminCreateWaitSetRequest("all", false);
                WaitSetAddSpec add = new WaitSetAddSpec();
                add.setId(accountId);
                req.addAccount(add);
                checkAuth();
                AdminCreateWaitSetResponse resp = soapProv.invokeJaxb(req, server);
                wsID = resp.getWaitSetId();
                lastSequence.put(wsID, resp.getSequence());
            }
            //send asynchronous WaitSetRequest
            AdminWaitSetRequest waitSetReq = new AdminWaitSetRequest(wsID, Integer.toString(lastSequence.get(wsID)));
            WaitSetAddSpec add = new WaitSetAddSpec();
            add.setId(accountId);
            waitSetReq.addAddAccount(add);
            cancelPendingRequest();
            pendingRequest = soapProv.invokeJaxbAsync(waitSetReq, server, cb);
        }
    }

    private void deleteWaitSet() throws ServiceException {
        cancelPendingRequest();

        if(wsID != null) {
            AdminDestroyWaitSetRequest req = new AdminDestroyWaitSetRequest(wsID);
            checkAuth();
            synchronized(soapProv) {
                soapProv.invokeJaxb(req);
            }
            if(lastSequence.containsKey(wsID)) {
                lastSequence.remove(wsID);
            }
            wsID = null;
        }
    }

    private void continueWaitSet() {
        cancelPendingRequest();
        if(wsID != null) {
            //send asynchronous WaitSetRequest
            AdminWaitSetRequest waitSetReq = new AdminWaitSetRequest(wsID, Integer.toString(lastSequence.get(wsID)));
            try {
                synchronized(soapProv) {
                    pendingRequest = soapProv.invokeJaxbAsync(waitSetReq, server, cb);
                }
            } catch (ServiceException e) {
                ZimbraLog.imap.error("Failed to send WaitSetRequest. ", e);
            }
        } else {
            ZimbraLog.imap.error("Cannot continue to poll waitset, because waitset ID is not known");
        }
    }
    @VisibleForTesting
    public String getWSId() {
        return wsID;
    }

    private void cancelPendingRequest() {
        if(pendingRequest != null && !(pendingRequest.isCancelled() || pendingRequest.isDone())) {
            pendingRequest.cancel(true);
        }
    }

    private final FutureCallback<HttpResponse> cb = new FutureCallback<HttpResponse>() {
        public void completed(final HttpResponse response) {
            int respCode = response.getStatusLine().getStatusCode();
            if(respCode == 200) {
                Element envelope;
                try {
                    envelope = W3cDomUtil.parseXML(response.getEntity().getContent());
                    SoapProtocol proto = SoapProtocol.determineProtocol(envelope);
                    Element doc = proto.getBodyElement(envelope);
                    AdminWaitSetResponse wsResp = (AdminWaitSetResponse) JaxbUtil.elementToJaxb(doc);
                    lastSequence.put(wsResp.getWaitSetId(), Integer.parseInt(wsResp.getSeqNo()));
                    if(wsResp.getSignalledAccounts().size() > 0) {
                        Iterator<Id> iter = wsResp.getSignalledAccounts().iterator();
                        while(iter.hasNext()) {
                            Id accId = iter.next();
                            if(accountToSessionMap.containsKey(accId.getId())) {
                                accountToSessionMap.get(accId.getId()).signalAccountChange();
                            }
                        }
                    }
                } catch (UnsupportedOperationException | IOException | ServiceException e) {
                    ZimbraLog.imap.error("Exception handling WaitSetResponse. ", e);
                }
            } else {
                ZimbraLog.imap.error("WaitSetRequest failed with response code %d ", respCode);
            }
            continueWaitSet();
        }

        public void failed(final Exception ex) {
            ZimbraLog.imap.error("WaitSetRequest failed ", ex);
        }

        public void cancelled() {
            ZimbraLog.imap.info("WaitSetRequest was cancelled");
        }
    };
}
