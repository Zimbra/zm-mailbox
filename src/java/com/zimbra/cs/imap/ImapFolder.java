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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
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

import java.io.UnsupportedEncodingException;
import java.util.*;

import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.*;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ZimbraLog;


class ImapFolder {
    private int       mFolderId;
    private String    mPath;
    private boolean   mWritable;
    private String    mQuery;
    private int       mUIDValidityValue;
    private ArrayList mSequence    = new ArrayList();
    private HashMap   mUIDs        = new HashMap();
    private int       mHighwaterUID = -1;
    private int       mFirstUnread  = -1;
    private int       mLastSize     = 0;
    private HashSet   mDirtyMessages = new HashSet();
    private boolean   mNotificationsSuspended;

    ImapFolder(String name, boolean select, Mailbox mailbox, OperationContext octxt) throws ServiceException {
        boolean debug = ZimbraLog.imap.isDebugEnabled();
        if (debug)  ZimbraLog.imap.debug("  ** loading folder: " + name);

        synchronized (mailbox) {
            Folder folder = mailbox.getFolderByPath(octxt, name);
            if (!isFolderSelectable(folder))
                throw ServiceException.PERM_DENIED("cannot select folder: /" + name);
            mPath = name;
            mFolderId = folder.getId();
            if (folder instanceof SearchFolder) {
                String types = ((SearchFolder) folder).getReturnTypes().toLowerCase();
                if (types.indexOf("conversation") != -1 || types.indexOf("message") != -1)
                    mQuery = ((SearchFolder) folder).getQuery();
                else
                    mQuery = "item:none";
            }
            mWritable = select && isFolderWritable(folder);
            mUIDValidityValue = getUIDValidity(folder);

            List msgs;
            if (!isVirtual())
                msgs = mailbox.getItemList(octxt, MailItem.TYPE_MESSAGE, folder.getId());
            else
                msgs = loadVirtualFolder((SearchFolder) folder, mailbox, octxt);
            // FIXME: need to check messages for flag "IMAP dirty" and assign new IMAP IDs if necessary
            Collections.sort(msgs, new Message.SortImapUID());
            StringBuffer added = debug ? new StringBuffer("  ** added: ") : null;
            for (Iterator it = msgs.iterator(); it.hasNext(); ) {
                Message msg = (Message) it.next();
                ImapMessage i4msg = cache(msg);
                if (mFirstUnread == -1 && msg.isUnread())
                    mFirstUnread = i4msg.seq;
                if (debug)  added.append(' ').append(i4msg.id);
            }
            if (debug)  ZimbraLog.imap.debug(added);

            mLastSize = mSequence.size();
        }
    }
    /** Fetches the messages contained within a search folder.  When a search
     *  folder is IMAP-visible, it appears in folder listings, is SELECTable
     *  READ-ONLY, and appears to have all matching messages as its contents.
     *  If it is not visible, it will be completely hidden from all IMAP
     *  commands.
     * 
     * @param search   The search folder being exposed.
     * @param mailbox  The {@link Mailbox} belonging to the user. */
    private List loadVirtualFolder(SearchFolder search, Mailbox mailbox, OperationContext octxt) throws ServiceException {
        List msgs = new ArrayList();
        try {
            ZimbraQueryResults zqr = mailbox.search(octxt, mQuery, ImapHandler.MESSAGE_TYPES, MailboxIndex.SEARCH_ORDER_DATE_ASC, 1000);
            int i = 0, hitIds[] = new int[100];
            Arrays.fill(hitIds, Mailbox.ID_AUTO_INCREMENT);
            try {
                for (ZimbraHit hit = zqr.getFirstHit(); hit != null || i > 0; ) {
                    if (hit == null || i == 100) {
                        msgs.addAll(Arrays.asList(mailbox.getItemById(octxt, hitIds, MailItem.TYPE_MESSAGE)).subList(0, i));
                        Arrays.fill(hitIds, Mailbox.ID_AUTO_INCREMENT);
                        i = 0;
                    }
                    if (hit != null) {
                        hitIds[i++] = hit.getItemId();
                        hit = zqr.getNext();
                    }
                }
            } finally {
                zqr.doneWithSearchResults();
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw ServiceException.FAILURE("failure opening searchfolder", e);
        }
        return msgs;
    }

    int getId()           { return mFolderId; }
    int getSize()         { return mSequence.size(); }
    String getQuery()     { return mQuery == null ? "" : mQuery; }
    int getUIDValidity()  { return mUIDValidityValue; }
    int getFirstUnread()  { return mFirstUnread; }
    int getHighwaterUID() { return mHighwaterUID; }
    boolean isVirtual()   { return mQuery != null; }
    boolean isWritable()  { return mWritable; }

    static boolean isFolderWritable(Folder folder) {
        return isFolderSelectable(folder) && !(folder instanceof SearchFolder);
    }
    static boolean isFolderSelectable(Folder folder) {
        return isFolderVisible(folder) && !folder.isTagged(folder.getMailbox().mDeletedFlag);
    }
    static boolean isFolderVisible(Folder folder) {
        return folder != null &&
               folder.getId() != Mailbox.ID_FOLDER_CONTACTS &&
               folder.getId() != Mailbox.ID_FOLDER_CALENDAR &&
               folder.getId() != Mailbox.ID_FOLDER_USER_ROOT &&
               (!(folder instanceof SearchFolder) || ((SearchFolder) folder).isImapVisible());
    }

    static boolean isPathCreatable(String path) {
        return path != null &&
               !path.toLowerCase().matches("/\\s*contacts\\s*(/.*)?") &&
               !path.toLowerCase().matches("/\\s*calendar\\s*(/.*)?");
    }

    static String encodeFolder(String folderName) {
        // make sure that the Inbox is called "INBOX", regarless of how we capitalize it
        if (folderName.length() == 5 && folderName.equalsIgnoreCase("INBOX"))
            folderName = "INBOX";
        else if (folderName.length() > 5 && folderName.substring(0, 6).equalsIgnoreCase("INBOX/"))
            folderName = "INBOX" + folderName.substring(5);
        try {
            folderName = '"' + new String(folderName.getBytes("imap-utf-7"), "US-ASCII") + '"';
        } catch (UnsupportedEncodingException e) {
            folderName = '"' + folderName + '"';
        }
        return folderName.replaceAll("\\\\", "\\\\\\\\");
    }

    String getPath()                { return mPath; }
    String getQuotedPath()          { return '"' + mPath + '"'; }
    void updatePath(Folder folder)  { mPath = folder.getPath(); }

    static int getUIDValidity(Folder folder) { return Math.max(folder.getModifiedSequence(), 1); }

    ImapMessage getById(int id)        { if (id <= 0) return null;   return checkRemoved((ImapMessage) mUIDs.get(new Integer(-id))); }
    ImapMessage getByImapId(int uid)   { if (uid <= 0) return null;  return checkRemoved((ImapMessage) mUIDs.get(new Integer(uid))); }
    ImapMessage getBySequence(int seq) { return checkRemoved(seq > 0 && seq <= mSequence.size() ? (ImapMessage) mSequence.get(seq - 1) : null); }
    ImapMessage getLastMessage()       { return getBySequence(mSequence.size()); }
    private ImapMessage checkRemoved(ImapMessage i4msg)  { return (i4msg == null || i4msg.expunged ? null : i4msg); }

    ImapMessage cache(Message msg) {
        ImapMessage i4msg = new ImapMessage(msg, this);
        if (i4msg.uid > mHighwaterUID)
            mHighwaterUID = i4msg.uid;
        mSequence.add(i4msg);
        setIndex(i4msg, mSequence.size());
        return i4msg;
    }
    void setIndex(ImapMessage i4msg, int position) {
        i4msg.seq = position;
        mUIDs.put(new Integer(i4msg.uid), i4msg);
        mUIDs.put(new Integer(-i4msg.id), i4msg);
    }
    void uncache(ImapMessage i4msg) {
        mUIDs.remove(new Integer(i4msg.uid));
        mUIDs.remove(new Integer(-i4msg.id));
        mDirtyMessages.remove(i4msg);
    }

    void disableNotifications()     { mNotificationsSuspended = true; }
    void enableNotifications()      { mNotificationsSuspended = false; }
    boolean notificationsEnabled()  { return !mNotificationsSuspended; }

    boolean checkpointSize()  { int last = mLastSize;  return last != (mLastSize = getSize()); }

    void dirtyMessage(ImapMessage i4msg) {
        if (i4msg.getParent() == this && i4msg.seq <= getSize() && i4msg == getBySequence(i4msg.seq))
            mDirtyMessages.add(i4msg);
    }
    void undirtyMessage(ImapMessage i4msg) {
        if (mDirtyMessages.remove(i4msg))
            i4msg.added = false;
    }
    Iterator dirtyIterator()  { return mDirtyMessages.iterator(); }
    void clearDirty()         { mDirtyMessages.clear(); }

    void dirtyTag(int id) {
        dirtyTag(id, false);
    }
    void dirtyTag(int id, boolean removeTag) {
        long mask = 1L << Tag.getIndex(id);
        for (int i = 0; i < mSequence.size(); i++) {
            ImapMessage i4msg = (ImapMessage) mSequence.get(i);
            if (i4msg != null && (i4msg.tags & mask) != 0) {
                mDirtyMessages.add(i4msg);
                if (removeTag)
                	i4msg.tags &= ~mask;
            }
        }
    }

    private static class NullableComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            if (o1 == null)       return o2 == null ? 0 : -1;
            else if (o2 == null)  return 1;
            int seq1 = ((ImapMessage) o1).seq;
            int seq2 = ((ImapMessage) o2).seq;
            return (seq1 < seq2 ? -1 : (seq1 == seq2 ? 0 : 1));
        }
    }

    Set getFlaggedMessages(ImapSession.ImapFlag i4flag) {
        if (i4flag == null || mSequence.size() == 0)
            return Collections.EMPTY_SET;
        TreeSet result = new TreeSet(new NullableComparator());
        for (Iterator it = mSequence.iterator(); it.hasNext(); ) {
            ImapMessage i4msg = (ImapMessage) it.next();
            if (i4msg != null && (i4msg.sflags & i4flag.mBitmask) != 0)
                result.add(i4msg);
        }
        return result;
    }

    Set getSubsequence(String subseqStr, boolean byUID) {
        if (subseqStr == null || subseqStr.trim().equals("") || mSequence.size() == 0)
            return Collections.EMPTY_SET;
        ImapMessage i4msg = getLastMessage();
        int lastID = (i4msg == null ? (byUID ? Integer.MAX_VALUE : mSequence.size()) : (byUID ? i4msg.uid : i4msg.seq));

        TreeSet result = new TreeSet(new NullableComparator());
        String[] subsets = subseqStr.split(",");
        for (int i = 0; i < subsets.length; i++)
            if (subsets[i].indexOf(':') == -1) {
                // single message -- get it and add it (may be null)
                int id = (subsets[i].equals("*") ? lastID : Integer.parseInt(subsets[i]));
                result.add(byUID ? getByImapId(id) : getBySequence(id));
            } else {
                // colon-delimited range -- get them and add them (may be null)
                String[] range = subsets[i].split(":", 2);
                int lower = (range[0].equals("*") ? lastID : Integer.parseInt(range[0]));
                int upper = (range[1].equals("*") ? lastID : Integer.parseInt(range[1]));
                if (lower > upper)  { int tmp = upper; upper = lower; lower = tmp; }
                if (!byUID) {
                    upper = Math.min(lastID, upper);
                    for (int seq = Math.max(0, lower); seq <= upper; seq++)
                        result.add(getBySequence(seq));
                } else
                    for (int seq = 0; seq < mSequence.size(); seq++)
                        if ((i4msg = getBySequence(seq + 1)) != null && i4msg.uid >= lower && i4msg.uid <= upper)
                            result.add(i4msg);
                        else if (i4msg != null && i4msg.uid > upper)
                            break;
            }

        return result;
    }
    static String encodeSubsequence(List items) {
        if (items == null || items.size() == 0)
            return "";
        StringBuffer sb = new StringBuffer();
        int start = -1, last = -1;
        boolean done;
        Iterator it = items.iterator();
        do {
            done = !it.hasNext();
            int next = done ? -1 : ((Integer) it.next()).intValue();
            if (last == -1)
                last = start = next;
            else if (done || next != last + 1) {
                if (sb.length() > 0)
                    sb.append(',');
                sb.append(start);
                if (start != last)
                    sb.append(':').append(last);
                last = start = next;
            } else
                last = next;
        } while (!done);
        return sb.toString();
    }
    
    void expungeMessages(Mailbox mailbox, String sequenceSet) throws ServiceException {
        Set i4set = (sequenceSet == null ? null : getSubsequence(sequenceSet, true));
        synchronized (mailbox) {
            for (Iterator it = mSequence.iterator(); it.hasNext(); ) {
                ImapMessage i4msg = (ImapMessage) it.next();
                if (i4msg != null && !i4msg.expunged && (i4msg.flags & Flag.FLAG_DELETED) > 0)
                    if (i4set == null || i4set.contains(i4msg)) {
                        // message tagged for deletion -- delete now
                        // FIXME: should handle moves separately
                        // FIXME: it'd be nice to have a bulk-delete Mailbox operation
                        try {
                            ZimbraLog.imap.debug("  ** deleting: " + i4msg.id);
                            mailbox.delete(null, i4msg.id, MailItem.TYPE_MESSAGE);
                        } catch (ServiceException e) {
                            if (!(e instanceof MailServiceException.NoSuchItemException))
                                throw e;
                        }
                    }
            }
        }
    }
    List collapseExpunged() {
        // FIXME: need synchronization
        boolean debug = ZimbraLog.imap.isDebugEnabled();
        List removed = new ArrayList();
        boolean trimmed = false;
        int seq = 1;
        if (debug)  ZimbraLog.imap.debug("  ** iterating (collapseExpunged)");
        for (ListIterator lit = mSequence.listIterator(); lit.hasNext(); seq++) {
            ImapMessage i4msg = (ImapMessage) lit.next();
            if (i4msg.expunged) {
                if (debug)  ZimbraLog.imap.debug("  ** removing: " + i4msg.id);
                // uncache() removes pointers to the message from mUIDs;
                //   if the message appears again in mSequence, it *must* be later
                //   and the subsequent call to setIndex() will correctly set the mUIDs mapping
                uncache(i4msg);
                lit.remove();
                removed.add(new Integer(seq--));
                trimmed = true;
            } else if (trimmed)
                setIndex(i4msg, seq);
        }
        return removed;
    }
}