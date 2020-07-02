/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
 * Created on Apr 30, 2005
 */
package com.zimbra.cs.imap;

import java.io.ObjectInputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.event.ZEventHandler;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.MailboxStore;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapTransport.NotificationFormat;
import com.zimbra.common.util.SystemUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.zclient.ZClientException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.soap.type.AccountWithModifications;

public class ImapCredentials implements java.io.Serializable {
    private static final long serialVersionUID = -3323076274740054770L;

    /** The various special modes the server can be thrown into in order to
     *  deal with client weirdnesses.  These modes are specified by appending
     *  various suffixes to the USERNAME when logging into the IMAP server; for
     *  instance, the Windows Mobile 5 hack is enabled via the suffix "/wm". */
    static enum EnabledHack {
        NONE, WM5("/wm"), THUNDERBIRD("/tb"), NO_IDLE("/ni");

        private String extension;
        EnabledHack()            { }
        EnabledHack(String ext)  { extension = ext; }

        @Override public String toString()  { return extension; }
    }

    transient private ImapMailboxStore mStore = null;
    private final String      mAccountId;
    private final String      mUsername;
    private final boolean     mIsLocal;
    private final EnabledHack mEnabledHack;
    private Set<ImapPath>     mHiddenFolders;
    private final ZEventHandler zMailboxEventHandler = new ZEventHandler() {
        @Override
        public void handlePendingModification(int changeId, AccountWithModifications info) throws ServiceException {
            ZimbraLog.imap.debug("Handling ZMailbox modification changeId=%s info=%s", changeId, info);
            MailboxStore store = getMailbox();
            if(store != null && store instanceof ZMailbox) {
                ImapServerListenerPool.getInstance().get((ZMailbox)store).notifyAccountChange(info);
            }
        }
    };

    public ImapCredentials(Account acct) throws ServiceException {
        this(acct, EnabledHack.NONE);
    }

    protected ImapCredentials(Account acct, EnabledHack hack) throws ServiceException {
        mAccountId = acct.getId();
        mUsername = acct.getName();
        mIsLocal = Provisioning.onLocalServer(acct);
        mEnabledHack = (hack == null ? EnabledHack.NONE : hack);
    }

    protected String getUsername() {
        return mUsername;
    }

    protected boolean isLocal() {
        return mIsLocal;
    }

    protected boolean isHackEnabled(EnabledHack hack) {
        if (hack == null) {
            return (mEnabledHack == null);
        }
        return hack.equals(mEnabledHack);
    }

    protected EnabledHack[] getEnabledHacks() {
        if (mEnabledHack == null || mEnabledHack == EnabledHack.NONE)
            return null;
        return new EnabledHack[] { mEnabledHack };
    }

    protected String getAccountId() {
        return mAccountId;
    }

    protected Account getAccount() throws ServiceException {
        return Provisioning.getInstance().get(Key.AccountBy.id, mAccountId);
    }

    protected OperationContext getContext() throws ServiceException {
        return new OperationContext(mAccountId);
    }

    protected MailboxStore getMailbox() throws ServiceException {
        ImapMailboxStore imapStore = getImapMailboxStore();
        return imapStore.getMailboxStore();
    }

