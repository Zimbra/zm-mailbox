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
import java.io.Writer;
import java.text.DateFormat;
import java.util.*;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.im.IMNotification;
import com.zimbra.cs.mailbox.*;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.util.Constants;
import com.zimbra.cs.util.ZimbraLog;

/**
 * @author dkarp
 */
public class ImapSession extends Session {

    /** The various special modes the server can be thrown into in order to
     *  deal with client weirdnesses.  These modes are specified by appending
     *  various suffixes to the USERNAME when logging into the IMAP server; for
     *  instance, the Windows Mobile 5 hack is enabled via the suffix "/wm". */
    static enum EnabledHack { NONE, WM5 };

    static final byte STATE_NOT_AUTHENTICATED = 0;
    static final byte STATE_AUTHENTICATED     = 1;
    static final byte STATE_SELECTED          = 2;
    static final byte STATE_LOGOUT            = 3;

    public static final long IMAP_IDLE_TIMEOUT_MSEC = 30 * Constants.MILLIS_PER_MINUTE;

    private String      mUsername;
    private byte        mState;
    private String      mIdleTag;
    private ImapSessionHandler mHandler;
    private ImapFolder  mSelectedFolder;
    private Map<Object, ImapFlag> mFlags = new LinkedHashMap<Object, ImapFlag>();
    private Map<Object, ImapFlag> mTags = new HashMap<Object, ImapFlag>();
    private boolean     mCheckingSpam;
    private EnabledHack mEnabledHack;

    public ImapSession(String accountId, String contextId) throws ServiceException {
        super(accountId, contextId, SessionCache.SESSION_IMAP);
        getMailbox().beginTrackingImap(getContext());
        mState = STATE_AUTHENTICATED;
        try {
            Provisioning prov = Provisioning.getInstance();
            mCheckingSpam = prov.getConfig().getBooleanAttr(Provisioning.A_zimbraSpamCheckEnabled, false);

            parseConfig(getMailbox().getConfig(getContext(), "imap"));
        } catch (ServiceException e) { }
    }

    public void dumpState(Writer w) {
    	try {
    		StringBuilder s = new StringBuilder(this.toString());
    		s.append("\n\t\tuser=").append(mUsername);
    		s.append("\n\t\tstate=").append(mState);
    		s.append("\n\t\tidleTag=").append(mIdleTag);
    		s.append("\n\t\tselectedFolder=").append(mSelectedFolder.toString());
            s.append("\n\t\tcheckingSpam=").append(mCheckingSpam);
            s.append("\n\t\thacks=").append(mEnabledHack);
    		
    		w.write(s.toString());
    		if (mHandler != null) 
    			mHandler.dumpState(w);
    	} catch(IOException e) { e.printStackTrace(); }
    }

    protected long getSessionIdleLifetime() {
        return IMAP_IDLE_TIMEOUT_MSEC;
    }

    static byte getState(ImapSession s)  { return s == null ? STATE_NOT_AUTHENTICATED : s.mState; }

    void setHandler(ImapSessionHandler handler)  { mHandler = handler; }

    String getUsername()          { return mUsername; }
    void setUsername(String name) { mUsername = name; }

    void enableHack(EnabledHack hack)        { mEnabledHack = hack; }
    boolean isHackEnabled(EnabledHack hack)  { return mEnabledHack == hack; }

    OperationContext getContext() throws ServiceException {
        return new OperationContext(getAccountId());
    }

    boolean isSpamCheckEnabled()  { return mCheckingSpam; }

    private void parseConfig(Metadata config)  { }

    boolean isSelected()  { return mState == STATE_SELECTED; }
    void selectFolder(ImapFolder folder) {
        if (mState != STATE_LOGOUT) {
            mSelectedFolder = folder;
            mState = STATE_SELECTED;
        }
    }
    ImapFolder deselectFolder() {
        ImapFolder i4folder = null;
        if (mState != STATE_LOGOUT) {
            mState = STATE_AUTHENTICATED;
            i4folder = mSelectedFolder;
            mSelectedFolder = null;
        }
        return i4folder;
    }
    void loggedOut()        { mState = STATE_LOGOUT; }
    ImapFolder getFolder()  { return mSelectedFolder; }

