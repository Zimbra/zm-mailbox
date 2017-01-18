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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

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
import com.zimbra.cs.session.PendingRemoteModifications;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.admin.message.AdminCreateWaitSetRequest;
import com.zimbra.soap.admin.message.AdminCreateWaitSetResponse;
import com.zimbra.soap.admin.message.AdminDestroyWaitSetRequest;
import com.zimbra.soap.admin.message.AdminWaitSetRequest;
import com.zimbra.soap.admin.message.AdminWaitSetResponse;
import com.zimbra.soap.mail.type.PendingFolderModifications;
import com.zimbra.soap.type.AccountWithModifications;
import com.zimbra.soap.type.Id;
import com.zimbra.soap.type.WaitSetAddSpec;

public class ImapServerListener {
    private final String server;
    private volatile String wsID = null;
    private final ConcurrentHashMap<String /* account ID */, ConcurrentHashMap<Integer /* folder ID */, List<ImapRemoteSession>>> sessionMap = new ConcurrentHashMap<String, ConcurrentHashMap<Integer, List<ImapRemoteSession>>>();
    private final AtomicInteger lastSequence = new AtomicInteger(0);
    private final SoapProvisioning soapProv = new SoapProvisioning();
    private Future<HttpResponse> pendingRequest;

    ImapServerListener(String svr) throws ServiceException {
        this.server = svr;
        soapProv.soapSetURI(URLUtil.getAdminURL(Provisioning.getInstance().getServerByName(server)));
    }

    private void checkAuth() throws ServiceException {
        synchronized(soapProv) {
            if(soapProv.isExpired()) {
                soapProv.soapZimbraAdminAuthenticate();
            }
        }
    }

    public void shutdown() throws ServiceException {
        try {
            deleteWaitSet();
            sessionMap.clear();
            lastSequence.set(0);
        } finally {
            synchronized(soapProv) {
                soapProv.soapLogOut();
            }
        }
    }

    public void addListener(ImapRemoteSession listener) throws ServiceException {
        String accountId = listener.getTargetAccountId();
        boolean alreadyListening = sessionMap.containsKey(accountId);
        sessionMap.putIfAbsent(accountId, new ConcurrentHashMap<Integer, List<ImapRemoteSession>>());
        Integer folderId = listener.getFolderId();
        ConcurrentHashMap<Integer, List<ImapRemoteSession>> foldersToSessions = sessionMap.get(accountId);
        foldersToSessions.putIfAbsent(folderId, Collections.synchronizedList(new ArrayList<ImapRemoteSession>()));
        List<ImapRemoteSession> sessions = foldersToSessions.get(folderId);
        if(!sessions.contains(listener)) {
            sessions.add(listener);
        }
        initWaitSet(listener.getTargetAccountId(), alreadyListening);
    }

    public void removeListener(ImapRemoteSession listener) throws ServiceException {
        String accountId = listener.getTargetAccountId();
        ConcurrentHashMap<Integer, List<ImapRemoteSession>> foldersToSessions = sessionMap.get(accountId);
        if(foldersToSessions != null) {
            Integer folderId = listener.getFolderId();
            List<ImapRemoteSession> sessions = foldersToSessions.get(folderId);
            if(sessions != null) {
                sessions.remove(listener);
                //cleanup to save memory at cost of reducing speed of adding/removing sessions
                if(sessions.isEmpty()) {
                    //if no more sessions are registered for this folder remove the empty List from the map
                    foldersToSessions.remove(folderId);
                    if(foldersToSessions.isEmpty()) {
                        //if no more sessions registered for this account remove the empty map
                        sessionMap.remove(accountId);
                        if(sessionMap.isEmpty()) {
                            deleteWaitSet();
                        }
                    }
                }
                if(wsID != null) {
                    initWaitSet(listener.getTargetAccountId(), true);
                }
            }
        }
    }

    public boolean isListeningOn(String accountId, Integer folderId) {
        ConcurrentHashMap<Integer, List<ImapRemoteSession>> folderSessions = sessionMap.get(accountId);
        if(folderSessions != null) {
            List<ImapRemoteSession> sessions = folderSessions.get(folderId);
            return sessions != null && !sessions.isEmpty();
        } else {
            return false;
        }
    }

