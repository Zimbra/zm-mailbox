/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import com.zimbra.cs.mailbox.DistributedWaitSet;
import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.service.admin.AdminServiceException;
import com.zimbra.cs.service.util.SyncToken;
import com.zimbra.cs.servlet.continuation.ResumeContinuationListener;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.WaitSetAccount;
import com.zimbra.cs.session.WaitSetCallback;
import com.zimbra.cs.session.WaitSetError;
import com.zimbra.cs.session.WaitSetMgr;
import com.zimbra.cs.session.WaitSetSession;
import com.zimbra.soap.SoapServlet;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.base.WaitSetReq;
import com.zimbra.soap.base.WaitSetResp;
import com.zimbra.soap.mail.message.WaitSetResponse;
import com.zimbra.soap.mail.type.PendingFolderModifications;
import com.zimbra.soap.type.AccountWithModifications;
import com.zimbra.soap.type.Id;
import com.zimbra.soap.type.IdAndType;
import com.zimbra.soap.type.WaitSetAddSpec;

import static com.google.common.collect.Iterables.isEmpty;

/**
 *
 */
public class WaitSetRequest extends MailDocumentHandler {

    private static final String VARS_ATTR_NAME = WaitSetRequest.class.getName() + ".vars";
    private static final long DEFAULT_TIMEOUT;
    private static final long MIN_TIMEOUT;
    private static final long MAX_TIMEOUT;
    private static final long DEFAULT_ADMIN_TIMEOUT;
    private static final long MIN_ADMIN_TIMEOUT;
    private static final long MAX_ADMIN_TIMEOUT;
    private static final long INITIAL_SLEEP_TIME_MILLIS;
    private static final long NODATA_SLEEP_TIME_MILLIS;

    static {
        DEFAULT_TIMEOUT = LC.zimbra_waitset_default_request_timeout.longValueWithinRange(1, Constants.SECONDS_PER_DAY);
        MIN_TIMEOUT = LC.zimbra_waitset_min_request_timeout.longValueWithinRange(1, Constants.SECONDS_PER_DAY);
        MAX_TIMEOUT = LC.zimbra_waitset_max_request_timeout.longValueWithinRange(1, Constants.SECONDS_PER_DAY);

        DEFAULT_ADMIN_TIMEOUT = LC.zimbra_admin_waitset_default_request_timeout.longValueWithinRange(1, Constants.SECONDS_PER_DAY);
        MIN_ADMIN_TIMEOUT = LC.zimbra_admin_waitset_min_request_timeout.longValueWithinRange(1, Constants.SECONDS_PER_DAY);
        MAX_ADMIN_TIMEOUT = LC.zimbra_admin_waitset_max_request_timeout.longValueWithinRange(1, Constants.SECONDS_PER_DAY);

        INITIAL_SLEEP_TIME_MILLIS = LC.zimbra_waitset_initial_sleep_time.longValueWithinRange(1, 5 * Constants.SECONDS_PER_MINUTE * 1000);
        NODATA_SLEEP_TIME_MILLIS = LC.zimbra_waitset_nodata_sleep_time.longValueWithinRange(1, 5 * Constants.SECONDS_PER_MINUTE * 1000);
    }

    public static long getTimeoutMillis(Element request, boolean isAdminRequest) throws ServiceException {
        long to;
        if (!isAdminRequest) {
            to = request.getAttributeLong(MailConstants.A_TIMEOUT, DEFAULT_TIMEOUT);
        } else {
            to = request.getAttributeLong(MailConstants.A_TIMEOUT, DEFAULT_ADMIN_TIMEOUT);
        }
        return getTimeoutMillis(to, isAdminRequest);
    }

    public static long getTimeoutMillis(Long timeout, boolean isAdminRequest) throws ServiceException {
        long to;
        if (!isAdminRequest) {
            to = (timeout != null) ? timeout : DEFAULT_TIMEOUT;
            if (to < MIN_TIMEOUT)
                to = MIN_TIMEOUT;
            if (to > MAX_TIMEOUT)
                to = MAX_TIMEOUT;
        } else {
            to = (timeout != null) ? timeout : DEFAULT_ADMIN_TIMEOUT;
            if (to < MIN_ADMIN_TIMEOUT)
                to = MIN_ADMIN_TIMEOUT;
            if (to > MAX_ADMIN_TIMEOUT)
                to = MAX_ADMIN_TIMEOUT;
        }
        return to * 1000;
    }

    @Override
    public void preProxy(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        boolean adminAllowed = zsc.getAuthToken().isAdmin();
        setProxyTimeout(getTimeoutMillis(request, adminAllowed) + 10 * Constants.MILLIS_PER_SECOND);
        super.preProxy(request, context);
    }