    void beginIdle(String tag)  { mIdleTag = tag; }
    String endIdle()            { String tag = mIdleTag;  mIdleTag = null;  return tag; }
    boolean isIdle()            { return mIdleTag != null; }

    static final class ImapFlag {
        String  mName;
        String  mImapName;
        int     mId;
        long    mBitmask;
        boolean mPositive;
        boolean mPermanent;
        boolean mListed;

        static final boolean VISIBLE = true, HIDDEN = false;

        ImapFlag(String name, Tag ltag, boolean positive) {
            mName = ltag.getName();  mImapName  = normalize(name, mId);
            mId   = ltag.getId();    mBitmask   = ltag.getBitmask();
            mPositive = positive;    mPermanent = true;
            mListed = VISIBLE;
        }

        ImapFlag(String name, short bitmask, boolean listed) {
            mName = name;      mImapName  = name.toUpperCase();
            mId   = 0;         mBitmask   = bitmask;
            mPositive = true;  mPermanent = false;
            mListed = listed;
        }

        private String normalize(String name, int id) {
            String imapName = name.toUpperCase().replaceAll("[ *(){%*\\]\\\\]+", "");
            if (name.startsWith("\\"))
                imapName = '\\' + imapName;
            if (!name.equals(""))
                return imapName;
            return ":FLAG" + Tag.getIndex(id);
        }

        public String toString()  { return mImapName; }
    }

    private ImapFlag cache(ImapFlag i4flag) {
        Map<Object, ImapFlag> map = (i4flag.mId <= 0 ? mFlags : mTags);
        map.put(i4flag.mImapName, i4flag);
        Long bitmask = new Long(i4flag.mBitmask);
        if (!map.containsKey(bitmask))
            map.put(bitmask, i4flag);
        return i4flag;
    }
    void cacheFlags(Mailbox mbox) {
        mFlags.clear();
        cache(new ImapFlag("\\Answered", mbox.mReplyFlag,    true));
        cache(new ImapFlag("\\Deleted",  mbox.mDeletedFlag,  true));
        cache(new ImapFlag("\\Draft",    mbox.mDraftFlag,    true));
        cache(new ImapFlag("\\Flagged",  mbox.mFlaggedFlag,  true));
        cache(new ImapFlag("\\Seen",     mbox.mUnreadFlag,   false));
        cache(new ImapFlag("$Forwarded", mbox.mForwardFlag,  true));
        cache(new ImapFlag("$MDNSent",   mbox.mNotifiedFlag, true));
        cache(new ImapFlag("Forwarded",  mbox.mForwardFlag,  true));

        cache(new ImapFlag("\\Recent",     ImapMessage.FLAG_RECENT,       ImapFlag.HIDDEN));
        cache(new ImapFlag("$Junk",        ImapMessage.FLAG_SPAM,         ImapFlag.VISIBLE));
        cache(new ImapFlag("$NotJunk",     ImapMessage.FLAG_NONSPAM,      ImapFlag.VISIBLE));
        cache(new ImapFlag("Junk",         ImapMessage.FLAG_SPAM,         ImapFlag.VISIBLE));
        cache(new ImapFlag("JunkRecorded", ImapMessage.FLAG_JUNKRECORDED, ImapFlag.VISIBLE));
        cache(new ImapFlag("NonJunk",      ImapMessage.FLAG_NONSPAM,      ImapFlag.VISIBLE));
        cache(new ImapFlag("NotJunk",      ImapMessage.FLAG_NONSPAM,      ImapFlag.VISIBLE));
    }
    ImapFlag cacheTag(Tag ltag) {
        return (ltag instanceof Flag ? null : cache(new ImapFlag(ltag.getName(), ltag, true)));
    }
    private void uncacheTag(int tagId) {
        if (!Tag.validateId(tagId))
            return;
        int index = Tag.getIndex(tagId);
        ImapFlag i4flag = mTags.remove(new Long(1L << index));
        if (i4flag != null)
            mTags.remove(i4flag.mImapName);
    }
    ImapFlag getFlagByName(String name) {
        ImapFlag i4flag = mFlags.get(name);
        return (i4flag != null ? i4flag : mTags.get(name));
    }
    ImapFlag getTagByMask(long mask)  { return mTags.get(new Long(mask)); }
    String getRealTagName(String name) {
        ImapFlag i4flag = getFlagByName(name);
        return (i4flag == null ? null : i4flag.mName);
    }
    String getFlagList(boolean permanentOnly) {
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        Map[] flagSets = new Map[] { mFlags, mTags };
        for (int i = 0; i < flagSets.length; i++)
            for (Iterator it = flagSets[i].entrySet().iterator(); it.hasNext(); first = false) {
                Map.Entry entry = (Map.Entry) it.next();
                if (entry.getKey() instanceof String) {
                    ImapFlag i4flag = (ImapFlag) entry.getValue();
                    if (i4flag.mListed && (!permanentOnly || i4flag.mPermanent))
                        sb.append(first ? "" : " ").append(entry.getKey());
                }
            }
        return sb.toString();
    }
    void clearTagCache()  { mTags.clear(); }

