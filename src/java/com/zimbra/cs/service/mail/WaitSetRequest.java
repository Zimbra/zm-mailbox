/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.service.admin.AdminServiceException;
import com.zimbra.cs.service.util.SyncToken;
import com.zimbra.cs.session.IWaitSet;
import com.zimbra.cs.session.WaitSetAccount;
import com.zimbra.cs.session.WaitSetCallback;
import com.zimbra.cs.session.WaitSetError;
import com.zimbra.cs.session.WaitSetMgr;
import com.zimbra.soap.SoapServlet;
import com.zimbra.soap.ZimbraSoapContext;

/**
 *
 */
public class WaitSetRequest extends MailDocumentHandler {

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

    private static long getTimeoutMillis(Element request, boolean isAdminRequest) throws ServiceException {
        long to;
        if (!isAdminRequest) {
            to = request.getAttributeLong(MailConstants.A_TIMEOUT, DEFAULT_TIMEOUT);
            if (to < MIN_TIMEOUT)
                to = MIN_TIMEOUT;
            if (to > MAX_TIMEOUT)
                to = MAX_TIMEOUT;
        } else {
            to = request.getAttributeLong(MailConstants.A_TIMEOUT, DEFAULT_ADMIN_TIMEOUT);
            if (to < MIN_ADMIN_TIMEOUT)
                to = MIN_ADMIN_TIMEOUT;
            if (to > MAX_ADMIN_TIMEOUT)
                to = MAX_ADMIN_TIMEOUT;
        }
        return to * 1000;
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


    private static final String VARS_ATTR_NAME = WaitSetRequest.class.getName()+".vars";

    /* (non-Javadoc)
     * @see com.zimbra.soap.DocumentHandler#handle(com.zimbra.common.soap.Element, java.util.Map)
     */
    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        boolean adminAllowed = zsc.getAuthToken().isAdmin();
        Element response = zsc.createElement(MailConstants.WAIT_SET_RESPONSE);
        return staticHandle(request, context, response, adminAllowed);
    }

    public static Element staticHandle(Element request, Map<String, Object> context, Element response, boolean adminAllowed) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        HttpServletRequest servletRequest = (HttpServletRequest) context.get(SoapServlet.SERVLET_REQUEST);

        String waitSetId = request.getAttribute(MailConstants.A_WAITSET_ID);
        String lastKnownSeqNo = request.getAttribute(MailConstants.A_SEQ);
        boolean block = request.getAttributeBool(MailConstants.A_BLOCK, false);