    /*
<!--*************************************
    WaitMultipleAccounts:  optionally modifies the wait set and checks
    for any notifications.  If block=1 and there are no notificatins, then
    this API will BLOCK until there is data.

    Client should always set 'seq' to be the highest known value it has
    received from the server.  The server will use this information to
    retransmit lost data.

    If the client sends a last known sync token then the notification is
    calculated by comparing the accounts current token with the client's
    last known.

    If the client does not send a last known sync token, then notification
    is based on change since last Wait (or change since <add> if this
    is the first time Wait has been called with the account)
    ************************************* -->
<WaitMultipleAccountsRequest waitSet="setId" seq="highestSeqKnown" [block="1"]>
  [ <add>
      [<a id="ACCTID" [token="lastKnownSyncToken"] [types="a,c..."]/>]+
    </add> ]
  [ <update>
      [<a id="ACCTID" [token="lastKnownSyncToken"] [types=]/>]+
    </update> ]
  [ <remove>
      [<a id="ACCTID"/>]+
    </remove> ]
</WaitMultipleAccountsRequest>

<WaitMultipleAccountsResponse waitSet="setId" seq="seqNo" [canceled="1"]>
  [ <n id="ACCTID"/>]*
  [ <error ...something.../>]*
</WaitMultipleAccountsResponse>
     */

