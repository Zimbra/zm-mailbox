/*
 * Created on Apr 30, 2005
 */
package com.liquidsys.coco.imap;

import java.util.*;

import com.liquidsys.coco.index.LiquidHit;
import com.liquidsys.coco.index.LiquidQueryResults;
import com.liquidsys.coco.index.MailboxIndex;
import com.liquidsys.coco.mailbox.*;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.util.LiquidLog;


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

    ImapFolder(String name, boolean select, Mailbox mailbox) throws ServiceException {
        boolean debug = LiquidLog.imap.isDebugEnabled();
        if (debug)  LiquidLog.imap.debug("  ** loading folder: " + name);

        synchronized (mailbox) {
            Folder folder = mailbox.getFolderByPath(name);
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
                msgs = mailbox.getItemList(MailItem.TYPE_MESSAGE, folder.getId());
            else
                msgs = loadVirtualFolder((SearchFolder) folder, mailbox);
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
            if (debug)  LiquidLog.imap.debug(added);

            mLastSize = mSequence.size();
        }
    }
    private List loadVirtualFolder(SearchFolder search, Mailbox mailbox) throws ServiceException {
        List msgs = new ArrayList();
        try {
            LiquidQueryResults lqr = mailbox.search(mQuery, ImapHandler.MESSAGE_TYPES, MailboxIndex.SEARCH_ORDER_DATE_ASC);
            int i = 0, hitIds[] = new int[100];
            Arrays.fill(hitIds, Mailbox.ID_AUTO_INCREMENT);
            try {
                for (LiquidHit hit = lqr.getFirstHit(); hit != null || i > 0; ) {
                    if (hit == null || i == 100) {
                        msgs.addAll(Arrays.asList(mailbox.getItemById(hitIds, MailItem.TYPE_MESSAGE)).subList(0, i));
                        Arrays.fill(hitIds, Mailbox.ID_AUTO_INCREMENT);
                        i = 0;
                    }
                    if (hit != null) {
                        hitIds[i++] = hit.getItemId();
                        hit = lqr.getNext();
                    }
                }
            } finally {
                lqr.doneWithSearchResults();
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
        long mask = 1L << (id - MailItem.TAG_ID_OFFSET);
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
                            LiquidLog.imap.debug("  ** deleting: " + i4msg.id);
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
        boolean debug = LiquidLog.imap.isDebugEnabled();
        List removed = new ArrayList();
        boolean trimmed = false;
        int seq = 1;
        if (debug)  LiquidLog.imap.debug("  ** iterating (collapseExpunged)");
        for (ListIterator lit = mSequence.listIterator(); lit.hasNext(); seq++) {
            ImapMessage i4msg = (ImapMessage) lit.next();
            if (i4msg.expunged) {
                if (debug)  LiquidLog.imap.debug("  ** removing: " + i4msg.id);
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