        Callback cb;
        Continuation continuation = ContinuationSupport.getContinuation(servletRequest);
        if (!continuation.isInitial()) {
            cb  = (Callback)servletRequest.getAttribute(VARS_ATTR_NAME);
            // load variables here
        } else {
            cb = new Callback();
            cb.continuation = continuation;
            servletRequest.setAttribute(VARS_ATTR_NAME, cb);

            String defInterestStr = null;
            if (waitSetId.startsWith(WaitSetMgr.ALL_ACCOUNTS_ID_PREFIX)) {
                WaitSetMgr.checkRightForAllAccounts(zsc);
                
                // default interest types required for "All" waitsets
                defInterestStr = request.getAttribute(MailConstants.A_DEFTYPES);
                Set<MailItem.Type> defaultInterests = WaitSetRequest.parseInterestStr(defInterestStr,
                        EnumSet.noneOf(MailItem.Type.class));
                cb.ws = WaitSetMgr.lookupOrCreateForAllAccts(zsc.getRequestedAccountId(), waitSetId, defaultInterests, lastKnownSeqNo);
            } else {
                cb.ws = WaitSetMgr.lookup(waitSetId);
            }

            if (cb.ws == null)
                throw AdminServiceException.NO_SUCH_WAITSET(waitSetId);

            WaitSetMgr.checkRightForOwnerAccount(cb.ws, zsc.getRequestedAccountId());

            List<WaitSetAccount> add = parseAddUpdateAccounts(zsc, 
                request.getOptionalElement(MailConstants.E_WAITSET_ADD), cb.ws.getDefaultInterest());
            List<WaitSetAccount> update = parseAddUpdateAccounts(zsc, 
                request.getOptionalElement(MailConstants.E_WAITSET_UPDATE), cb.ws.getDefaultInterest());
            List<String> remove = parseRemoveAccounts(zsc, request.getOptionalElement(MailConstants.E_WAITSET_REMOVE));

            ///////////////////
            // workaround for 27480: load the mailboxes NOW, before we grab the waitset lock
            List<Mailbox> referencedMailboxes = new ArrayList<Mailbox>();
            for (WaitSetAccount acct : add) {
                try {
                    Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(acct.getAccountId(), MailboxManager.FetchMode.AUTOCREATE);
                    referencedMailboxes.add(mbox);
                } catch (ServiceException e) {
                    ZimbraLog.session.debug("Caught exception preloading mailbox for waitset", e);
                }
            }
            for (WaitSetAccount acct : update) {
                try {
                    Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(acct.getAccountId(), MailboxManager.FetchMode.AUTOCREATE);
                    referencedMailboxes.add(mbox);
                } catch (ServiceException e) {
                    ZimbraLog.session.debug("Caught exception preloading mailbox for waitset", e);
                }
            }
            // END workaround for 27480
            ///////////////////


            // Force the client to wait briefly before processing -- this will stop 'bad' clients from polling
            // the server in a very fast loop (they should be using the 'block' mode)
            try { Thread.sleep(INITIAL_SLEEP_TIME_MILLIS); } catch (InterruptedException ex) {}

            cb.errors.addAll(cb.ws.removeAccounts(remove));
            synchronized(cb.ws) { // bug 28190: always grab the WS lock before the CB lock.
                synchronized(cb) {
                    cb.errors.addAll(cb.ws.doWait(cb, lastKnownSeqNo, add, update));
                    // after this point, the ws has a pointer to the cb and so we *MUST NOT* lock
                    // the ws until we release the cb lock!
                    if (cb.completed)
                        block = false;
                }
            }

            if (block) {
                // No data after initial check...wait a few extra seconds
                // before going into the notification wait...basically we're just
                // trying to let the server coalesce notification data a little
                // bit.
                try { Thread.sleep(NODATA_SLEEP_TIME_MILLIS); } catch (InterruptedException ex) {}

                synchronized (cb) {
                    if (!cb.completed) { // don't wait if it completed right away
                        long timeout = getTimeoutMillis(request, adminAllowed);
                        if (ZimbraLog.soap.isTraceEnabled())
                            ZimbraLog.soap.trace("Suspending <WaitSetRequest> for %dms", timeout);
                        continuation.setTimeout(timeout);
                        continuation.suspend();
                        continuation.undispatch();
                    }
                }
            }
        }

        // if we got here, then we did *not* execute a jetty RetryContinuation,
        // soooo, we'll fall through and finish up at the bottom

        // clear the
        cb.ws.doneWaiting();

        response.addAttribute(MailConstants.A_WAITSET_ID, waitSetId);
        if (cb.canceled) {
            response.addAttribute(MailConstants.A_CANCELED, true);
        } else if (cb.completed) {
            response.addAttribute(MailConstants.A_SEQ, cb.seqNo);

            for (String s : cb.signalledAccounts) {
                Element saElt = response.addElement(MailConstants.E_A);
                saElt.addAttribute(MailConstants.A_ID, s);
            }
        } else {
            // timed out....they should try again
            response.addAttribute(MailConstants.A_SEQ, lastKnownSeqNo);
        }

        encodeErrors(response, cb.errors);