    /* (non-Javadoc)
     * @see com.zimbra.soap.DocumentHandler#handle(com.zimbra.common.soap.Element, java.util.Map)
     */
    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        com.zimbra.soap.mail.message.WaitSetRequest req = zsc.elementToJaxb(request);
        boolean adminAllowed = zsc.getAuthToken().isAdmin();
        WaitSetResponse resp = new WaitSetResponse();
        staticHandle(req, context, resp, adminAllowed);
        return zsc.jaxbToElement(resp);  /* MUST use zsc variant NOT JaxbUtil */
    }

    public static void staticHandle(WaitSetReq req, Map<String, Object> context, WaitSetResp resp,
            boolean adminAllowed)
    throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        HttpServletRequest servletRequest = (HttpServletRequest) context.get(SoapServlet.SERVLET_REQUEST);

        String waitSetId = req.getWaitSetId();
        String lastKnownSeqNo = req.getLastKnownSeqNo();
        boolean block = req.getBlock();
        boolean expand = req.getExpand();

        WaitSetCallback cb = (WaitSetCallback)servletRequest.getAttribute(VARS_ATTR_NAME);

        if (cb == null) { // Initial
            Continuation continuation = ContinuationSupport.getContinuation(servletRequest);
            cb = new WaitSetCallback();
            cb.continuationResume = new ResumeContinuationListener(continuation);
            servletRequest.setAttribute(VARS_ATTR_NAME, cb);
            servletRequest.setAttribute(ZimbraSoapContext.soapRequestIdAttr, zsc.getSoapRequestId());

            String defInterestStr = null;
            if (waitSetId.startsWith(WaitSetMgr.ALL_ACCOUNTS_ID_PREFIX)) {
                WaitSetMgr.checkRightForAllAccounts(zsc);

                // default interest types required for "All" waitsets
                defInterestStr = req.getDefaultInterests();
                Set<MailItem.Type> defaultInterests = WaitSetRequest.parseInterestStr(defInterestStr,
                        EnumSet.noneOf(MailItem.Type.class));
                cb.ws = WaitSetMgr.lookupOrCreateForAllAccts(
                        zsc.getRequestedAccountId(), waitSetId, defaultInterests, lastKnownSeqNo);
            } else {
                cb.ws = WaitSetMgr.lookup(waitSetId);
            }

            if (cb.ws == null)
                throw AdminServiceException.NO_SUCH_WAITSET(waitSetId);

            WaitSetMgr.checkRightForOwnerAccount(cb.ws, zsc.getRequestedAccountId());

            List<WaitSetAccount> add = parseAddUpdateAccounts(zsc, req.getAddAccounts(), cb.ws.getDefaultInterest());
            List<WaitSetAccount> update =
                    parseAddUpdateAccounts(zsc, req.getUpdateAccounts(), cb.ws.getDefaultInterest());
            List<String> remove = parseRemoveAccounts(zsc, req.getRemoveAccounts());

            ///////////////////
            // workaround for 27480: load the mailboxes NOW, before we grab the waitset lock
            preloadMailboxes(add);
            preloadMailboxes(update);
            // END workaround for 27480
            ///////////////////

            // Force the client to wait briefly before processing -- this will stop 'bad' clients from polling
            // the server in a very fast loop (they should be using the 'block' mode)
            if(!block && !adminAllowed) {
                try { Thread.sleep(INITIAL_SLEEP_TIME_MILLIS); } catch (InterruptedException ex) {}
            }

            cb.errors.addAll(cb.ws.removeAccounts(remove));
            synchronized(cb.ws) { // bug 28190: always grab the WS lock before the CB lock.
                synchronized(cb) {
                    // note that doWait for AllAccountsWaitSet ignores 'add' and 'update'
                    cb.errors.addAll(cb.ws.doWait(cb, lastKnownSeqNo, add, update));
                    // after this point, the ws has a pointer to the cb and so we *MUST NOT* lock
                    // the ws until we release the cb lock!
                    if (cb.completed) {
                        block = false;
                    }
                }
            }

            if (block) {
                noDataSleep(cb);
                synchronized (cb) {
                    if (!cb.completed) { // don't wait if it completed right away
                        long timeout = getTimeoutMillis(req.getTimeout(), adminAllowed);
                        ZimbraLog.soap.trace("Suspending <WaitSetRequest> for %dms", timeout);
                        cb.continuationResume.suspendAndUndispatch(timeout);
                    }
                }
            }
        }

        // if we got here, then we did *not* execute a jetty RetryContinuation,
        // soooo, we'll fall through and finish up at the bottom
        processCallback(resp, cb, waitSetId, lastKnownSeqNo, expand);

        // publish WaitSetResp to all subscribers via Redis
        final DistributedWaitSet dws = DistributedWaitSet.getInstance();
        if (!isEmpty(cb.signalledAccounts)) {
            cb.signalledAccounts.forEach((accountId) -> dws.publish(accountId, resp));
        }
    }

    /** No data after initial check...wait up to a few extra seconds before going into the
     * notification wait...basically we're just trying to let the server coalesce notification
     * data a little bit. */
    private static void noDataSleep(WaitSetCallback cb) {
        CountDownLatch doneSignal = new CountDownLatch(1);
        cb.completedLatch = doneSignal;
        try {
            doneSignal.await(NODATA_SLEEP_TIME_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ie) {
            ZimbraLog.soap.trace("WaitSetRequest.noDataSleep await threw exception", ie);
        }
    }

    private static void processCallback(WaitSetResp resp, WaitSetCallback cb, String waitSetId,
            String lastKnownSeqNo, boolean expand)
                    throws ServiceException {
        cb.ws.doneWaiting(cb);

        resp.setWaitSetId(waitSetId);
        if (cb.canceled) {
            resp.setCanceled(true);
        } else if (cb.completed) {
            resp.setSeqNo(cb.seqNo);
            for (String signalledAccount : cb.signalledAccounts) {
                WaitSetSession signalledSession = cb.signalledSessions.get(signalledAccount);
                int lastChangeId = 0;
                Set<Integer> folderInterests = null;
                if (signalledSession != null) {
                    folderInterests = signalledSession.getFolderInterest();
                    if(signalledSession.getMailbox() != null) {
                        lastChangeId = signalledSession.getMailbox().getLastChangeID();
                    }
                } else {
                    Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(signalledAccount,
                            false);
                    if(mbox != null) {
                        lastChangeId = mbox.getLastChangeID();
                    }
                }
                AccountWithModifications info = new AccountWithModifications(signalledAccount, lastChangeId);
                @SuppressWarnings("rawtypes")
                PendingModifications accountMods = cb.pendingModifications.get(signalledAccount);
                Map<Integer, PendingFolderModifications> folderMap =
                        PendingModifications.encodeIMAPFolderModifications(accountMods, folderInterests);
                if (folderInterests!= null && !folderInterests.isEmpty() && !folderMap.isEmpty()) {
                    //interested only in specific folders
                    if (expand) {
                        info.setPendingFolderModifications(folderMap.values());
                    }
                    resp.addSignalledAccount(info);
                } else if (folderInterests == null || folderInterests.isEmpty()) {
                    //interested in any folder
                    resp.addSignalledAccount(info);
                }
            }
        } else {
            // timed out....they should try again
            resp.setSeqNo(lastKnownSeqNo);
        }
        resp.setErrors(encodeErrors(cb.errors));
    }

    private static void preloadMailboxes(List<WaitSetAccount> accts) {
        for (WaitSetAccount acct : accts) {
            try {
                MailboxManager.getInstance().getMailboxByAccountId(acct.getAccountId(),
                        MailboxManager.FetchMode.AUTOCREATE);
            } catch (ServiceException e) {
                ZimbraLog.session.debug("Caught exception preloading mailbox for %s", acct, e);
            }
        }
    }

    protected static List<WaitSetAccount> parseAddUpdateAccounts(ZimbraSoapContext zsc,
            List<WaitSetAddSpec> accountDetails, Set<MailItem.Type> defaultInterest)
    throws ServiceException {
        List<WaitSetAccount> toRet = new ArrayList<WaitSetAccount>();
        if (accountDetails != null) {
            for (WaitSetAddSpec accountDetail : accountDetails) {
                String id;
                String name = accountDetail.getName();
                if (name != null) {
                    Account acct = Provisioning.getInstance().get(AccountBy.name, name);
                    if (acct != null) {
                        id = acct.getId();
                    } else {
                        // TODO - what's going on here???  Presumably this should be being used
                        WaitSetError err = new WaitSetError(name, WaitSetError.Type.NO_SUCH_ACCOUNT);
                        continue;
                    }
                } else {
                    id = accountDetail.getId();
                }

                WaitSetMgr.checkRightForAdditionalAccount(id, zsc);

                String tokenStr = accountDetail.getToken();
                SyncToken token = tokenStr != null ? new SyncToken(tokenStr) : null;
                Set<MailItem.Type> interests = parseInterestStr(accountDetail.getInterests(), defaultInterest);
                Set<Integer> folderInterests = accountDetail.getFolderInterestsAsSet();
                toRet.add(new WaitSetAccount(id, token, interests, folderInterests));
            }
        }
        return toRet;
    }

    private static List<String> parseRemoveAccounts(ZimbraSoapContext zsc, List<Id> ids)
            throws ServiceException {
        List<String> remove = Lists.newArrayList();
        if (ids != null) {
            for (Id currid : ids) {
                String id = currid.getId();
                WaitSetMgr.checkRightForAdditionalAccount(id, zsc);
                remove.add(id);
            }
        }
        return remove;
    }

    public static enum TypeEnum {
        f(EnumSet.of(MailItem.Type.FOLDER)),
        m(EnumSet.of(MailItem.Type.MESSAGE)),
        c(EnumSet.of(MailItem.Type.CONTACT)),
        a(EnumSet.of(MailItem.Type.APPOINTMENT)),
        t(EnumSet.of(MailItem.Type.TASK)),
        d(EnumSet.of(MailItem.Type.DOCUMENT)),
        all(EnumSet.of(MailItem.Type.FOLDER, MailItem.Type.MESSAGE, MailItem.Type.CONTACT,
                       MailItem.Type.APPOINTMENT, MailItem.Type.TASK, MailItem.Type.DOCUMENT));

        private final Set<MailItem.Type> types;

        TypeEnum(Set<MailItem.Type> types) {
            this.types = types;
        }

        public Set<MailItem.Type> getTypes() {
            return types;
        }
    }

    public static final String expandInterestStr(Set<MailItem.Type> interest) {
        if (interest.containsAll(TypeEnum.all.getTypes())) {
            return TypeEnum.all.name();
        }

        StringBuilder result = new StringBuilder();
        for (MailItem.Type type : interest) {
            switch (type) {
            case FOLDER:
                result.append(TypeEnum.f.name());
                break;
            case MESSAGE:
                result.append(TypeEnum.m.name());
                break;
            case CONTACT:
                result.append(TypeEnum.c.name());
                break;
            case APPOINTMENT:
                result.append(TypeEnum.a.name());
                break;
            case TASK:
                result.append(TypeEnum.t.name());
                break;
            case DOCUMENT:
                result.append(TypeEnum.d.name());
                break;
            default:
                break;
            }
        }
        return result.toString();
    }

    public static final List<IdAndType> encodeErrors(List<WaitSetError> errors) {
        if ((errors == null) || errors.size() == 0) {
            return null;
        }
        List<IdAndType> errs = Lists.newArrayList();
        for (WaitSetError error : errors) {
            errs.add(new IdAndType(error.accountId, error.error.name()));
        }
        return errs;
    }

    public static final Set<MailItem.Type> parseInterestStr(String typesList, Set<MailItem.Type> defaultInterest) {
        if (typesList == null) {
            return defaultInterest;
        }

        EnumSet<MailItem.Type> result = EnumSet.noneOf(MailItem.Type.class);
        for (String s : Splitter.on(',').trimResults().split(typesList)) {
            TypeEnum te = TypeEnum.valueOf(s);
            result.addAll(te.getTypes());
        }
        return result;
    }

    public static final String interestToStr(Set<MailItem.Type> interests) {
        EnumSet<TypeEnum> result = EnumSet.noneOf(TypeEnum.class);
        for (MailItem.Type type : interests) {
            switch (type) {
            case FOLDER:
                result.add(TypeEnum.f);
                break;
            case MESSAGE:
                result.add(TypeEnum.m);
                break;
            case CONTACT:
                result.add(TypeEnum.c);
                break;
            case APPOINTMENT:
                result.add(TypeEnum.a);
                break;
            case TASK:
                result.add(TypeEnum.t);
                break;
            case DOCUMENT:
                result.add(TypeEnum.d);
                break;
            default:
                break;
            }
        }
        return Joiner.on(',').join(result);
    }

}
