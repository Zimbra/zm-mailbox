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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.zimbra.client.ZMailbox;
import com.zimbra.common.account.Key;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.MailboxStore;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.MetadataList;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.util.AccountUtil;

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

    private static final String SN_IMAP = "imap";
    private static final String FN_SUBSCRIPTIONS = "subs";

    private final String      mAccountId;
    private final String      mUsername;
    private final boolean     mIsLocal;
    private final EnabledHack mEnabledHack;
    private Set<ImapPath>     mHiddenFolders;

    public ImapCredentials(Account acct) throws ServiceException {
        this(acct, EnabledHack.NONE);
    }

    ImapCredentials(Account acct, EnabledHack hack) throws ServiceException {
        mAccountId = acct.getId();
        mUsername = acct.getName();
        mIsLocal = Provisioning.onLocalServer(acct);
        mEnabledHack = (hack == null ? EnabledHack.NONE : hack);
    }

    String getUsername() {
        return mUsername;
    }

    boolean isLocal() {
        return mIsLocal;
    }

    boolean isHackEnabled(EnabledHack hack) {
        return mEnabledHack == hack;
    }

    EnabledHack[] getEnabledHacks() {
        if (mEnabledHack == null || mEnabledHack == EnabledHack.NONE)
            return null;
        return new EnabledHack[] { mEnabledHack };
    }

    String getAccountId() {
        return mAccountId;
    }

    Account getAccount() throws ServiceException {
        return Provisioning.getInstance().get(Key.AccountBy.id, mAccountId);
    }

    OperationContext getContext() throws ServiceException {
        return new OperationContext(mAccountId);
    }

    MailboxStore getMailbox() throws ServiceException {
        ImapMailboxStore imapStore = getImapMailboxStore();
        return imapStore.getMailboxStore();
    }

    ImapMailboxStore getImapMailboxStore() throws ServiceException {
        if (mIsLocal && !LC.imap_always_use_remote_store.booleanValue()) {
            ZimbraLog.imap.debug("ImapCredentials returning local mailbox store for %s", mAccountId);
            return new LocalImapMailboxStore(MailboxManager.getInstance().getMailboxByAccountId(mAccountId));
        }
        try {
            Account acct = getAccount();
            ZMailbox.Options options =
                    new ZMailbox.Options(AuthProvider.getAuthToken(acct).getEncoded(), AccountUtil.getSoapUri(acct));
            options.setTargetAccount(acct.getName());
            options.setNoSession(true);
            MailboxStore store =  ZMailbox.getMailbox(options);
            return ImapMailboxStore.get(store, mAccountId);
        } catch (AuthTokenException ate) {
            throw ServiceException.FAILURE("error generating auth token", ate);
        }
    }

    private void saveSubscriptions(Set<String> subscriptions) throws ServiceException {
        getImapMailboxStore().saveSubscriptions(getContext(), subscriptions);
    }

    Set<String> listSubscriptions() throws ServiceException {
        return getImapMailboxStore().listSubscriptions(getContext());
    }

    void subscribe(ImapPath path) throws ServiceException {
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

    void unsubscribe(ImapPath path) throws ServiceException {
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

    void hideFolder(ImapPath path) {
        if (mHiddenFolders == null)
            mHiddenFolders = new HashSet<ImapPath>();
        mHiddenFolders.add(path);
    }

    boolean isFolderHidden(ImapPath path) {
        return mHiddenFolders == null ? false : mHiddenFolders.contains(path);
    }
}