    public List<ImapRemoteSession> getListeners(String accountId, int folderId) throws ServiceException {
        ConcurrentHashMap<Integer, List<ImapRemoteSession>> folderSessions = sessionMap.get(accountId);
        if(folderSessions != null) {
            List<ImapRemoteSession> sessions = folderSessions.get(folderId);
            if(sessions != null) {
                return sessions;
            }
        }
        return Collections.emptyList();
    }

    private void initWaitSet(String accountId, boolean alreadyListening) throws ServiceException {
        if(wsID == null && this.sessionMap.containsKey(accountId)) {
            AdminCreateWaitSetRequest req = new AdminCreateWaitSetRequest("all", false);
            checkAuth();
            AdminCreateWaitSetResponse resp;
            resp = soapProv.invokeJaxbAsAdminWithRetry(req, server);
            if(resp == null) {
                throw ServiceException.FAILURE("Received null response from AdminCreateWaitSetRequest", null);
            }
            wsID = resp.getWaitSetId();
            lastSequence.set(resp.getSequence());
        } else {
            cancelPendingRequest();
        }
        ZimbraLog.imap.debug("Current waitset ID is %s", wsID);
        //send non-blocking synchronous WaitSetRequest. This way the caller has certainty that listener was added on remote server
        AdminWaitSetRequest waitSetReq = new AdminWaitSetRequest(wsID, lastSequence.toString());
        waitSetReq.setBlock(false);
        waitSetReq.setExpand(true);
        if(!this.sessionMap.containsKey(accountId) || this.sessionMap.get(accountId).isEmpty()) {
            ZimbraLog.imap.debug("Removing accout %s from waitset %s", accountId, wsID);
            waitSetReq.addRemoveAccount(new Id(accountId));
        } else {
            WaitSetAddSpec updateOrAdd = new WaitSetAddSpec();
            updateOrAdd.setId(accountId);
            Enumeration<Integer> folderIDs = this.sessionMap.get(accountId).keys();
            while(folderIDs.hasMoreElements()) {
                updateOrAdd.addFolderInterest(folderIDs.nextElement());
            }
            if(alreadyListening) {
                ZimbraLog.imap.debug("Updating folder interests for account %s in waitset %s", accountId, wsID);
                waitSetReq.addUpdateAccount(updateOrAdd);
            } else {
                ZimbraLog.imap.debug("Adding account %s to waitset %s", accountId, wsID);
                waitSetReq.addAddAccount(updateOrAdd);
            }
        }
        ZimbraLog.imap.debug("Sending initial AdminWaitSetRequest. WaitSet ID: %s", wsID);
        AdminWaitSetResponse wsResp = soapProv.invokeJaxbAsAdminWithRetry(waitSetReq, server);
        try {
            processAdminWaitSetResponse(wsResp);
            continueWaitSet();
        } catch (Exception e) {
            throw ServiceException.FAILURE("Failed to process initial AdminWaitSetResponse", e);
        }
    }

    private void deleteWaitSet() throws ServiceException {
        ZimbraLog.imap.debug("Deleting waitset %s", wsID);
        cancelPendingRequest();
        if(wsID != null) {
            AdminDestroyWaitSetRequest req = new AdminDestroyWaitSetRequest(wsID);
            checkAuth();
            try {
                synchronized(soapProv) {
                    soapProv.invokeJaxbAsAdminWithRetry(req);
                }
            } finally {
                wsID = null;
                lastSequence.set(0);
            }
        }
    }

    private void continueWaitSet() {
        ZimbraLog.imap.debug("Continuing waitset %s", wsID);
        cancelPendingRequest();
        if(wsID != null) {
            //send asynchronous AdminWaitSetRequest
            AdminWaitSetRequest waitSetReq = new AdminWaitSetRequest(wsID, lastSequence.toString());
            waitSetReq.setBlock(true);
            waitSetReq.setExpand(true);
            try {
                checkAuth();
                ZimbraLog.imap.debug("Sending followup asynchronous AdminWaitSetRequest. WaitSet ID: %s", wsID);
                synchronized(soapProv) {
                    pendingRequest = soapProv.invokeJaxbAsync(waitSetReq, server, cb);
                }
            } catch (ServiceException ex) {
                ZimbraLog.imap.error("Failed to send WaitSetRequest. ", ex);
            }
        } else {
            ZimbraLog.imap.error("Cannot continue to poll waitset, because waitset ID is not known");
        }
    }