    DateFormat getDateFormat() {
        return mHandler.getDateFormat();
    }
    
    DateFormat getZimbraDateFormat() {
        return mHandler.getZimbraFormat();
    }

    public void notifyIM(IMNotification imn) { }
    protected boolean shouldRegisterWithIM() { return false; }

    private static class AddedItems {
        List<ImapMessage> numbered = new ArrayList<ImapMessage>();
        List<ImapMessage> unnumbered = new ArrayList<ImapMessage>();

        boolean isEmpty()  { return numbered.isEmpty() && unnumbered.isEmpty(); }
        void add(MailItem item) {
            (item.getImapUid() > 0 ? numbered : unnumbered).add(new ImapMessage(item));
        }
        void sort()  { Collections.sort(numbered);  Collections.sort(unnumbered); }
    }

    public void notifyPendingChanges(PendingModifications pns) {
        if (!pns.hasNotifications())
            return;

        if (pns.deleted != null)
            if (!handleDeletes(pns.deleted))
                return;
        AddedItems added = (mSelectedFolder == null ? null : new AddedItems());
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
                    mSelectedFolder.cache(i4msg);
                    if (debug)  addlog.append(' ').append(i4msg.msgId);
                    i4msg.setAdded(true);
                    mSelectedFolder.dirtyMessage(i4msg);
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
                mHandler.dropConnection(false);
			}
    }

    private boolean handleDeletes(Map<Integer, Object> deleted) {
        boolean selected = mSelectedFolder != null;
        for (Object obj : deleted.values()) {
            int id = (obj instanceof MailItem ? ((MailItem) obj).getId() : ((Integer) obj).intValue());
            if (Tag.validateId(id)) {
                uncacheTag(id);
                if (selected)
                    mSelectedFolder.dirtyTag(id, true);
            } else if (!selected || id <= 0) {
                continue;
            } else if (id == mSelectedFolder.getId()) {
                // notify client that mailbox is deselected due to delete?
                // RFC 2180 3.3: "The server MAY allow the DELETE/RENAME of a multi-accessed
                //                mailbox, but disconnect all other clients who have the
                //                mailbox accessed by sending a untagged BYE response."
                mState = STATE_AUTHENTICATED;
                mSelectedFolder = null;
                selected = false;
            } else {
                ImapMessage i4msg = mSelectedFolder.getById(id);
                if (i4msg != null) {
                    i4msg.setExpunged(true);
                    ZimbraLog.imap.debug("  ** deleted (ntfn): " + i4msg.msgId);
                }
            }
        }
        return true;
    }

    private boolean handleCreates(Map<Integer, MailItem> created, AddedItems newItems) {
        boolean selected = mSelectedFolder != null;
        for (MailItem item : created.values()) {
            if (item instanceof Tag)
                cacheTag((Tag) item);
            else if (!selected || item == null || item.getId() <= 0)
                continue;
            else if (!(item instanceof Message || item instanceof Contact))
                continue;
            else if (item.getFolderId() == mSelectedFolder.getId()) {
                int msgId = item.getId();
                // make sure this message hasn't already been detected in the folder
                if (mSelectedFolder.getById(msgId) != null)
                    continue;
                ImapMessage i4msg = mSelectedFolder.getByImapId(item.getImapUid());
                if (i4msg != null && i4msg.isGhost())
                    mSelectedFolder.unghost(i4msg, item.getId());
                else
                    newItems.add(item);
                ZimbraLog.imap.debug("  ** created (ntfn): " + msgId);
            }
        }
        return true;
    }

    private boolean handleModifies(Map<Integer, Change> modified, AddedItems newItems) {
        boolean selected = mSelectedFolder != null;
        boolean virtual = selected && mSelectedFolder.isVirtual();
        boolean debug = ZimbraLog.imap.isDebugEnabled();
        for (Change chg : modified.values()) {
            if (chg.what instanceof Tag && (chg.why & Change.MODIFIED_NAME) != 0) {
                Tag ltag = (Tag) chg.what;
                uncacheTag(ltag.getId());
                cacheTag(ltag);
                if (selected)
                    mSelectedFolder.dirtyTag(ltag.getId());
//            } else if (chg.what instanceof Mailbox && (chg.why & Change.MODIFIED_CONFIG) != 0) {
//                try {
//                    parseConfig(((Mailbox) chg.what).getConfig(getContext(), "imap"));
//                } catch (ServiceException e) { }
            } else if (!selected) {
                continue;
            } else if (chg.what instanceof Folder && ((Folder) chg.what).getId() == mSelectedFolder.getId()) {
                Folder folder = (Folder) chg.what;
                if ((chg.why & Change.MODIFIED_FLAGS) != 0 && (folder.getFlagBitmask() & Flag.BITMASK_DELETED) != 0) {
                    // notify client that mailbox is deselected due to \Noselect?
                    // RFC 2180 3.3: "The server MAY allow the DELETE/RENAME of a multi-accessed
                    //                mailbox, but disconnect all other clients who have the
                    //                mailbox accessed by sending a untagged BYE response."
                    mState = STATE_AUTHENTICATED;
                    mSelectedFolder = null;
                    selected = false;
                } else if ((chg.why & (Change.MODIFIED_FOLDER | Change.MODIFIED_NAME)) != 0) {
                    mSelectedFolder.updatePath(folder);
                    // FIXME: can we change the folder's UIDVALIDITY?
                    //        if not, how do we persist it for the session?
                    // RFC 2180 3.4: "The server MAY allow the RENAME of a multi-accessed mailbox
                    //                by simply changing the name attribute on the mailbox."
                }
            } else if (chg.what instanceof Message) {
                Message msg = (Message) chg.what;
                boolean inFolder = virtual || (msg.getFolderId() == mSelectedFolder.getId());
                if (!inFolder && (chg.why & Change.MODIFIED_FOLDER) == 0)
                    continue;
                ImapMessage i4msg = mSelectedFolder.getById(msg.getId());
                if (i4msg == null) {
                    if (inFolder && !virtual) {
                        newItems.add(msg);
                        if (debug)  ZimbraLog.imap.debug("  ** moved (ntfn): " + msg.getId());
                    }
                } else if (!inFolder && !virtual) {
                    if (!i4msg.isGhost())
                        i4msg.setExpunged(true);
                } else if ((chg.why & (Change.MODIFIED_TAGS | Change.MODIFIED_FLAGS | Change.MODIFIED_UNREAD)) != 0)
                    i4msg.setPermanentFlags(msg.getFlagBitmask(), msg.getTagBitmask());
                else if ((chg.why & Change.MODIFIED_IMAP_UID) != 0) {
                    // if the IMAP uid changed, need to bump it to the back of the sequence!
                    i4msg.setExpunged(true);
                    if (!virtual)
                        newItems.add(msg);
                    if (debug)  ZimbraLog.imap.debug("  ** imap uid changed (ntfn): " + msg.getId());
                }
            }
        }
        return true;
    }


    protected void cleanup() {
        // XXX: is there a synchronization issue here?
        if (mHandler != null)
            mHandler.dropConnection(true);
    }
}
