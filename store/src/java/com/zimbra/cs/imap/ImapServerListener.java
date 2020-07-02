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

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.concurrent.FutureCallback;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.mailbox.ItemIdentifier;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.soap.W3cDomUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.service.admin.AdminServiceException;
import com.zimbra.cs.session.PendingRemoteModifications;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.cs.session.WaitSetError;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.admin.message.AdminCreateWaitSetRequest;
import com.zimbra.soap.admin.message.AdminCreateWaitSetResponse;
import com.zimbra.soap.admin.message.AdminDestroyWaitSetRequest;
import com.zimbra.soap.admin.message.AdminWaitSetRequest;
import com.zimbra.soap.admin.message.AdminWaitSetResponse;
import com.zimbra.soap.mail.type.PendingFolderModifications;
import com.zimbra.soap.type.AccountWithModifications;
import com.zimbra.soap.type.Id;
import com.zimbra.soap.type.IdAndType;
import com.zimbra.soap.type.WaitSetAddSpec;

public class ImapServerListener {
    private final String server;
    private volatile String wsID = null;
    private final ConcurrentHashMap<String /* account ID */,
                                    ConcurrentHashMap<Integer /* folder ID */, Set<ImapRemoteSession>>>
            sessionMap = new ConcurrentHashMap<>();
    /* Used to track when WaitSet system has caught up to a known last change ID we know about. */
    private final ConcurrentHashMap<String /* account ID */,
                                ConcurrentHashMap<Integer /* last known change ID */, Set<CountDownLatch>>>
            catchupToKnownLastChangeId = new ConcurrentHashMap<>();
    private final AtomicInteger lastSequence = new AtomicInteger(0);
    private final SoapProvisioning soapProv = new SoapProvisioning();
    private Future<HttpResponse> pendingRequest;
    private final Integer pendingRequestGuard = 1;

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
            catchupToKnownLastChangeId.clear();
            lastSequence.set(0);
        } finally {
            synchronized(soapProv) {
                soapProv.soapLogOut();
            }
        }
    }

    public void addListener(ImapRemoteSession listener) throws ServiceException {
        ItemIdentifier folderIdent = listener.getFolderItemIdentifier();
        String accountId = (folderIdent.accountId != null) ? folderIdent.accountId : listener.getTargetAccountId();
        boolean alreadyListening = sessionMap.containsKey(accountId);
        sessionMap.putIfAbsent(accountId, new ConcurrentHashMap<Integer, Set<ImapRemoteSession>>());
        Integer folderId = folderIdent.id;
        ConcurrentHashMap<Integer, Set<ImapRemoteSession>> foldersToSessions = sessionMap.get(accountId);
        foldersToSessions.putIfAbsent(folderId, Collections.newSetFromMap(new ConcurrentHashMap<ImapRemoteSession, Boolean>()));
        Set<ImapRemoteSession> sessions = foldersToSessions.get(folderId);
        if(!sessions.contains(listener)) {
            ZimbraLog.imap.debug("addListener acct=%s folderId=%s %s", listener.getTargetAccountId(), folderIdent, listener);
            sessions.add(listener);
        }
        if (wsID != null) {
            ZMailbox zmbox = (ZMailbox) listener.getMailbox();
            zmbox.setCurWaitSetID(wsID);
        }
        initWaitSet(accountId, alreadyListening);
    }

    public void removeListener(ImapRemoteSession listener) throws ServiceException {
        ItemIdentifier folderIdent = listener.getFolderItemIdentifier();
        String accountId = (folderIdent.accountId != null) ? folderIdent.accountId : listener.getTargetAccountId();
        ConcurrentHashMap<Integer, Set<ImapRemoteSession>> foldersToSessions = sessionMap.get(accountId);
        if(foldersToSessions != null) {
            Integer folderId = folderIdent.id;
            Set<ImapRemoteSession> sessions = foldersToSessions.get(folderId);
            if(sessions != null) {
                ZimbraLog.imap.debug("removeListener acct=%s folderId=%s %s",
                        listener.getTargetAccountId(), folderIdent, listener);
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
                if (wsID != null) {
                    initWaitSet(accountId, true);
                }
            }
        }
    }

    public boolean isListeningOn(String accountId, Integer folderId) {
        ConcurrentHashMap<Integer, Set<ImapRemoteSession>> folderSessions = sessionMap.get(accountId);
        if(folderSessions != null) {
            Set<ImapRemoteSession> sessions = folderSessions.get(folderId);
            return sessions != null && !sessions.isEmpty();
        } else {
            return false;
        }
    }

    public Set<ImapRemoteSession> getListeners(String accountId, int folderId) throws ServiceException {
        ConcurrentHashMap<Integer, Set<ImapRemoteSession>> folderSessions = sessionMap.get(accountId);
        if(folderSessions != null) {
            Set<ImapRemoteSession> sessions = folderSessions.get(folderId);
            if(sessions != null) {
                return sessions;
            }
        }
        return Collections.emptySet();
    }

    public int getLastKnownSequenceNumber() {
        return lastSequence.intValue();
    }

    protected void addNotifyWhenCaughtUp(String accountId, int lastKnownChangeId, CountDownLatch cdl) {
        catchupToKnownLastChangeId.putIfAbsent(accountId, new ConcurrentHashMap<>());
        ConcurrentHashMap<Integer, Set<CountDownLatch>> acctWaitList = catchupToKnownLastChangeId.get(accountId);
        acctWaitList.putIfAbsent(Integer.valueOf(lastKnownChangeId), new HashSet<>());
        Set<CountDownLatch> latches = acctWaitList.get(lastKnownChangeId);
        ZimbraLog.imap.debug("ImapServerListener.addNotifyWhenCaughtUp(%s,%s,%s)", accountId,
                lastKnownChangeId, cdl);
        latches.add(cdl);
    }

    protected void removeNotifyWhenCaughtUp(String acctId, int changeId) {
        ConcurrentHashMap<Integer, Set<CountDownLatch>> changeId2Latches =
                catchupToKnownLastChangeId.get(acctId);
        if (changeId2Latches == null) {
            return;
        }
        for (Entry<Integer, Set<CountDownLatch>> entry : changeId2Latches.entrySet()) {
            Integer expectedLastKnownChangeId = entry.getKey();
            if (expectedLastKnownChangeId > changeId) {
                ZimbraLog.imap.debug(
                        "ImapServerListener.removeNotifyWhenCaughtUp not caught up acct=%s expect=%s got=%s",
                        acctId, expectedLastKnownChangeId, changeId);
            } else {
                for (CountDownLatch latch : entry.getValue()) {
                    ZimbraLog.imap.debug(
                        "ImapServerListener.removeNotifyWhenCaughtUp acct=%s expect=%s got=%s latch=%s",
                        acctId, expectedLastKnownChangeId, changeId , latch);
                    latch.countDown();
                }
                changeId2Latches.remove(expectedLastKnownChangeId);
            }
        }
    }

    private void cleanupCatchupToKnownLastChangeId() {
        for (String acctId : catchupToKnownLastChangeId.keySet()) {
            if (!sessionMap.containsKey(acctId)) {
                catchupToKnownLastChangeId.remove(acctId);
            }
        }
    }

    private synchronized void restoreWaitSet() throws ServiceException {
        ZimbraLog.imap.debug("Attempting to restore admin waitset for all registered listeners.");
        if(wsID != null) {
            ZimbraLog.imap.debug("Another thread has already restored waitset.");
            return; //another thread has already restored waitset
        }
        cancelPendingRequest();
        AdminCreateWaitSetRequest req = new AdminCreateWaitSetRequest("all", false);
        checkAuth();
        AdminCreateWaitSetResponse resp;
        resp = soapProv.invokeJaxbAsAdminWithRetry(req, server);
        if(resp == null) {
            throw ServiceException.FAILURE("Received null response from AdminCreateWaitSetRequest", null);
        }
        wsID = resp.getWaitSetId();
        setWaitSetIdOnMailboxes();
        lastSequence.set(resp.getSequence());
        ZimbraLog.imap.debug("Created new waitset to replace lost or cancelled one. WaitSet ID: %s", wsID);
        //send non-blocking synchronous WaitSetRequest. This way the caller has certainty that listeners were added on remote server
        AdminWaitSetRequest waitSetReq = new AdminWaitSetRequest(wsID, lastSequence.toString());
        waitSetReq.setBlock(false);
        waitSetReq.setExpand(true);
        Enumeration<String> accountIds = this.sessionMap.keys();
        while(accountIds.hasMoreElements()) {
            String accountId = accountIds.nextElement();
            WaitSetAddSpec updateOrAdd = new WaitSetAddSpec();
            updateOrAdd.setId(accountId);
            Enumeration<Integer> folderIDs = this.sessionMap.get(accountId).keys();
            while(folderIDs.hasMoreElements()) {
                updateOrAdd.addFolderInterest(folderIDs.nextElement());
                ZimbraLog.imap.debug("Adding account %s to waitset %s", accountId, wsID);
                waitSetReq.addAddAccount(updateOrAdd);
            }
        }
        ZimbraLog.imap.debug("Sending initial AdminWaitSetRequest. WaitSet ID: %s", wsID);
        AdminWaitSetResponse wsResp = soapProv.invokeJaxbAsAdminWithRetry(waitSetReq, server);
        try {
            processAdminWaitSetResponse(wsResp);
        } catch (Exception e) {
            throw ServiceException.FAILURE("Failed to process initial AdminWaitSetResponse", e);
        }
    }

    private void setWaitSetIdOnMailboxes() {
        for (Map<Integer, Set<ImapRemoteSession>> foldersToSessions: sessionMap.values()) {
            for (Set<ImapRemoteSession> sessions: foldersToSessions.values()) {
                for (ImapRemoteSession session: sessions) {
                    ZMailbox zmbox = (ZMailbox) session.getMailbox();
                    zmbox.setCurWaitSetID(wsID);
                }
            }
        }
    }

    private void unsetWaitSetIdOnMailboxes() {
        for (Map<Integer, Set<ImapRemoteSession>> foldersToSessions: sessionMap.values()) {
            for (Set<ImapRemoteSession> sessions: foldersToSessions.values()) {
                for (ImapRemoteSession session: sessions) {
                    ZMailbox zmbox = (ZMailbox) session.getMailbox();
                    zmbox.unsetCurWaitSetID();
                }
            }
        }
    }

    private synchronized void initWaitSet(String accountId, boolean alreadyListening) throws ServiceException {
        if(wsID == null && this.sessionMap.containsKey(accountId)) {
            AdminCreateWaitSetRequest req = new AdminCreateWaitSetRequest("all", false);
            checkAuth();
            AdminCreateWaitSetResponse resp;
            resp = soapProv.invokeJaxbAsAdminWithRetry(req, server);
            if(resp == null) {
                throw ServiceException.FAILURE("Received null response from AdminCreateWaitSetRequest", null);
            }
            wsID = resp.getWaitSetId();
            setWaitSetIdOnMailboxes();
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

        try {
            ZimbraLog.imap.debug("Sending initial AdminWaitSetRequest. WaitSet ID: %s", wsID);
            AdminWaitSetResponse wsResp = soapProv.invokeJaxbAsAdminWithRetry(waitSetReq, server);
            processAdminWaitSetResponse(wsResp);
        } catch (SoapFaultException e) {
            if(AdminServiceException.NO_SUCH_WAITSET.equalsIgnoreCase(e.getCode())) {
                //waitset is gone. Create a new one
                ZimbraLog.imap.warn("AdminWaitSet %s does not exist anymore", wsID);
                wsID = null;
                unsetWaitSetIdOnMailboxes();
                lastSequence.set(0);
                restoreWaitSet();
            } else {
                throw ServiceException.FAILURE("Failed to process initial AdminWaitSetResponse", e);
            }
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
                    try {
                        soapProv.invokeJaxbAsAdminWithRetry(req);
                    } catch (SoapFaultException ex) {
                        if(AdminServiceException.NO_SUCH_WAITSET.equalsIgnoreCase(ex.getCode())) {
                            ZimbraLog.imap.debug("Caught NO_SUCH_WAITSET exception trying to delete a waitset. Waitset may have been deleted by sweeper. Ignoring.");
                        }
                    } catch (ServiceException e) {
                        ZimbraLog.imap.error("Caught unexpected exception trying to delete a waitset.", e);
                    }
                }
            } finally {
                wsID = null;
                unsetWaitSetIdOnMailboxes();
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

    private void cancelPendingRequest() {
        if (pendingRequest != null && !(pendingRequest.isCancelled() || pendingRequest.isDone())) {
            ZimbraLog.imap.debug("Canceling pending AdminWaitSetRequest for waitset %s. Sequence %s", wsID, lastSequence.toString());
            synchronized (pendingRequestGuard) {
                pendingRequest.cancel(true);
                pendingRequest = null;
            }
        }
    }

    private synchronized void dropAllListeners() {
        ZimbraLog.imap.error("Terminating all IMAP sessions.");
        sessionMap.forEach((accountId, foldersToSessions) -> {
            sessionMap.remove(accountId);
            if(foldersToSessions != null && !foldersToSessions.isEmpty()) {
                foldersToSessions.forEach((folderId, listeners) -> {
                    if(listeners != null) {
                        for(ImapRemoteSession l : listeners) {
                            SessionCache.clearSession(l);
                        }
                    }
                });
            }
        });
    }

    public void notifyAccountChange(AccountWithModifications accInfo) {
        ConcurrentHashMap<Integer, Set<ImapRemoteSession>> foldersToSessions = sessionMap.get(accInfo.getId());
        if(foldersToSessions != null && !foldersToSessions.isEmpty()) {
            Collection<PendingFolderModifications> mods = accInfo.getPendingFolderModifications();
            if(mods != null && !mods.isEmpty()) {
                for(PendingFolderModifications folderMods : mods) {
                    Integer folderId = folderMods.getFolderId();
                    PendingRemoteModifications remoteMods = PendingRemoteModifications.fromSOAP(folderMods, folderId, accInfo.getId());
                    Set<ImapRemoteSession> listeners = foldersToSessions.get(folderId);
                    if(listeners != null) {
                        for(ImapRemoteSession l : listeners) {
                            l.notifyPendingChanges(remoteMods, accInfo.getLastChangeId(), null);
                        }
                    }
                }
            }
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
            ZimbraLog.imap.warn("AdminWaitSet %s was cancelled", respWSId);
            deleteWaitSet();
            restoreWaitSet();
            return;
        }
        String seqNum = wsResp.getSeqNo();
        int modSeq = 0;
        if(seqNum != null) {
            modSeq = Integer.parseInt(wsResp.getSeqNo());
        }
        ZimbraLog.imap.debug("Received AdminWaitSetResponse for waitset %s. Updating sequence to %s",
                respWSId, wsResp.getSeqNo());
        lastSequence.set(modSeq);
        List<AccountWithModifications> signalledAccounts = wsResp.getSignalledAccounts();
        if(signalledAccounts != null && signalledAccounts.size() > 0) {
            Iterator<AccountWithModifications> iter = signalledAccounts.iterator();
            while(iter.hasNext()) {
                AccountWithModifications accInfo = iter.next();
                notifyAccountChange(accInfo);
                removeNotifyWhenCaughtUp(accInfo.getId(), accInfo.getLastChangeId());
            }
        }
        cleanupCatchupToKnownLastChangeId();

        //check for errors
        List<IdAndType> errors = wsResp.getErrors();
        if(errors != null) {
            Iterator<IdAndType> iter = errors.iterator();
            while(iter.hasNext()) {
                IdAndType error = iter.next();
                String errorType = error.getType();
                String accId = error.getId();
                if(errorType != null) {
                    ZimbraLog.imap.error("AdminWaitSetResponse returned an error for account %s. Error: %s", accId, errorType);
                    WaitSetError.Type err = WaitSetError.Type.valueOf(errorType);
                    if(err == WaitSetError.Type.NO_SUCH_ACCOUNT ||
                            err == WaitSetError.Type.MAILBOX_DELETED ||
                            err == WaitSetError.Type.MAINTENANCE_MODE ||
                            err == WaitSetError.Type.WRONG_HOST_FOR_ACCOUNT ||
                            err == WaitSetError.Type.ERROR_LOADING_MAILBOX) {
                        ConcurrentHashMap<Integer, Set<ImapRemoteSession>> foldersToSessions = sessionMap.get(accId);
                        sessionMap.remove(accId);
                        if(foldersToSessions != null && !foldersToSessions.isEmpty()) {
                            foldersToSessions.forEach((folderId, listeners) -> {
                                if(listeners != null) {
                                    for(ImapRemoteSession l : listeners) {
                                        SessionCache.clearSession(l);
                                    }
                                }
                            });
                        }
                    }
                }
            }
        }
        if(sessionMap.isEmpty()) {
            deleteWaitSet();
        } else {
            continueWaitSet();
        }
    }

    private final FutureCallback<HttpResponse> cb = new FutureCallback<HttpResponse>() {
        @Override
        public void completed(final HttpResponse response) {
            int respCode = response.getStatusLine().getStatusCode();
            if(respCode == HttpStatus.SC_OK) {
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
            } else if (respCode == HttpStatus.SC_INTERNAL_SERVER_ERROR){
                Element envelope;
                try {
                    envelope = W3cDomUtil.parseXML(response.getEntity().getContent());
                    SoapProtocol proto = SoapProtocol.determineProtocol(envelope);
                    if(proto.hasFault(envelope)) {
                        Element doc = proto.getBodyElement(envelope);
                        if(proto.isFault(doc)) {
                            SoapFaultException ex = proto.soapFault(doc);
                            if(AdminServiceException.NO_SUCH_WAITSET.equalsIgnoreCase(ex.getCode())) {
                                //waitset is gone. Create a new one
                                ZimbraLog.imap.warn("AdminWaitSet %s does not exist anymore", wsID);
                                wsID = null;
                                unsetWaitSetIdOnMailboxes();
                                lastSequence.set(0);
                                restoreWaitSet();
                            }
                        }
                    } else {
                        ZimbraLog.imap.error("Mailbox server returned error 500 w/o a SOAP exception. %s", envelope);
                        dropAllListeners();
                    }
                } catch (Exception e) {
                    ZimbraLog.imap.error("Exception thrown while handling WaitSetResponse.", e);
                    dropAllListeners();
                }
            } else {
                ZimbraLog.imap.error("WaitSetRequest failed with response code %d ", respCode);
                dropAllListeners();
            }
        }

        @Override
        public void failed(final Exception ex) {
            ZimbraLog.imap.error("WaitSetRequest failed.", ex);
            dropAllListeners();
        }

        @Override
        public void cancelled() {
            ZimbraLog.imap.info("WaitSetRequest was cancelled");
        }
    };
}