    @VisibleForTesting
    public String getWSId() {
        return wsID;
    }

    private synchronized void cancelPendingRequest() {
        ZimbraLog.imap.debug("Canceling pending AdminWaitSetRequest for waitset %s. Sequence %s", wsID, lastSequence.toString());
        if(pendingRequest != null && !(pendingRequest.isCancelled() || pendingRequest.isDone())) {
            pendingRequest.cancel(true);
        }
    }

    private synchronized void processAdminWaitSetResponse(AdminWaitSetResponse wsResp) throws Exception {
        String respWSId = wsResp.getWaitSetId();
        if(wsID == null || !wsID.equalsIgnoreCase(respWSId)) {
            //rogue response, which we are not interested in
            ZimbraLog.imap.error("Received AdminWaitSetResponse for another waitset ", respWSId);
            return;
        }
        if(wsResp.getCanceled() != null && wsResp.getCanceled().booleanValue() && wsID.equalsIgnoreCase(respWSId)) {
            //this waitset was canceled
            //TODO: figure out what to do with listeners if they are still registered
            wsID = null;
            lastSequence.set(0);
            ZimbraLog.imap.debug("AdminWaitSet %s was canceled", respWSId);
        } else {
            String seqNum = wsResp.getSeqNo();
            int modSeq = 0;
            if(seqNum != null) {
                modSeq = Integer.parseInt(wsResp.getSeqNo());
            }
            ZimbraLog.imap.debug("Received AdminWaitSetResponse for waitset %s. Updating sequence to %s ", respWSId, wsResp.getSeqNo());
            lastSequence.set(modSeq);
            if(wsResp.getSignalledAccounts().size() > 0) {
                Iterator<AccountWithModifications> iter = wsResp.getSignalledAccounts().iterator();
                while(iter.hasNext()) {
                    AccountWithModifications accInfo = iter.next();
                    ConcurrentHashMap<Integer, List<ImapRemoteSession>> foldersToSessions = sessionMap.get(accInfo.getId());
                    if(foldersToSessions != null && !foldersToSessions.isEmpty()) {
                        Collection<PendingFolderModifications> mods = accInfo.getPendingFolderModifications();
                        if(mods != null && !mods.isEmpty()) {
                            for(PendingFolderModifications folderMods : mods) {
                                Integer folderId = folderMods.getFolderId();
                                PendingRemoteModifications remoteMods = PendingRemoteModifications.fromSOAP(folderMods, folderId, accInfo.getId());
                                List<ImapRemoteSession> listeners = foldersToSessions.get(folderId);
                                if(listeners != null) {
                                    for(ImapRemoteSession l : listeners) {
                                        l.notifyPendingChanges(remoteMods, modSeq, null);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    private final FutureCallback<HttpResponse> cb = new FutureCallback<HttpResponse>() {
        @Override
        public void completed(final HttpResponse response) {
            int respCode = response.getStatusLine().getStatusCode();
            if(respCode == 200) {
                Element envelope;
                try {
                    envelope = W3cDomUtil.parseXML(response.getEntity().getContent());
                    SoapProtocol proto = SoapProtocol.determineProtocol(envelope);
                    Element doc = proto.getBodyElement(envelope);
                    AdminWaitSetResponse wsResp = (AdminWaitSetResponse) JaxbUtil.elementToJaxb(doc);
                    processAdminWaitSetResponse(wsResp);
                } catch (Exception e) {
                    ZimbraLog.imap.error("Exception thrown while handling WaitSetResponse. ", e);
                }
            } else {
                ZimbraLog.imap.error("WaitSetRequest failed with response code %d ", respCode);
            }
            continueWaitSet();
        }

        @Override
        public void failed(final Exception ex) {
            ZimbraLog.imap.error("WaitSetRequest failed ", ex);
        }

        @Override
        public void cancelled() {
            ZimbraLog.imap.info("WaitSetRequest was cancelled");
        }
    };
}