    protected ImapMailboxStore getImapMailboxStore() throws ServiceException {
        if(mStore != null) {
            return mStore;
        }
        if (mIsLocal && !LC.imap_always_use_remote_store.booleanValue() && ImapDaemon.isRunningImapInsideMailboxd()) {
            ZimbraLog.imap.debug("ImapCredentials returning local mailbox store for %s", mAccountId);
            return new LocalImapMailboxStore(MailboxManager.getInstance().getMailboxByAccountId(mAccountId));
        }
        try {
            Account acct = getAccount();
            ZMailbox.Options options =
                    new ZMailbox.Options(AuthProvider.getAuthToken(acct).getEncoded(), AccountUtil.getSoapUri(acct));
            /* getting by ID avoids failed GetInfo SOAP requests trying to determine ID before auth setup. */
            options.setTargetAccountBy(AccountBy.id);
            options.setTargetAccount(acct.getId());
            options.setNoSession(false);
            options.setUserAgent("zclient-imap", SystemUtil.getProductVersion());
            options.setNotificationFormat(NotificationFormat.IMAP);
            options.setAlwaysRefreshFolders(true);
            ZMailbox store =  ZMailbox.getMailbox(options);
            store.setAccountId(acct.getId());
            store.setName(acct.getName());
            store.setAuthName(acct.getName());
            mStore = ImapMailboxStore.get(store, mAccountId);
            ZimbraLog.imap.debug("Registering listener with ZMailbox for '%s' [id=%s]",
                    acct.getName(), mAccountId);
            store.addEventHandler(zMailboxEventHandler);
            return mStore;
        } catch (AuthTokenException ate) {
            throw ServiceException.FAILURE("error generating auth token", ate);
        }
    }

    private void saveSubscriptions(Set<String> subscriptions) throws ServiceException {
        getImapMailboxStore().saveSubscriptions(getContext(), subscriptions);
    }

    /** @return Modifiable set of subscriptions  or null if there are no subscriptions */
    protected Set<String> listSubscriptions() throws ServiceException {
        Set<String> subs = getImapMailboxStore().listSubscriptions(getContext());
        /* subs may be an unmodifiable set as that is what the JAXB provides in the remote case.
         * Change it into a modifiable set, as the result is modified when subscribing/unsubscribing
         */
        return (subs == null || subs.isEmpty()) ? null : Sets.newHashSet(subs);
    }

    protected void subscribe(ImapPath path) throws ServiceException {
        Set<String> subscriptions = listSubscriptions();
        if (subscriptions != null && !subscriptions.isEmpty()) {
            String upcase = path.asImapPath().toUpperCase();
            for (String sub : subscriptions) {
                if (upcase.equals(sub.toUpperCase()))
                    return;
            }
        }
        if (subscriptions == null)
            subscriptions = new HashSet<String>();
        subscriptions.add(path.asImapPath());
        saveSubscriptions(subscriptions);
    }

    protected void unsubscribe(ImapPath path) throws ServiceException {
        Set<String> subscriptions = listSubscriptions();
        if (subscriptions == null || subscriptions.isEmpty())
            return;
        String upcase = path.asImapPath().toUpperCase();
        boolean found = false;
        for (Iterator<String> it = subscriptions.iterator(); it.hasNext(); ) {
            if (upcase.equals(it.next().toUpperCase())) {
                it.remove();  found = true;
            }
        }
        if (!found)
            return;
        saveSubscriptions(subscriptions);
    }

    protected void hideFolder(ImapPath path) {
        if (mHiddenFolders == null)
            mHiddenFolders = new HashSet<ImapPath>();
        mHiddenFolders.add(path);
    }

    protected boolean isFolderHidden(ImapPath path) {
        return mHiddenFolders == null ? false : mHiddenFolders.contains(path);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", mUsername)
                .add("acctId", mAccountId)
                .add("hiddenFolders", mHiddenFolders)
                .add("isLocal", mIsLocal)
                .add("enabledHack", mEnabledHack)
                .toString();
    }

    public void logout() {
        if(mStore != null) {
            MailboxStore store = mStore.getMailboxStore();
            if(store != null && store instanceof ZMailbox) {
                try {
                    ((ZMailbox)store).logout();
                } catch (ZClientException e) {
                    ZimbraLog.imap.error("ZMailbox failed to logout", e);
                } finally {
                    mStore = null;
                }
            }
        }
    }

    // ZCS-6695 Deserialization protection
    private final void readObject(ObjectInputStream in) throws java.io.IOException {
        throw new java.io.IOException("Cannot be deserialized");
    }
}
