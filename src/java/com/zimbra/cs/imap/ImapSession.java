/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Apr 30, 2005
 */
package com.zimbra.cs.imap;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.imap.ImapFlagCache.ImapFlag;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.MetadataList;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.session.Session;

/**
 * @author dkarp
 */
public class ImapSession extends Session {

    /** The various special modes the server can be thrown into in order to
     *  deal with client weirdnesses.  These modes are specified by appending
     *  various suffixes to the USERNAME when logging into the IMAP server; for
     *  instance, the Windows Mobile 5 hack is enabled via the suffix "/wm". */
    static enum EnabledHack {
        NONE, WM5("/wm"), THUNDERBIRD("/tb"), NO_IDLE("/ni");

        private String extension;
        EnabledHack()             { }
        EnabledHack(String ext)   { extension = ext; }
        public String toString()  { return extension; }
    }

    public static final long IMAP_IDLE_TIMEOUT_MSEC = 30 * Constants.MILLIS_PER_MINUTE;

    private static final String SN_IMAP = "imap";
    private static final String FN_SUBSCRIPTIONS = "subs";

    private final String      mUsername;
    private final EnabledHack mEnabledHack;

    private ImapHandler mHandler;
    private String      mIdleTag;
    private ImapFlagCache mFlags = new ImapFlagCache();
    private ImapFlagCache mTags = new ImapFlagCache();

    public ImapSession(Account acct, ImapHandler handler, EnabledHack hack) {
        super(acct.getId(), Session.Type.IMAP);
        mUsername = acct.getName();
        mHandler = handler;
        mEnabledHack = (hack == null ? EnabledHack.NONE : hack);
    }

    @Override
    public Session register() throws ServiceException {
        super.register();

        mMailbox.beginTrackingImap(getContext());
        mFlags = ImapFlagCache.getSystemFlags(mMailbox);
//        try {
//            parseConfig(getMailbox().getConfig(getContext(), SN_IMAP));
//        } catch (ServiceException e) { }
        return this;
    }

    @Override
    protected boolean isMailboxListener() {
        return true;
    }

    @Override
    protected boolean isRegisteredInCache() {
        return true;
    }

    @Override
    public void doEncodeState(Element parent) {
        Element e = parent.addElement("imap");
        e.addAttribute("username", mUsername);
        e.addAttribute("idleTag", mIdleTag);
//        if (mSelectedFolder != null) 
//            e.addAttribute("selectedFolder", mSelectedFolder.getPath().asImapPath());
        if (mEnabledHack != null) 
            e.addAttribute("hacks", mEnabledHack.toString());
        if (mHandler != null) 
            mHandler.encodeState(e);
    }

    @Override
    protected long getSessionIdleLifetime() {
        return IMAP_IDLE_TIMEOUT_MSEC;
    }

    void setHandler(ImapHandler handler) {
        mHandler = handler;
    }

    String getUsername() {
        return mUsername;
    }

    boolean isHackEnabled(EnabledHack hack) {
        return mEnabledHack == hack;
    }

