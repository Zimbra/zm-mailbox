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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
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
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.session.PendingRemoteModifications;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.admin.message.AdminCreateWaitSetRequest;
import com.zimbra.soap.admin.message.AdminCreateWaitSetResponse;
import com.zimbra.soap.admin.message.AdminDestroyWaitSetRequest;
import com.zimbra.soap.admin.message.AdminWaitSetRequest;
import com.zimbra.soap.admin.message.AdminWaitSetResponse;
import com.zimbra.soap.mail.type.PendingFolderModifications;
import com.zimbra.soap.type.AccountWithModifications;
import com.zimbra.soap.type.WaitSetAddSpec;

public class ImapServerListener {
    private final String server;
    private volatile String wsID = null;
    private final ConcurrentHashMap<String, ConcurrentHashMap<Integer, List<ImapRemoteSession>>> sessionMap = new ConcurrentHashMap<String, ConcurrentHashMap<Integer, List<ImapRemoteSession>>>();
    private final ConcurrentMap<String, Integer> lastSequence = new ConcurrentHashMap<String, Integer>();
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
            lastSequence.clear();
        } finally {
            synchronized(soapProv) {
                soapProv.soapLogOut();
            }
        }
    }

    public void addListener(ImapRemoteSession listener) throws ServiceException {
        String accountId = listener.getTargetAccountId();
        sessionMap.putIfAbsent(accountId, new ConcurrentHashMap<Integer, List<ImapRemoteSession>>());
        Integer folderId = listener.getFolderId();
        ConcurrentHashMap<Integer, List<ImapRemoteSession>> foldersToSessions = sessionMap.get(accountId);
        foldersToSessions.putIfAbsent(folderId, Collections.synchronizedList(new ArrayList<ImapRemoteSession>()));
        List<ImapRemoteSession> sessions = foldersToSessions.get(folderId);
        if(!sessions.contains(listener)) {
            sessions.add(listener);
        }
        initWaitSet(listener.getTargetAccountId(), foldersToSessions.keySet());
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
                    } else {
                        //no more sessions registered for this folder, but other sessions are registered for other folders of this account
                        if(wsID != null) {
                            initWaitSet(listener.getTargetAccountId(), foldersToSessions.keySet());
                        }
                    }
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

    private void initWaitSet(String accountId, KeySetView<Integer, List<ImapRemoteSession>> keySetView) throws ServiceException {
        synchronized(soapProv) {
            if(wsID == null) {
                AdminCreateWaitSetRequest req = new AdminCreateWaitSetRequest("all", false);
                WaitSetAddSpec add = new WaitSetAddSpec();
                add.setId(accountId);
                add.setFolderInterests(keySetView);
                req.addAccount(add);
                checkAuth();
                AdminCreateWaitSetResponse resp;
                resp = soapProv.invokeJaxbAsAdminWithRetry(req, server);
                if(resp == null) {
                    throw ServiceException.FAILURE("Received null response from AdminCreateWaitSetRequest", null);
                }
                wsID = resp.getWaitSetId();
                lastSequence.put(wsID, resp.getSequence());
            }
            ZimbraLog.imap.debug("Current waitset ID is %s", wsID);
            //send asynchronous WaitSetRequest
            AdminWaitSetRequest waitSetReq = new AdminWaitSetRequest(wsID, Integer.toString(lastSequence.get(wsID)));
            WaitSetAddSpec update = new WaitSetAddSpec();
            update.setId(accountId);
            update.setFolderInterests(keySetView);
            waitSetReq.addUpdateAccount(update);
            cancelPendingRequest();
            pendingRequest = soapProv.invokeJaxbAsync(waitSetReq, server, cb);
        }
    }

    private void deleteWaitSet() throws ServiceException {
        cancelPendingRequest();
        if(wsID != null) {
            AdminDestroyWaitSetRequest req = new AdminDestroyWaitSetRequest(wsID);
            checkAuth();
            try {
                synchronized(soapProv) {
                    soapProv.invokeJaxbAsAdminWithRetry(req);
                }
            } finally {
                if(lastSequence.containsKey(wsID)) {
                    lastSequence.remove(wsID);
                }
                wsID = null;
            }
        }
    }

    private void continueWaitSet() {
        cancelPendingRequest();
        if(wsID != null) {
            //send asynchronous WaitSetRequest
            AdminWaitSetRequest waitSetReq = new AdminWaitSetRequest(wsID, Integer.toString(lastSequence.get(wsID)));
            try {
                checkAuth();
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

    private void cancelPendingRequest() {
        if(pendingRequest != null && !(pendingRequest.isCancelled() || pendingRequest.isDone())) {
            pendingRequest.cancel(true);
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
                    int modSeq = Integer.parseInt(wsResp.getSeqNo());
                    lastSequence.put(wsResp.getWaitSetId(), modSeq);
                    if(wsResp.getSignalledAccounts().size() > 0) {
                        Iterator<AccountWithModifications> iter = wsResp.getSignalledAccounts().iterator();
                        while(iter.hasNext()) {
                            AccountWithModifications accInfo = iter.next();
                            ConcurrentHashMap<Integer, List<ImapRemoteSession>> foldersToSessions = sessionMap.get(accInfo.getId());
                            if(foldersToSessions != null && !foldersToSessions.isEmpty()) {
                                Collection<PendingFolderModifications> mods = accInfo.getPendingFolderModifications();
                                if(mods != null && !mods.isEmpty()) {
                                    for(PendingFolderModifications folderMods : mods) {
                                        PendingRemoteModifications remoteMods = new PendingRemoteModifications();
                                        //TODO: fill in the contents of PendingRemoteModifications from PendingAccountModifications
                                        Integer folderId = folderMods.getFolderId();
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
                } catch (UnsupportedOperationException | IOException | ServiceException e) {
                    ZimbraLog.imap.error("Exception handling WaitSetResponse. ", e);
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