        return response;
    }

    /**
     * @param allowedAccountIds NULL means "all allowed" (admin)
     */
    static List<WaitSetAccount> parseAddUpdateAccounts(ZimbraSoapContext zsc, Element elt, Set<MailItem.Type> defaultInterest) 
    throws ServiceException {
        List<WaitSetAccount> toRet = new ArrayList<WaitSetAccount>();
        if (elt != null) {
            for (Iterator<Element> iter = elt.elementIterator(MailConstants.E_A); iter.hasNext();) {
                Element a = iter.next();
                String id;
                String name = a.getAttribute(MailConstants.A_NAME, null);
                if (name != null) {
                    Account acct = Provisioning.getInstance().get(AccountBy.name, name);
                    if (acct != null) {
                        id = acct.getId();
                    } else {
                        WaitSetError err = new WaitSetError(name, WaitSetError.Type.NO_SUCH_ACCOUNT);
                        continue;
                    }
                } else {
                    id = a.getAttribute(MailConstants.A_ID);
                }
                
                WaitSetMgr.checkRightForAdditionalAccount(id, zsc);

                String tokenStr = a.getAttribute(MailConstants.A_TOKEN, null);
                SyncToken token = tokenStr != null ? new SyncToken(tokenStr) : null;
                Set<MailItem.Type> interests = parseInterestStr(a.getAttribute(MailConstants.A_TYPES, null), defaultInterest);
                toRet.add(new WaitSetAccount(id, token, interests));
            }
        }
        
        return toRet;
    }

    static List<String> parseRemoveAccounts(ZimbraSoapContext zsc, Element elt) throws ServiceException {
        List<String> remove = new ArrayList<String>();
        if (elt != null) {
            for (Iterator<Element> iter = elt.elementIterator(MailConstants.E_A); iter.hasNext();) {
                Element a = iter.next();
                String id = a.getAttribute(MailConstants.A_ID);
                WaitSetMgr.checkRightForAdditionalAccount(id, zsc);
                remove.add(id);
            }
        }
        return remove;
    }

    public static class Callback implements WaitSetCallback {
        @Override
        public void dataReady(IWaitSet ws, String seqNo, boolean canceled, List<WaitSetError> inErrors, String[] signalledAccounts) {
            boolean trace = ZimbraLog.session.isTraceEnabled();
            if (trace) {
                String accts = signalledAccounts != null ? "[" + StringUtil.join(", ", signalledAccounts) + "]" : "<null>";
                ZimbraLog.session.trace("WaitSetRequest.Callback.dataReady: ws=" + ws.getWaitSetId() + ", seq=" + seqNo +
                        (canceled ? ", CANCEL" : "") + ", accounts=" + accts);
            }
            synchronized(this) {
                ZimbraLog.session.debug("WaitSet: Called WaitSetCallback.dataReady()!");
                if (inErrors != null && inErrors.size() > 0)
                    errors.addAll(inErrors);
                this.waitSet = ws;
                this.canceled = canceled;
                this.signalledAccounts = signalledAccounts;
                this.seqNo = seqNo;
                this.completed = true;
                if (continuation != null) {
                    if (trace) ZimbraLog.session.trace("WaitSetRequest.Callback.dataReady 1");
                    if (continuation.isSuspended()) {
                        continuation.resume();
                    }
                    if (trace) ZimbraLog.session.trace("WaitSetRequest.Callback.dataReady 2");
                }
            }
            if (trace) ZimbraLog.session.trace("WaitSetRequest.Callback.dataReady done");
        }

        public boolean completed = false;
        public boolean canceled;
        public String[] signalledAccounts;
        public IWaitSet waitSet;
        public String seqNo;
        public IWaitSet ws;
        public List<WaitSetError> errors = new ArrayList<WaitSetError>();
        public Continuation continuation;
    }

    public static enum TypeEnum {
        m(EnumSet.of(MailItem.Type.MESSAGE)),
        c(EnumSet.of(MailItem.Type.CONTACT)),
        a(EnumSet.of(MailItem.Type.APPOINTMENT)),
        t(EnumSet.of(MailItem.Type.TASK)),
        d(EnumSet.of(MailItem.Type.DOCUMENT)),
        all(EnumSet.of(MailItem.Type.MESSAGE, MailItem.Type.CONTACT,
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
            }
        }
        return result.toString();
    }

    public static final void encodeErrors(Element parent, List<WaitSetError> errors) {
        for (WaitSetError error : errors) {
            Element errorElt = parent.addElement(MailConstants.E_ERROR);
            errorElt.addAttribute(MailConstants.A_ID, error.accountId);
            errorElt.addAttribute(MailConstants.A_TYPE, error.error.name());
        }
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
            }
        }
        return Joiner.on(',').join(result);
    }

}