    OperationContext getContext() throws ServiceException {
        return new OperationContext(getAuthenticatedAccountId());
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
        mMailbox.setConfig(getContext(), SN_IMAP, new Metadata().put(FN_SUBSCRIPTIONS, slist));
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

    Set<String> listSubscriptions() throws ServiceException {
        return parseConfig(mMailbox.getConfig(getContext(), SN_IMAP));
    }


    void beginIdle(String tag)  { mIdleTag = tag; }

    String endIdle()            { String tag = mIdleTag;  mIdleTag = null;  return tag; }

    boolean isIdle()            { return mIdleTag != null; }


    ImapFlag cacheTag(Tag ltag) {
        return (ltag instanceof Flag ? null : mTags.cache(new ImapFlag(ltag.getName(), ltag, true)));
    }

    ImapFlag getFlagByName(String name) {
        ImapFlag i4flag = mFlags.getByName(name);
        return (i4flag != null ? i4flag : mTags.getByName(name));
    }

    ImapFlag getTagByMask(long mask) {
        return mTags.getByMask(mask);
    }

    String getFlagList(boolean permanentOnly) {
        List <String> names = mFlags.listNames(permanentOnly);
        names.addAll(mTags.listNames(permanentOnly));
        return StringUtil.join(" ", names);
    }

    void clearTagCache() {
        mTags.clear();
    }


    DateFormat getDateFormat() {
        return mHandler.getDateFormat();
    }
    
    DateFormat getZimbraDateFormat() {
        return mHandler.getZimbraFormat();
    }

    private static class AddedItems {
        List<ImapMessage> numbered = new ArrayList<ImapMessage>();
        List<ImapMessage> unnumbered = new ArrayList<ImapMessage>();

        boolean isEmpty()  { return numbered.isEmpty() && unnumbered.isEmpty(); }
        void add(MailItem item) {
            (item.getImapUid() > 0 ? numbered : unnumbered).add(new ImapMessage(item));
        }
        void sort()  { Collections.sort(numbered);  Collections.sort(unnumbered); }
    }

    public void notifyPendingChanges(int changeId, PendingModifications pns) {
        if (!pns.hasNotifications())
            return;

        ImapFolder selectedFolder = mHandler.getSelectedFolder();
        AddedItems added = (selectedFolder == null ? null : new AddedItems());
        if (pns.deleted != null)
            if (!handleDeletes(pns.deleted))
                return;
        if (pns.created != null)
            if (!handleCreates(pns.created, added))
                return;
        if (pns.modified != null)
            if (!handleModifies(pns.modified, added))
                return;

        // add new messages to the currently selected mailbox
        if (added != null && !added.isEmpty()) {
            added.sort();
            boolean debug = ZimbraLog.imap.isDebugEnabled();

            if (!added.numbered.isEmpty()) {
                // if messages have acceptable UIDs, just add 'em
                StringBuilder addlog = debug ? new StringBuilder("  ** adding messages (ntfn):") : null;
                for (ImapMessage i4msg : added.numbered) {
                    selectedFolder.cache(i4msg);
                    if (debug)  addlog.append(' ').append(i4msg.msgId);
                    i4msg.setAdded(true);
                    selectedFolder.dirtyMessage(i4msg);
                }
                if (debug)  ZimbraLog.imap.debug(addlog);
            }
            if (!added.unnumbered.isEmpty()) {
                // 2.3.1.1: "Unique identifiers are assigned in a strictly ascending fashion in
                //           the mailbox; as each message is added to the mailbox it is assigned
                //           a higher UID than the message(s) which were added previously."
                List<Integer> renumber = new ArrayList<Integer>();
                StringBuilder chglog = debug ? new StringBuilder("  ** moved; changing imap uid (ntfn):") : null;
                for (ImapMessage i4msg : added.unnumbered) {
                    renumber.add(i4msg.msgId);
                    if (debug)  chglog.append(' ').append(i4msg.msgId);
                }
                try {
                    if (debug)  ZimbraLog.imap.debug(chglog);
                    // notification will take care of adding to mailbox
                    getMailbox().resetImapUid(getContext(), renumber);
                } catch (ServiceException e) {
                    if (debug)  ZimbraLog.imap.debug("  ** moved; imap uid change failed; msg hidden (ntfn): " + renumber);
                }
            }
        }

        if (isIdle() && mHandler != null)
			try {
				mHandler.sendNotifications(true, true);
			} catch (IOException e) {
                // ImapHandler.dropConnection clears our mHandler and calls SessionCache.clearSession,
                //   which calls Session.doCleanup, which calls Mailbox.removeListener
                ZimbraLog.imap.debug("dropping connection due to IOException during IDLE notification", e);
                mHandler.dropConnection(false);
			}
    }

    private boolean handleDeletes(Map<Integer, Object> deleted) {
        ImapFolder selectedFolder = mHandler.getSelectedFolder();
        boolean selected = selectedFolder != null;
        for (Object obj : deleted.values()) {
            int id = (obj instanceof MailItem ? ((MailItem) obj).getId() : ((Integer) obj).intValue());
            if (Tag.validateId(id)) {
                mTags.uncache(1L << Tag.getIndex(id));
                if (selected)
                    selectedFolder.dirtyTag(id, true);
            } else if (!selected || id <= 0) {
                continue;
            } else if (id == selectedFolder.getId()) {
                // notify client that mailbox is deselected due to delete?
                // RFC 2180 3.3: "The server MAY allow the DELETE/RENAME of a multi-accessed
                //                mailbox, but disconnect all other clients who have the
                //                mailbox accessed by sending a untagged BYE response."
                mHandler.dropConnection(true);
            } else {
                ImapMessage i4msg = selectedFolder.getById(id);
                if (i4msg != null) {
                    selectedFolder.markMessageExpunged(i4msg);
                    ZimbraLog.imap.debug("  ** deleted (ntfn): " + i4msg.msgId);
                }
            }
        }
        return true;
    }

    private boolean handleCreates(Map<Integer, MailItem> created, AddedItems newItems) {
        ImapFolder selectedFolder = mHandler.getSelectedFolder();
        boolean selected = selectedFolder != null;
        for (MailItem item : created.values()) {
            if (item instanceof Tag) {
                cacheTag((Tag) item);
            } else if (!selected || item == null || item.getId() <= 0) {
                continue;
            } else if (!(item instanceof Message || item instanceof Contact)) {
                continue;
            } else if (item.getFolderId() == selectedFolder.getId()) {
                int msgId = item.getId();
                // make sure this message hasn't already been detected in the folder
                if (selectedFolder.getById(msgId) != null)
                    continue;
                ImapMessage i4msg = selectedFolder.getByImapId(item.getImapUid());
                if (i4msg == null)
                    newItems.add(item);
                ZimbraLog.imap.debug("  ** created (ntfn): " + msgId);
            }
        }
        return true;
    }

    private boolean handleModifies(Map<Integer, Change> modified, AddedItems newItems) {
        ImapFolder selectedFolder = mHandler.getSelectedFolder();
        boolean selected = selectedFolder != null;
        boolean virtual = selected && selectedFolder.isVirtual();
        boolean debug = ZimbraLog.imap.isDebugEnabled();
        for (Change chg : modified.values()) {
            if (chg.what instanceof Tag && (chg.why & Change.MODIFIED_NAME) != 0) {
                Tag ltag = (Tag) chg.what;
                mTags.uncache(ltag.getBitmask());
                cacheTag(ltag);
                if (selected)
                    selectedFolder.dirtyTag(ltag.getId());
//            } else if (chg.what instanceof Mailbox && (chg.why & Change.MODIFIED_CONFIG) != 0) {
//                try {
//                    parseConfig(((Mailbox) chg.what).getConfig(getContext(), SN_IMAP));
//                } catch (ServiceException e) { }
            } else if (!selected) {
                continue;
            } else if (chg.what instanceof Folder && ((Folder) chg.what).getId() == selectedFolder.getId()) {
                Folder folder = (Folder) chg.what;
                if ((chg.why & Change.MODIFIED_FLAGS) != 0 && (folder.getFlagBitmask() & Flag.BITMASK_DELETED) != 0) {
                    // notify client that mailbox is deselected due to \Noselect?
                    // RFC 2180 3.3: "The server MAY allow the DELETE/RENAME of a multi-accessed
                    //                mailbox, but disconnect all other clients who have the
                    //                mailbox accessed by sending a untagged BYE response."
                    mHandler.dropConnection(true);
                } else if ((chg.why & (Change.MODIFIED_FOLDER | Change.MODIFIED_NAME)) != 0) {
                    selectedFolder.updatePath(folder);
                    // FIXME: can we change the folder's UIDVALIDITY?
                    //        if not, how do we persist it for the session?
                    // RFC 2180 3.4: "The server MAY allow the RENAME of a multi-accessed mailbox
                    //                by simply changing the name attribute on the mailbox."
                }
            } else if (chg.what instanceof Message || chg.what instanceof Contact) {
                MailItem item = (MailItem) chg.what;
                boolean inFolder = virtual || (item.getFolderId() == selectedFolder.getId());
                if (!inFolder && (chg.why & Change.MODIFIED_FOLDER) == 0)
                    continue;
                ImapMessage i4msg = selectedFolder.getById(item.getId());
                if (i4msg == null) {
                    if (inFolder && !virtual) {
                        newItems.add(item);
                        if (debug)  ZimbraLog.imap.debug("  ** moved (ntfn): " + item.getId());
                    }
                } else if (!inFolder && !virtual) {
                    selectedFolder.markMessageExpunged(i4msg);
                } else if ((chg.why & (Change.MODIFIED_TAGS | Change.MODIFIED_FLAGS | Change.MODIFIED_UNREAD)) != 0) {
                    i4msg.setPermanentFlags(item.getFlagBitmask(), item.getTagBitmask(), selectedFolder);
                } else if ((chg.why & Change.MODIFIED_IMAP_UID) != 0) {
                    // if the IMAP uid changed, need to bump it to the back of the sequence!
                    selectedFolder.markMessageExpunged(i4msg);
                    if (!virtual)
                        newItems.add(item);
                    if (debug)  ZimbraLog.imap.debug("  ** imap uid changed (ntfn): " + item.getId());
                }
            }
        }
        return true;
    }

    @Override
    protected void cleanup() {
        // XXX: is there a synchronization issue here?
        if (mHandler != null) {
            ZimbraLog.imap.debug("dropping connection because Session is closing");
            mHandler.dropConnection(true);
        }
    }
}
