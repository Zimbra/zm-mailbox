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
 * Created on Apr 30, 2005
 */
package com.zimbra.cs.imap;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.MetadataList;
import com.zimbra.cs.mailbox.OperationContext;

class ImapCredentials implements java.io.Serializable {
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
        return Provisioning.getInstance().get(Provisioning.AccountBy.id, mAccountId);
    }

    OperationContext getContext() throws ServiceException {
        return new OperationContext(mAccountId);
    }

    Mailbox getMailbox() throws ServiceException {
        if (!mIsLocal)
            throw ServiceException.WRONG_HOST(getAccount().getAttr(Provisioning.A_zimbraMailHost), null);
        return MailboxManager.getInstance().getMailboxByAccountId(mAccountId);
    }


    private Set<String> parseConfig(Metadata config) throws ServiceException {
        if (config == null || !config.containsKey(FN_SUBSCRIPTIONS))
            return null;
        MetadataList slist = config.getList(FN_SUBSCRIPTIONS, true);
        if (slist == null || slist.isEmpty())
            return null;
        Set<String> subscriptions = new HashSet<String>(slist.size());
        for (int i = 0; i < slist.size(); i++)
            subscriptions.add(slist.get(i));
        return subscriptions;
    }

    private void saveConfig(Set<String> subscriptions) throws ServiceException {
        MetadataList slist = new MetadataList();
        if (subscriptions != null && !subscriptions.isEmpty()) {
            for (String sub : subscriptions)
                slist.add(sub);
        }
        getMailbox().setConfig(getContext(), SN_IMAP, new Metadata().put(FN_SUBSCRIPTIONS, slist));
    }

    Set<String> listSubscriptions() throws ServiceException {
        return parseConfig(getMailbox().getConfig(getContext(), SN_IMAP));
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
        saveConfig(subscriptions);
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
        saveConfig(subscriptions);
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
