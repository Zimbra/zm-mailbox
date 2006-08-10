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
 * Created on Aug 12, 2004
 */
package com.zimbra.cs.mailbox;

import java.io.IOException;
import java.util.*;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.redolog.op.IndexItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.util.ZimbraLog;

/**
 * @author dkarp
 */
public abstract class MailItem implements Comparable {

    /** Item is a standard {@link Folder}. */
    public static final byte TYPE_FOLDER       = 1;
    /** Item is a saved search {@link SearchFolder}. */
    public static final byte TYPE_SEARCHFOLDER = 2;
    /** Item is a user-created {@link Tag}. */
    public static final byte TYPE_TAG          = 3;
    /** Item is a real, persisted {@link Conversation}. */
    public static final byte TYPE_CONVERSATION = 4;
    /** Item is a mail {@link Message}. */
    public static final byte TYPE_MESSAGE      = 5;
    /** Item is a {@link Contact}. */
    public static final byte TYPE_CONTACT      = 6;
    // /** Item is a {@link InviteMessage} with a <code>text/calendar</code> MIME part. */
    // public static final byte TYPE_INVITE       = 7;   // SKIP 7 FOR NOW!
    /** Item is a bare {@link Document}. */
    public static final byte TYPE_DOCUMENT     = 8;
    /** Item is a {@link Note}. */
    public static final byte TYPE_NOTE         = 9;
    /** Item is a memory-only system {@link Flag}. */
    public static final byte TYPE_FLAG         = 10;
    /** Item is a calendar {@link Appointment}. */
    public static final byte TYPE_APPOINTMENT  = 11;
    /** Item is a memory-only, 1-message {@link VirtualConversation}. */
    public static final byte TYPE_VIRTUAL_CONVERSATION = 12;
    /** Item is a {@link Mountpoint} pointing to a {@link Folder} or
     *  {@link Tag}, possibly in another user's {@link Mailbox}. */
    public static final byte TYPE_MOUNTPOINT   = 13;
    /** Item is a {@link WikiItem} */
    public static final byte TYPE_WIKI         = 14;

    static final byte TYPE_MAX = TYPE_WIKI;
    public static final byte TYPE_UNKNOWN = -1;

    private static String[] TYPE_NAMES = {
        null,
        "folder",
        "search folder",
        "tag",
        "conversation",
        "message",
        "contact",
        "invite",
        "document",
        "note",
        "flag",
        "appointment",
        "virtual conversation",
        "remote folder",
        "wiki"
    };

    /** Throws {@link ServiceException} <code>mail.INVALID_TYPE</code> if the
     *  specified internal Zimbra item type is not supported.  At present, all
     *  types from 1 to {@link #TYPE_MAX} <b>except 7</b> are supported. */
    static byte validateType(byte type) throws ServiceException {
        if (type <= 0 || type > TYPE_MAX || type == 7)
            throw MailServiceException.INVALID_TYPE(type);
        return type;
    }

    /** Returns the human-readable name (e.g. <code>"tag"</code>) for the
     *  item's type.  Returns <code>null</code> if parameter is null. */
    public static String getNameForType(MailItem item) {
        return getNameForType(item == null ? TYPE_UNKNOWN : item.getType());
    }

    /** Returns the human-readable name (e.g. <code>"tag"</code>) for the
     *  specified item type.  Returns <code>null</code> for unknown types. */
    public static String getNameForType(byte type) {
        return (type <= 0 || type > TYPE_MAX ? null : TYPE_NAMES[type]);
    }

    /** Returns the internal Zimbra item type (e.g. {@link #TYPE_TAG}) for
     *  the specified human-readable type name (e.g. <code>"tag"</code>). */
    public static byte getTypeForName(String name) {
        if (name != null && !name.trim().equals(""))
            for (byte i = 1; i < TYPE_NAMES.length; i++)
                if (name.equals(TYPE_NAMES[i]))
                    return i;
        return TYPE_UNKNOWN;
    }

    public static final int FLAG_UNCHANGED = 0x80000000;
    public static final long TAG_UNCHANGED = (1L << 31);

    public static final int TAG_ID_OFFSET  = 64;
    public static final int MAX_FLAG_COUNT = 31;
    public static final int MAX_TAG_COUNT  = 63;

    public static final byte DEFAULT_COLOR = 0;

    public static final class UnderlyingData {
        public int    id;
        public byte   type;
        public int    parentId = -1;
        public int    folderId = -1;
        public int    indexId  = -1;
        public int    imapId   = -1;
        public short  volumeId = -1;
        public String blobDigest;
        public int    date;
        public int    size;
        public int    flags;
        public long   tags;
        public String sender;
        public String subject;
        public String metadata;
        public int    modMetadata;
        public int    dateChanged;
        public int    modContent;

        public String inheritedTags;
        public List<Integer> children;
        public int    unreadCount;

        public boolean isUnread() {
            return (unreadCount > 0);
        }

        public UnderlyingData duplicate(int newId, int newFolder, short newVolume) {
            UnderlyingData data = new UnderlyingData();
            data.id          = newId;
            data.type        = this.type;
            data.parentId    = this.parentId;
            data.folderId    = newFolder;
            data.indexId     = this.indexId;
            data.imapId      = this.imapId <= 0 ? this.imapId : newId;
            data.volumeId    = newVolume;
            data.blobDigest  = this.blobDigest;
            data.date        = this.date;
            data.size        = this.size;
            data.flags       = this.flags;
            data.tags        = this.tags;
            data.subject     = this.subject;
            data.unreadCount = this.unreadCount;
            return data;
        }

        void metadataChanged(Mailbox mbox) throws ServiceException {
            modMetadata = mbox.getOperationChangeID();
            dateChanged = mbox.getOperationTimestamp();
        }
        void contentChanged(Mailbox mbox) throws ServiceException {
            metadataChanged(mbox);
            modContent = modMetadata;
        }
    }

    public static final class TargetConstraint {
        public static final short INCLUDE_TRASH  = 0x01;
        public static final short INCLUDE_SPAM   = 0x02;
        public static final short INCLUDE_SENT   = 0x04;
        public static final short INCLUDE_OTHERS = 0x08;
        public static final short INCLUDE_QUERY  = 0x10;
        private static final short ALL_LOCATIONS = INCLUDE_TRASH | INCLUDE_SPAM | INCLUDE_SENT | INCLUDE_OTHERS;

        private static final char ENC_TRASH = 't';
        private static final char ENC_SPAM  = 'j';
        private static final char ENC_SENT  = 's';
        private static final char ENC_OTHER = 'o';
        private static final char ENC_QUERY = 'q';

        private short  inclusions;
        private String query;

        private Mailbox mailbox;
        private int     sentFolder = -1;

        public TargetConstraint(Mailbox mbox, short include)       { this(mbox, include, null); }
        public TargetConstraint(Mailbox mbox, String includeQuery) { this(mbox, INCLUDE_QUERY, includeQuery); }
        public TargetConstraint(Mailbox mbox, short include, String includeQuery) {
            mailbox = mbox;
            if (includeQuery == null || includeQuery.trim().length() == 0)
                inclusions = (short) (include & ~INCLUDE_QUERY);
            else {
                inclusions = (short) (include | INCLUDE_QUERY);
                query = includeQuery;
            }
        }

        public static TargetConstraint parseConstraint(Mailbox mbox, String encoded) throws ServiceException {
            if (encoded == null)
                return null;
            boolean invert = false;
            short inclusions = 0;
            String query = null;
            loop: for (int i = 0; i < encoded.length(); i++)
                switch (encoded.charAt(i)) {
                    case ENC_TRASH:  inclusions |= INCLUDE_TRASH;       break;
                    case ENC_SPAM:   inclusions |= INCLUDE_SPAM;        break;
                    case ENC_SENT:   inclusions |= INCLUDE_SENT;        break;
                    case ENC_OTHER:  inclusions |= INCLUDE_OTHERS;      break;
                    case ENC_QUERY:  inclusions |= INCLUDE_QUERY;
                                     query = encoded.substring(i + 1);  break loop;
                    case '-':  if (i == 0 && encoded.length() > 1)  { invert = true;  break; }
                        // fall through...
                    default:  throw ServiceException.INVALID_REQUEST("invalid encoded constraint: " + encoded, null);
                }
            if (invert)
                inclusions ^= ALL_LOCATIONS;
            return new TargetConstraint(mbox, inclusions, query);
        }
        public String toString() {
            if (inclusions == 0)
                return "";
            StringBuilder sb = new StringBuilder();
            if ((inclusions & INCLUDE_TRASH) != 0)   sb.append(ENC_TRASH);
            if ((inclusions & INCLUDE_SPAM) != 0)    sb.append(ENC_SPAM);
            if ((inclusions & INCLUDE_SENT) != 0)    sb.append(ENC_SENT);
            if ((inclusions & INCLUDE_OTHERS) != 0)  sb.append(ENC_OTHER);
            if ((inclusions & INCLUDE_QUERY) != 0)   sb.append(ENC_QUERY).append(query);
            return sb.toString();
        }

        static boolean checkItem(TargetConstraint tcon, MailItem item) throws ServiceException {
            return (tcon == null ? true : tcon.checkItem(item));
        }
        private boolean checkItem(MailItem item) throws ServiceException {
            // FIXME: doesn't support EXCLUDE_QUERY
            if ((inclusions & ALL_LOCATIONS) == 0)
                return false;
            if ((inclusions & INCLUDE_TRASH) != 0 && item.inTrash())
                return true;
            if ((inclusions & INCLUDE_SPAM) != 0 && item.inSpam())
                return true;
            if ((inclusions & INCLUDE_SENT) != 0 && inSent(item))
                return true;
            if ((inclusions & INCLUDE_OTHERS) != 0 && !item.inTrash() && !item.inSpam() && !inSent(item))
                return true;
            return false;
        }
        /** Returns whether an item is in the user's sent folder.  Returns
         *  <code>false</code> if the user has set their sent folder to be
         *  any folder other than the default "/Sent" folder, folder 5.<p>
         *  
         *  The reason we don't just compare the item's folder against the
         *  user's configured sent folder is that when the user sets their
         *  sent folder to be "/Inbox", *all* Inbox messages will be skipped
         *  when the "sent" folder is excluded via tcon, which is not what
         *  we want.  See bug 3972 for details. */
        private boolean inSent(MailItem item) {
            // only count as "in sent" if the item's in the real "/Sent" folder
            if (item.getFolderId() != Mailbox.ID_FOLDER_SENT)
                return false;
            if (sentFolder == -1) {
                sentFolder = Mailbox.ID_FOLDER_SENT;
                try {
                    String sent = mailbox.getAccount().getAttr(Provisioning.A_zimbraPrefSentMailFolder, null);
                    if (sent != null)
                        sentFolder = mailbox.getFolderByPath(null, sent).getId();
                } catch (ServiceException e) { }
            }
            // only count as "in sent" if the user's sent folder is 5 and
            //   the item's in there
            return sentFolder == Mailbox.ID_FOLDER_SENT && sentFolder == item.getFolderId();
            // return sentFolder == item.getFolderId();
        }
    }

    protected int            mId;
    protected byte           mColor;
    protected UnderlyingData mData;
    protected Mailbox        mMailbox;
    protected MailboxBlob    mBlob;

    MailItem(Mailbox mbox, UnderlyingData data) throws ServiceException {
        if (data == null)
            throw new IllegalArgumentException();
        Flag.validateFlags(data.flags);
        mId      = data.id;
        mData    = data;
        mMailbox = mbox;
        if (mData.children != null && !canHaveChildren())
            mData.children = null;
        decodeMetadata(mData.metadata);
        mData.metadata = null;
        // store the item in the mailbox's cache
        mbox.cache(this);
    }

    /** Returns the item's ID.  IDs are unique within a {@link Mailbox} and
     *  are assigned in monotonically-increasing (though not necessarily
     *  gap-free) order. */
    public int getId() {
        return mData.id;
    }

    /** Returns the item's type (e.g. {@link #TYPE_MESSAGE}). */
    public byte getType() {
        return mData.type;
    }

    /** Returns the numeric ID of the {@link Mailbox} this item belongs to. */
    public int getMailboxId() {
        return mMailbox.getId();
    }

    /** Returns the {@link Mailbox} this item belongs to. */
    public Mailbox getMailbox() {
        return mMailbox;
    }

    /** Returns the {@link Account} this item's Mailbox belongs to. */
    public Account getAccount() throws ServiceException {
        return mMailbox.getAccount();
    }

    /** Returns the item's color.  If not specified, defaults to
     *  {@link #DEFAULT_COLOR}.  No "color inheritance" (e.g. from the
     *  item's folder or tags) is performed. */
    public byte getColor() {
        return mColor;
    }

    /** Returns the ID of the item's parent.  Not all items have parents;
     *  some that do include {@link Message} (parent is {@link Conversation})
     *  and {@link Folder} (parent is Folder). */
    public int getParentId() {
        return mData.parentId;
    }

    /** Returns the ID of the {@link Folder} the item lives in.  All items
     *  must have a non-<code>null</code> folder. */
    public int getFolderId() {
        return mData.folderId;
    }

    /** Returns the path to the MailItem.
     */
    public String getPath() throws ServiceException {
        Folder f = getMailbox().getFolderById(getFolderId());
        StringBuilder path = new StringBuilder();
        path.append(f.getPath());
        if (path.charAt(path.length() - 1) != '/')
            path.append('/');
        path.append(getSubject());
        return path.toString();
    }
    
    /** Returns the ID the item is referenced by in the index.  Returns -1
     *  for non-indexed items.  For indexed items, the "index ID" will be the
     *  same as the item ID unless the item is a copy of another item; in that
     *  case, the "index ID" is the same as the original item's "index ID". */
    public int getIndexId() {
        return mData.indexId;
    }

    /** Returns the UID the item is referenced by in the IMAP server.  Returns
     *  <code>0</code> for items that require renumbering because of moves.
     *  The "IMAP UID" will be the same as the item ID unless the item has
     *  been moved after the mailbox owner's first IMAP session. */
    public int getImapUid() {
        return mData.imapId;
    }

    /** Returns the ID of the {@link Volume} the item's blob is stored on.
     *  Returns -1 for items that have no stored blob. */
    public short getVolumeId() {
        return mData.volumeId;
    }

    /** Returns the SHA-1 hash of the item's uncompressed blob.  Returns
     *  <code>""</code> for items that have no stored blob. */
    public String getDigest() {
        return getDigest(false);
    }

    /**
     * Returns the SHA-1 hash of the item's uncompressed blob.  If item has
     * no blob, the return value depends on preserveNull argument.  If
     * preserveNull is true, null is returned.  If not, "" (empty string) is
     * returned.
     * @param preserveNull
     * @return
     */
    public String getDigest(boolean preserveNull) {
        if (preserveNull || mData.blobDigest != null)
            return mData.blobDigest;
        else
            return "";
    }

    /** Returns the date the item's content was last modified.  For immutable
     *  objects (e.g. received messages), this will be the same as the date
     *  the item was created. */
    public long getDate() {
        return mData.date * 1000L;
    }
    
    /** Returns the change ID corresponding to the last time the item's
     *  content was modified.  For immutable objects (e.g. received messages),
     *  this will be the same change ID as when the item was created. */
    public int getSavedSequence() {
        return mData.modContent;
    }
    
    /** Returns the date the item's metadata and/or content was last modified.
     *  This includes changes in tags and flags as well as folder-to-folder
     *  moves and recoloring. */
    public long getChangeDate() {
        return mData.dateChanged * 1000L;
    }

    /** Returns the change ID corresponding to the last time the item's
     *  metadata and/or content was modified.  This includes changes in tags
     *  and flags as well as folder-to-folder moves and recoloring. */
    public int getModifiedSequence() {
        return mData.modMetadata;
    }

    public int getSize() {
        return mData.size;
    }

    public String getSubject() {
        return (mData.subject == null ? "" : mData.subject);
    }

    public int getUnreadCount() {
        return mData.unreadCount;
    }

    /** Returns the "external" flag bitmask, which includes
     *  {@link Flag#BITMASK_UNREAD} when the item is unread. */
    public int getFlagBitmask() {
        int flags = mData.flags;
        if (isUnread())
            flags = flags | Flag.BITMASK_UNREAD;
        return flags;
    }

    /** Returns the "internal" flag bitmask, which does not include
     *  {@link Flag#BITMASK_UNREAD}.  This is the same bitmask as is stored
     *  in the database's <code>MAIL_ITEM.FLAGS</code> column. */
    public int getInternalFlagBitmask() {
        return mData.flags;
    }

    /** Returns the external string representation of this item's flags.
     *  This string includes the state of {@link Flag#BITMASK_UNREAD} and is
     *  formed by concatenating the appropriate {@link Flag#FLAG_REP}
     *  characters for all flags set on the item. */
    public String getFlagString() {
        if (mData.flags == 0)
            return isUnread() ? Flag.UNREAD_FLAG_ONLY : "";
        int flags = mData.flags | (isUnread() ? Flag.BITMASK_UNREAD : 0);
        return Flag.bitmaskToFlags(flags);
    }

    public long getTagBitmask() {
        return mData.tags;
    }

    public String getTagString() {
        return Tag.bitmaskToTags(mData.tags);
    }

    public boolean isTagged(Tag tag) {
        long bitmask = (tag == null ? 0 : (tag instanceof Flag ? mData.flags : mData.tags));
        return ((bitmask & tag.getBitmask()) != 0);
    }
    
    public List<Tag> getTagList() throws ServiceException {
        return Tag.bitmaskToTagList(mMailbox, mData.tags);
    }

    private boolean isTagged(int tagId) {
        long bitmask = (tagId < 0 ? mData.flags : mData.tags);
        int position = (tagId < 0 ? Flag.getIndex(tagId) : Tag.getIndex(tagId));
        return ((bitmask & (1L << position)) != 0);
    }

    public boolean isUnread() {
        return mData.unreadCount > 0;
    }

    public boolean isFlagged() {
        return isTagged(Flag.ID_FLAG_FLAGGED);
    }

    public boolean hasAttachment() {
        return isTagged(Flag.ID_FLAG_ATTACHED);
    }

    /** Returns whether the item is in the "main mailbox", i.e. not in the
     *  Junk or Trash folders.  Items in subfolders of Trash are considered
     *  to be in the Trash and hence not "inMailbox".
     *
     * @throws ServiceException on errors fetching the item's folder.
     * @see #inTrash
     * @see #inSpam */
    public boolean inMailbox() throws ServiceException {
        return !inSpam() && !inTrash();
    }

    /** Returns whether the item is in the Trash folder or any of its
     *  subfolders.
     *
     * @throws ServiceException on errors fetching the item's folder. */
    public boolean inTrash() throws ServiceException {
        if (mData.folderId <= Mailbox.HIGHEST_SYSTEM_ID)
            return (mData.folderId == Mailbox.ID_FOLDER_TRASH);
        Folder folder = null;
        synchronized (mMailbox) {
            folder = mMailbox.getFolderById(null, getFolderId());
        }
        return folder.inTrash();
    }

    /** Returns whether the item is in the Junk folder.  (The Junk folder
     *  may not have subfolders.) */
    public boolean inSpam() {
        return (mData.folderId == Mailbox.ID_FOLDER_SPAM);
    }


    /** Returns whether the caller has the requested access rights on this
     *  item.  The owner of the {@link Mailbox} has all rights on all items
     *  in the Mailbox, as do all admin accounts.  All other users must be
     *  explicitly granted access.  <i>(Tag sharing and negative rights not
     *  yet implemented.)</i>  The authenticated user is fetched from the
     *  transaction's {@link Mailbox.OperationContext} via a call to
     *  {@link Mailbox#getAuthenticatedAccount}.
     * 
     * @param rightsNeeded  A set of rights (e.g. {@link ACL#RIGHT_READ}
     *                      and {@link ACL#RIGHT_DELETE}).
     * @throws ServiceException on errors fetching LDAP entries or
     *         retrieving the item's folder
     * @see ACL
     * @see Folder#checkRights(short, Account) */
    boolean canAccess(short rightsNeeded) throws ServiceException {
        return canAccess(rightsNeeded, mMailbox.getAuthenticatedAccount());
    }
    
    /** Returns whether the specified account has the requested access rights
     *  on this item.  The owner of the {@link Mailbox} has all rights on all
     *  items in the Mailbox, as do all admin accounts.  All other users must
     *  be explicitly granted access.  <i>(Tag sharing and negative rights not
     *  yet implemented.)</i>
     * 
     * @param rightsNeeded  A set of rights (e.g. {@link ACL#RIGHT_READ}
     *                      and {@link ACL#RIGHT_DELETE}).
     * @param authuser      The user whose rights we need to query.
     * @throws ServiceException on errors fetching LDAP entries or
     *         retrieving the item's folder
     * @see ACL
     * @see Folder#canAccess(short) */
    boolean canAccess(short rightsNeeded, Account authuser) throws ServiceException {
        if (rightsNeeded == 0)
            return true;
        return checkRights(rightsNeeded, authuser) == rightsNeeded;
    }

    /** Returns the subset of the requested access rights that the user has
     *  been granted on this item.  The owner of the {@link Mailbox} has
     *  all rights on all items in the Mailbox, as do all admin accounts.
     *  All other users must be explicitly granted access.  <i>(Tag sharing
     *  and negative rights not yet implemented.)</i>
     * 
     * @param rightsNeeded  A set of rights (e.g. {@link ACL#RIGHT_READ}
     *                      and {@link ACL#RIGHT_DELETE}).
     * @param authuser      The user whose rights we need to query.
     * @see ACL
     * @see Folder#checkRights(short, Account) */
    short checkRights(short rightsNeeded, Account authuser) throws ServiceException {
        // check to see what access has been granted on the enclosing folder
        short granted = getFolder().checkRights(rightsNeeded, authuser);
        // FIXME: check to see what access has been granted on the item's tags
        //   granted |= getTags().getGrantedRights(rightsNeeded, authuser);
        // and see if the granted rights are sufficient
        return (short) (granted & rightsNeeded);
    }


    /** Returns the {@link MailboxBlob} corresponding to the item's on-disk
     *  representation.  If the item is memory- or database-only, returns
     *  <code>null</code>.
     * 
     * @throws ServiceException if the file cannot be found. */
    public synchronized MailboxBlob getBlob() throws ServiceException {
        if (mBlob == null && mData.blobDigest != null) {
            mBlob = StoreManager.getInstance().getMailboxBlob(mMailbox, mId, mData.modContent, mData.volumeId);
            if (mBlob == null)
                throw ServiceException.FAILURE("missing blob for id: " + mId + ", change: " + mData.modContent, null);
        }
        return mBlob;
    }


    public int compareTo(Object o) {
        if (this == o)
            return 0;
        MailItem that = (MailItem) o;
        long myDate = getChangeDate(), theirDate = that.getChangeDate();
        return (myDate < theirDate ? -1 : (myDate == theirDate ? 0 : 1));
    }

    public static final class SortDateAscending implements Comparator<MailItem> {
        public int compare(MailItem m1, MailItem m2) {
            long t1 = m1.getDate();
            long t2 = m2.getDate();

            if (t1 < t2)        return -1;
            else if (t1 == t2)  return 0;
            else                return 1;
        }
    }

    public static final class SortDateDescending implements Comparator<MailItem> {
        public int compare(MailItem m1, MailItem m2) {
            long t1 = m1.getDate();
            long t2 = m2.getDate();

            if (t1 < t2)        return 1;
            else if (t1 == t2)  return 0;
            else                return -1;
        }
    }
    
    public static final class SortImapUid implements Comparator<MailItem> {
        public int compare(MailItem m1, MailItem m2) {
            long t1 = m1.getImapUid();
            long t2 = m2.getImapUid();

            if (t1 < t2)        return -1;
            else if (t1 == t2)  return 0;
            else                return 1;
        }
    }

    public static final class SortSubjectAscending implements Comparator<MailItem> {
        public int compare(MailItem m1, MailItem m2) {
            return m1.getSubject().compareTo(m2.getSubject());
        }
    }

    public static final class SortSubjectDescending implements Comparator<MailItem> {
        public int compare(MailItem m1, MailItem m2) {
            return -m1.getSubject().compareTo(m2.getSubject());
        }
    }

    static Comparator<MailItem> getComparator(byte sort) {
        boolean ascending = (sort & DbMailItem.SORT_DIRECTION_MASK) == DbMailItem.SORT_ASCENDING;
        switch (sort & DbMailItem.SORT_FIELD_MASK) {
            case DbMailItem.SORT_BY_DATE:     return ascending ? new SortDateAscending() : new SortDateDescending();
            case DbMailItem.SORT_BY_SUBJECT:  return ascending ? new SortSubjectAscending() : new SortSubjectDescending();
        }
        return null;
    }


    /** Adds the item to the index.
     * 
     * @param redo       The redo recorder for the indexing operation.
     * @param indexData  Extra data to index.  Each subclass of MailItem
     *                   should interpret this argument differently;
     *                   currently only Message class uses this argument for
     *                   passing in a {@link com.zimbra.cs.mime.ParsedMessage}.
     * @throws ServiceException */
    public void reindex(IndexItem redo, Object indexData) throws ServiceException {
        // override in subclasses that support indexing
    }

    /** Returns the item's parent from the {@link Mailbox}'s cache.  If
     *  the item has no parent or the parent is not currently cached,
     *  returns <code>null</code>.
     * 
     * @throws ServiceException if there is a problem retrieving the
     *         Mailbox's item cache. */
    MailItem getCachedParent() throws ServiceException {
        if (mData.parentId == -1)
            return null;
        return mMailbox.getCachedItem(mData.parentId);
    }
    /** Returns the item's parent.  Returns <code>null</code> if the item
     *  does not have a parent.
     * 
     * @throws ServiceException if there is an error retrieving the
     *         Mailbox's item cache or fetching the parent's data from 
     *         the database. */
    MailItem getParent() throws ServiceException {
        if (mData.parentId == -1)
            return null;
        return mMailbox.getItemById(mData.parentId, TYPE_UNKNOWN);
    }

    /** Returns the item's {@link Folder}.  All items in the system must
     *  have a containing folder.
     * 
     * @throws ServiceException if there is a problem fetching folder data
     *         from the database. */
    Folder getFolder() throws ServiceException {
        return mMailbox.getFolderById(mData.folderId);
    }

    boolean checkChangeID() throws ServiceException {
        return mMailbox.checkItemChangeID(this);
    }


    abstract boolean isTaggable();
    abstract boolean isCopyable();
    abstract boolean isMovable();
    abstract boolean isMutable();
    abstract boolean isIndexed();
    abstract boolean canHaveChildren();
    boolean isDeletable()             { return true; }
    boolean isLeafNode()              { return true; }
    boolean trackUnread()             { return true; }
    boolean canParent(MailItem child) { return canHaveChildren(); }

    static MailItem getById(Mailbox mbox, int id) throws ServiceException {
        return getById(mbox, id, TYPE_UNKNOWN);
    }
    static MailItem getById(Mailbox mbox, int id, byte type) throws ServiceException {
        return mbox.getItem(DbMailItem.getById(mbox, id, type));
    }
    static List<MailItem> getById(Mailbox mbox, Collection<Integer> ids, byte type) throws ServiceException {
        if (ids == null || ids.isEmpty())
            return Collections.emptyList();
        List<MailItem> items = new ArrayList<MailItem>();
        for (UnderlyingData ud : DbMailItem.getById(mbox, ids, type))
            items.add(mbox.getItem(ud));
        return items;
    }

    static MailItem getByImapId(Mailbox mbox, int id, int folderId) throws ServiceException {
        return mbox.getItem(DbMailItem.getByImapId(mbox, id, folderId));
    }

    /** Instantiates the appropriate subclass of <code>MailItem</code> for
     *  the item described by the {@link MailItem.UnderlyingData}.  Will
     *  not create memory-only <code>MailItem</code>s like {@link Flag}
     *  and {@link VirtualConversation}.
     * 
     * @param mbox  The {@link Mailbox} the item is created in.
     * @param data  The contents of a <code>MAIL_ITEM</code> database row. */
    static MailItem constructItem(Mailbox mbox, UnderlyingData data) throws ServiceException {
        if (data == null)
            throw noSuchItem(-1, TYPE_UNKNOWN);
        switch (data.type) {
            case TYPE_FOLDER:       return new Folder(mbox, data);
            case TYPE_SEARCHFOLDER: return new SearchFolder(mbox, data);
            case TYPE_TAG:          return new Tag(mbox, data);
            case TYPE_CONVERSATION: return new Conversation(mbox,data);
            case TYPE_MESSAGE:      return new Message(mbox, data);
            case TYPE_CONTACT:      return new Contact(mbox,data);
            case TYPE_DOCUMENT:     return new Document(mbox, data);
            case TYPE_NOTE:         return new Note(mbox, data);
            case TYPE_APPOINTMENT:  return new Appointment(mbox, data);
            case TYPE_MOUNTPOINT:   return new Mountpoint(mbox, data);
            case TYPE_WIKI:         return new WikiItem(mbox, data);
            default:                return null;
        }
    }
    /** Returns {@link MailServiceException.NoSuchItemException} tailored
     *  for the given type.  Does not actually <u>throw</u> the exception;
     *  that's the caller's job.
     * 
     * @param id    The id of the missing item.
     * @param type  The type of the missing item (e.g. {@link #TYPE_TAG}). */
    public static MailServiceException noSuchItem(int id, byte type) {
        switch (type) {
            case TYPE_SEARCHFOLDER:
            case TYPE_MOUNTPOINT:
            case TYPE_FOLDER:       return MailServiceException.NO_SUCH_FOLDER(id);
            case TYPE_FLAG:
            case TYPE_TAG:          return MailServiceException.NO_SUCH_TAG(id);
            case TYPE_VIRTUAL_CONVERSATION:
            case TYPE_CONVERSATION: return MailServiceException.NO_SUCH_CONV(id);
//          case TYPE_INVITE:
            case TYPE_MESSAGE:      return MailServiceException.NO_SUCH_MSG(id);
            case TYPE_CONTACT:      return MailServiceException.NO_SUCH_CONTACT(id);
            case TYPE_DOCUMENT:     return MailServiceException.NO_SUCH_DOC(id);
            case TYPE_NOTE:         return MailServiceException.NO_SUCH_NOTE(id);
            case TYPE_APPOINTMENT:  return MailServiceException.NO_SUCH_APPT(id);
            default:                return MailServiceException.NO_SUCH_ITEM(id);
        }
    }

    /** Returns whether the an item type is a "subclass" of another item type.
     *  For instance, returns <code>true</code> if you have an item of type
     *  {@link #TYPE_FLAG} and you wanted things of type {@link #TYPE_TAG}.
     *  The exception to this rule is that a desired {@link #TYPE_UNKNOWN}
     *  matches any actual item type.
     * 
     * @param desired  The type of item that you wanted.
     * @param actual   The type of item that you've got.
     * @return <code>true</code> if the types match, if <code>desired</code>
     *         is {@link #TYPE_UNKNOWN}, or if the <code>actual</code> class
     *         is a subclass of the <code>desired</code> class. */
    public static boolean isAcceptableType(byte desired, byte actual) {
        // standard case: exactly what we're asking for
        if (desired == actual || desired == TYPE_UNKNOWN)
            return true;
        // exceptions: ask for Tag and get Flag, ask for Folder and get SearchFolder or Mountpoint,
        //             ask for Conversation and get VirtualConversation
        else if (desired == TYPE_FOLDER && actual == TYPE_SEARCHFOLDER)
            return true;
        else if (desired == TYPE_FOLDER && actual == TYPE_MOUNTPOINT)
            return true;
        else if (desired == TYPE_TAG && actual == TYPE_FLAG)
            return true;
        else if (desired == TYPE_CONVERSATION && actual == TYPE_VIRTUAL_CONVERSATION)
            return true;
        // failure: found something, but it's not the type you were looking for
        else
            return false;
    }


    /** Adds this item to the {@link Mailbox}'s list of items created during
     *  the transaction. */
    void markItemCreated() {
        mMailbox.markItemCreated(this);
    }
    /** Adds this item to the {@link Mailbox}'s list of items deleted during
     *  the transaction. */
    void markItemDeleted() {
        mMailbox.markItemDeleted(this);
    }
    /** Adds this item to the {@link Mailbox}'s list of items modified during
     *  the transaction.
     * 
     * @param reason  The bitmask of changes made to the item.
     * @see PendingModifications.Change */
    void markItemModified(int reason) {
        mMailbox.markItemModified(this, reason);
    }

    /** Removes all this item's children from the {@link Mailbox}'s cache.
     *  Does not uncache the item itself. */
    void uncacheChildren() throws ServiceException {
        if (mData.children != null)
            for (Integer childId : mData.children)
                mMailbox.uncacheItem(childId);
    }


    protected void finishCreation(MailItem parent) throws ServiceException {
        markItemCreated();

        // let the parent know it's got a new child
        if (parent != null)
            parent.addChild(this);

        // sanity-check the location of the newly-created item
        Folder folder = getFolder();
        if (!folder.canContain(this))
            throw MailServiceException.CANNOT_CONTAIN();

        // update mailbox and folder sizes
        mMailbox.updateSize(mData.size);
        if (isLeafNode())
            folder.updateSize(1);

        // let the folder and tags know if the new item is unread
        folder.updateUnread(mData.unreadCount);
        updateTagUnread(mData.unreadCount);
    }

    /** Changes the item's color.  The server does no value-to-color mapping;
     *  the supplied color is treated as an opaque byte.  Note than even
     *  "immutable" items can have their color changed.
     * 
     * @param color  The item's new color.
     * @perms {@link ACL#RIGHT_WRITE} on the item
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><code>service.PERM_DENIED</code> - if you don't have
     *        sufficient permissions</ul> */
    void setColor(byte color) throws ServiceException {
        if (!canAccess(ACL.RIGHT_WRITE))
            throw ServiceException.PERM_DENIED("you do not have the necessary permissions on the item");
        if (color == mColor)
            return;
        markItemModified(Change.MODIFIED_COLOR);
        mColor = color;
        saveMetadata();
    }

    void setImapUid(int imapId) throws ServiceException {
        if (mData.imapId == imapId)
            return;
        markItemModified(Change.MODIFIED_IMAP_UID);
        mData.imapId = imapId;
        DbMailItem.saveImapUid(this);
    }

    Blob setContent(byte[] data, String digest, short volumeId, Object content)
    throws ServiceException, IOException {
        // catch the "was no blob, is no blob" case
        if (digest == null && mData.blobDigest == null)
            return null;

        // delete the old blob *unless* we've already rewritten it in this transaction
        if (getSavedSequence() != mMailbox.getOperationChangeID()) {
            // mark the old blob as ready for deletion
            PendingDelete info = getDeletionInfo();
            info.itemIds.clear();  info.unreadIds.clear();
            mMailbox.markOtherItemDirty(info);
        }

        // remove the content from the cache
        MessageCache.purge(this);

        // update the item's relevant attributes
        markItemModified(Change.MODIFIED_CONTENT  | Change.MODIFIED_DATE |
                         Change.MODIFIED_IMAP_UID | Change.MODIFIED_SIZE);

        int size = (data == null ? 0 : data.length);
        if (mData.size != size) {
            mMailbox.updateSize(size - mData.size, false);
            mData.size = size;
        }
        mData.blobDigest = digest;
        mData.date       = mMailbox.getOperationTimestamp();
        mData.volumeId   = volumeId;
        mData.imapId     = mMailbox.isTrackingImap() ? 0 : mData.id;
        mData.contentChanged(mMailbox);
        mBlob = null;

        // rewrite the DB row to reflect our new view
        reanalyze(content);

        if (data == null)
            return null;

        // write the content to the store
        StoreManager sm = StoreManager.getInstance();
        Blob blob = sm.storeIncoming(data, digest, null, volumeId);
        MailboxBlob mb = sm.renameTo(blob, mMailbox, mId, getSavedSequence(), volumeId);
        mMailbox.markOtherItemDirty(mb);

        return blob;
    }

    /** Recalculates the size, metadata, etc. for an existing MailItem and
     *  persists that information to the database.  Maintains any existing
     *  mutable metadata.  Updates mailbox and folder sizes appropriately.
     * 
     * @param data  The (optional) extra item data for indexing (e.g.
     *              a Message's {@link com.zimbra.cs.index.ParsedMessage}. */
    void reanalyze(Object data) throws ServiceException {
        throw ServiceException.FAILURE("reanalysis of " + getNameForType(this) + "s not supported", null);
    }

    void detach() throws ServiceException  { }

    /** Updates the item's unread state.  Persists the change to the
     *  database and cache, and also updates the unread counts for the
     *  item's {@link Folder} and {@link Tag}s appropriately.
     * 
     * @param unread  <code>true</code> to mark the item unread,
     *                <code>false</code> to mark it as read.
     * @perms {@link ACL#RIGHT_WRITE} on the item
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><code>mail.CANNOT_TAG</code> - if the item can't be marked
     *        unread
     *    <li><code>service.FAILURE</code> - if there's a database failure
     *    <li><code>service.PERM_DENIED</code> - if you don't have
     *        sufficient permissions</ul> */
    void alterUnread(boolean unread) throws ServiceException {
        // detect NOOPs and bail
        if (unread == isUnread())
            return;
        if (!mMailbox.mUnreadFlag.canTag(this))
            throw MailServiceException.CANNOT_TAG(mMailbox.mUnreadFlag, this);
        if (!canAccess(ACL.RIGHT_WRITE))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the item");

        markItemModified(Change.MODIFIED_UNREAD);
        mData.metadataChanged(mMailbox);
        updateUnread(unread ? 1 : -1);
        DbMailItem.alterUnread(this, unread);
    }

    /** Tags or untags an item.  Persists the change to the database and
     *  cache.  If the item is unread and its tagged state is changing,
     *  updates the {@link Tag}'s unread count appropriately.<p>
     * 
     *  You must use {@link #alterUnread} to change an item's unread state.
     * 
     * @param tag  The tag or flag to add or remove from the item.
     * @param add  <code>true</code> to tag the item,
     *             <code>false</code> to untag it.
     * @perms {@link ACL#RIGHT_WRITE} on the item
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><code>mail.CANNOT_TAG</code> - if the item can't be tagged
     *        with the specified tag
     *    <li><code>service.FAILURE</code> - if there's a database
     *        failure or if an invalid Tag is supplied
     *    <li><code>service.PERM_DENIED</code> - if you don't have
     *        sufficient permissions</ul>
     * @see #alterUnread(boolean) */
    void alterTag(Tag tag, boolean add) throws ServiceException {
        if (tag == null)
            throw ServiceException.FAILURE("no tag supplied when trying to tag item " + mId, null);
        if (!isTaggable() || (add && !tag.canTag(this)))
            throw MailServiceException.CANNOT_TAG(tag, this);
        if (tag.getId() == Flag.ID_FLAG_UNREAD)
            throw ServiceException.FAILURE("unread state must be set with alterUnread", null);
        if (!canAccess(ACL.RIGHT_WRITE))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the item");
        // detect NOOPs and bail
        if (add == isTagged(tag))
            return;
        markItemModified(tag instanceof Flag ? Change.MODIFIED_FLAGS : Change.MODIFIED_TAGS);

        // change our cached tags
        tagChanged(tag, add);

        // tell our parent about the tag change (note: must precede DbMailItem.alterTags)
        MailItem parent = getCachedParent();
        if (parent != null)
            parent.inheritedTagChanged(tag, add, true);

        // since we're adding/removing a tag, the tag's unread count may change
        if (tag.trackUnread() && mData.unreadCount > 0)
            tag.updateUnread((add ? 1 : -1) * mData.unreadCount);

        // alter our tags in the DB
        DbMailItem.alterTag(this, tag, add);
    }

    /** Updates the object's in-memory state to reflect a {@link Tag} change.
     *  Does not update the database.
     * 
     * @param tag  The tag that was added or rmeoved from this object.
     * @param add  <code>true</code> if the item was tagged,
     *             <code>false</code> if the item was untagged.*/
    protected void tagChanged(Tag tag, boolean add) throws ServiceException {
        markItemModified(tag instanceof Flag ? Change.MODIFIED_FLAGS : Change.MODIFIED_TAGS);
        mData.metadataChanged(mMailbox);

        if (tag instanceof Flag) {
            if (add)  mData.flags |= tag.getBitmask();
            else      mData.flags &= ~tag.getBitmask();
        } else {
            if (add)  mData.tags |= tag.getBitmask();
            else      mData.tags &= ~tag.getBitmask();
        }
    }

    protected void inheritedTagChanged(Tag tag, boolean add, boolean onChild) { }

    /** Updates the in-memory unread counts for the item.  Also updates the
     *  item's folder, its tag, and its parent.  Note that the parent is not
     *  fetched from the database, so notifications may be off in the case of
     *  uncached {@link Conversation}s when a {@link Message} changes state.
     * 
     * @param delta  The change in unread count for this item. */
    protected void updateUnread(int delta) throws ServiceException {
        if (delta == 0 || !trackUnread())
            return;
        markItemModified(Change.MODIFIED_UNREAD);

        // update our unread count (should we check that we don't have too many unread?)
        mData.unreadCount += delta;
        if (mData.unreadCount < 0)
            throw ServiceException.FAILURE("inconsistent state: unread < 0 for " + getClass().getName() + " " + mId, null);

        // update the folder's unread count
        getFolder().updateUnread(delta);

        // update the parent's unread count
        MailItem parent = getCachedParent();
        if (parent != null)
            parent.updateUnread(delta);

        // tell the tags about the new unread item
        updateTagUnread(delta);
    }

    /** Adds <code>delta</code> to the unread count of each {@link Tag}
     *  assigned to this <code>MailItem</code>.
     * 
     * @param delta  The (signed) change in number unread.
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><code>mail.NO_SUCH_FOLDER</code> - if there's an error
     *        fetching the item's {@link Folder}</ul> */
    protected void updateTagUnread(int delta) throws ServiceException {
        if (delta == 0 || !isTaggable() || mData.tags == 0)
            return;
        long tags = mData.tags;
        for (int i = 0; tags != 0 && i < MAX_TAG_COUNT; i++) {
            long mask = 1L << i;
            if ((tags & mask) != 0) {
                Tag tag = null;
                try {
                    tag = mMailbox.getTagById(i + TAG_ID_OFFSET);
                } catch (MailServiceException.NoSuchItemException nsie) {
                    ZimbraLog.mailbox.warn("item " + mId + " has nonexistent tag " + (i + TAG_ID_OFFSET));
                    continue;
                }
                tag.updateUnread(delta);
                tags &= ~mask;
            }
        }
    }

    /** Updates the user-settable set of {@link Flag}s and {@link Tag}s on
     *  the item.  This overwrites the old set of flags and tags, but will
     *  not change system flags that are normally immutable after item
     *  creation, like {@link Flag#BITMASK_ATTACHED} and {@link Flag#BITMASK_DRAFT}.
     *  If a specified flag or tag does not exist, it is ignored.
     * 
     * @param flags  The bitmask of user-settable flags to apply.
     * @param tags   The bitmask of tags to apply.
     * @perms {@link ACL#RIGHT_WRITE} on the item
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><code>service.FAILURE</code> - if there's a database failure
     *    <li><code>service.PERM_DENIED</code> - if you don't have
     *        sufficient permissions</ul> */
    void setTags(int flags, long tags) throws ServiceException {
        if (!canAccess(ACL.RIGHT_WRITE))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the item");

        // FIXME: more optimal would be to do this with a single db UPDATE...

        // make sure the caller can't change immutable flags
        flags = (flags & ~Flag.FLAG_SYSTEM) | (getFlagBitmask() & Flag.FLAG_SYSTEM);
        // handle flags first...
        if (flags != mData.flags) {
            markItemModified(Change.MODIFIED_FLAGS);
            for (int i = 0; i < mMailbox.mFlags.length; i++) {
                Flag flag = mMailbox.mFlags[i];
                if (flag != null && (flags & flag.getBitmask()) != (mData.flags & flag.getBitmask()))
                    alterTag(flag, !isTagged(flag));
            }
        }

        // then handle tags...
        if (tags != mData.tags) {
            markItemModified(Change.MODIFIED_TAGS);
            for (int i = 0; i < MAX_TAG_COUNT; i++) {
                long mask = 1L << i;
                if ((tags & mask) != (mData.tags & mask)) {
                    Tag tag = null;
                    try {
                        tag = mMailbox.getTagById(i + TAG_ID_OFFSET);
                    } catch (MailServiceException.NoSuchItemException nsie) {
                        continue;
                    }
                    alterTag(tag, !isTagged(tag));
                }
            }
        }
    }

    /** Copies an item to a {@link Folder}.  Persists the new item to the
     *  database and the in-memory cache.  Copies to the same folder as the
     *  original item will succeed.<p>
     * 
     *  Immutable copied items (both the original and the target) share the
     *  same entry in the index and get the {@link Flag#BITMASK_COPIED} flag to
     *  facilitate garbage collection of index entries.  (Mutable copied items
     *  are indexed separately.)  They do not share the same blob on disk,
     *  although the system will use a hard link where possible.  Copying a
     *  {@link Message} will put it in the same {@link Conversation} as the
     *  original (exceptions: draft messages, messages in the Junk folder).
     * 
     * @param folder        The folder to copy the item to.
     * @param id            The item id for the newly-created copy.
     * @param destVolumeId  The id of the Volume to put the copied blob in.
     * @perms {@link ACL#RIGHT_INSERT} on the target folder,
     *        {@link ACL#RIGHT_READ} on the original item
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><code>mail.CANNOT_COPY</code> - if the item is not copyable
     *    <li><code>mail.CANNOT_CONTAIN</code> - if the target folder
     *        can't hold the copy of the item
     *    <li><code>service.FAILURE</code> - if there's a database failure
     *    <li><code>service.PERM_DENIED</code> - if you don't have
     *        sufficient permissions</ul>
     * @see com.zimbra.cs.store.Volume#getCurrentMessageVolume() */
    MailItem copy(Folder folder, int id, short destVolumeId) throws IOException, ServiceException {
        if (!isCopyable())
            throw MailServiceException.CANNOT_COPY(mId);
        if (!folder.canContain(this))
            throw MailServiceException.CANNOT_CONTAIN();

        if (!canAccess(ACL.RIGHT_READ))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the item");
        if (!folder.canAccess(ACL.RIGHT_INSERT))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the target folder");

        // we'll share the index entry if this item can't change out from under us
        if (isIndexed() && !isMutable())
            alterTag(mMailbox.mCopiedFlag, true);

        // if the copy or original is in Spam, put the copy in its own conversation
        boolean detach = getParentId() <= 0 || isTagged(mMailbox.mDraftFlag) || inSpam() != folder.inSpam();

        UnderlyingData data = mData.duplicate(id, folder.getId(), destVolumeId);
        if (detach)
            data.parentId = -1;
        if (isIndexed() && isMutable())
            data.indexId = id;
        data.metadata = encodeMetadata();
        data.contentChanged(mMailbox);
        DbMailItem.copy(this, id, folder, data.indexId, data.parentId, data.volumeId, data.metadata);

        MailItem copy = constructItem(mMailbox, data);
        copy.finishCreation(detach ? null : getParent());

        MailboxBlob srcBlob = getBlob();
        if (srcBlob != null) {
            StoreManager sm = StoreManager.getInstance();
            MailboxBlob blob = sm.link(srcBlob.getBlob(), mMailbox, data.id, data.modContent, data.volumeId);
            mMailbox.markOtherItemDirty(blob);
        }
        return copy;
    }

    /** Moves the item to the target folder and creates a copy of the item in
     *  the original item's folder.  Persists the new item to the database and
     *  the in-memory cache.  Copies to the same folder as the original item
     *  will succeed, but it is strongly suggested that {@link copy()} be used
     *  in that case.<p>
     * 
     *  Immutable copied items (both the original and the target) share the
     *  same entry in the index and get the {@link Flag#BITMASK_COPIED} flag to
     *  facilitate garbage collection of index entries.  (Mutable copied items
     *  are indexed separately.)  They do not share the same blob on disk,
     *  although the system will use a hard link where possible.  Copied
     *  {@link Message}s are placed in their own {@link VirtualConversation}
     *  rather than being grouped with the original (moved) Message.<p>
     * 
     *  The copy is assigned the same IMAP UID as the original item.  The
     *  moved item is then explicitly assigned a new IMAP UID.  
     * 
     * @param target        The folder to copy the item to.
     * @param id            The item id for the newly-created copy.
     * @param destVolumeId  The id of the Volume to put the copied blob in.
     * @param imapId        The IMAP UID to assign to the <u>moved</u> item.
     * @perms {@link ACL#RIGHT_INSERT} on the target folder,
     *        {@link ACL#RIGHT_READ} on the original item
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><code>mail.CANNOT_COPY</code> - if the item is not copyable
     *    <li><code>mail.CANNOT_CONTAIN</code> - if the target folder
     *        can't hold the copy of the item
     *    <li><code>service.FAILURE</code> - if there's a database failure
     *    <li><code>service.PERM_DENIED</code> - if you don't have
     *        sufficient permissions</ul>
     * @see com.zimbra.cs.store.Volume#getCurrentMessageVolume() */
    MailItem icopy(Folder target, int id, short destVolumeId, int imapId) throws IOException, ServiceException {
        if (!isCopyable())
            throw MailServiceException.CANNOT_COPY(mId);
        if (!isMovable())
            throw MailServiceException.IMMUTABLE_OBJECT(mId);
        if (!target.canContain(this))
            throw MailServiceException.CANNOT_CONTAIN();

        // permissions required are the same as for copy()
        if (!canAccess(ACL.RIGHT_READ))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the item");
        if (!target.canAccess(ACL.RIGHT_INSERT))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the target folder");

        // we'll share the index entry if this item can't change out from under us
        boolean shared = isIndexed() && !isMutable();
        boolean moved = getFolderId() != target.getId();

        int copyImapId = moved ? getImapUid() : imapId;
        Folder oldFolder = getFolder();

        // first, move the item to the target folder if necessary
        if (moved) {
            oldFolder.updateSize(-1);
            target.updateSize(1);
            oldFolder.updateUnread(-getUnreadCount());
            target.updateUnread(getUnreadCount());
            folderChanged(target, imapId);
            DbMailItem.imove(this, shared, target, imapId);
        } else if (shared && !isTagged(mMailbox.mCopiedFlag)) {
            DbMailItem.alterTag(this, mMailbox.mCopiedFlag, true);
        }

        if (shared)
            mData.flags |= Flag.BITMASK_COPIED;

        // then copy it back to the original folder, leaving the copy out of the conversation
        //   also, when moving make sure that the new item's imap ID is the same as the old item's old imap ID
        UnderlyingData data = mData.duplicate(id, oldFolder.getId(), destVolumeId);
        data.parentId = -1;
        data.metadata = encodeMetadata();
        if (moved)
            data.imapId = copyImapId;
        if (isIndexed() && !shared)
            data.indexId = id;
        data.contentChanged(mMailbox);
        DbMailItem.icopy(this, data);

        MailItem copy = constructItem(mMailbox, data);
        copy.finishCreation(null);

        MailboxBlob srcBlob = getBlob();
        if (srcBlob != null) {
            StoreManager sm = StoreManager.getInstance();
            MailboxBlob blob = sm.link(srcBlob.getBlob(), mMailbox, data.id, data.modContent, data.volumeId);
            mMailbox.markOtherItemDirty(blob);
        }
        return copy;
    }

    /** Moves an item to a different {@link Folder}.  Persists the change
     *  to the database and the in-memory cache.  Updates all relevant
     *  unread counts, folder sizes, etc.<p>
     * 
     *  Items moved to the Trash folder are automatically marked read.
     *  {@link Message}s moved to the Junk folder are removed from their
     *  {@link Conversation} (if any).  Conversations moved to the Junk
     *  folder will not receive newly-delivered messages.
     * 
     * @param folder  The folder to move the item to.
     * @perms {@link ACL#RIGHT_INSERT} on the target folder,
     *        {@link ACL#RIGHT_DELETE} on the source folder
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><code>mail.IMMUTABLE_OBJECT</code> - if the item is not
     *        movable
     *    <li><code>mail.CANNOT_CONTAIN</code> - if the target folder
     *        can't hold the item
     *    <li><code>service.FAILURE</code> - if there's a database failure
     *    <li><code>service.PERM_DENIED</code> - if you don't have
     *        sufficient permissions</ul> */
    void move(Folder folder) throws ServiceException {
        if (mData.folderId == folder.getId())
            return;
        markItemModified(Change.MODIFIED_FOLDER);
        if (!isMovable())
            throw MailServiceException.IMMUTABLE_OBJECT(mId);
        if (!folder.canContain(this))
            throw MailServiceException.CANNOT_CONTAIN();

        Folder oldFolder = getFolder();
        if (!oldFolder.canAccess(ACL.RIGHT_DELETE))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the source folder");
        if (!folder.canAccess(ACL.RIGHT_INSERT))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the target folder");

        if (isLeafNode()) {
            oldFolder.updateSize(-1);
            folder.updateSize(1);
        }

        if (!inTrash() && folder.inTrash()) {
            // moving something to Trash also marks it as read
            if (mData.unreadCount > 0)
                alterUnread(false);
        } else {
            oldFolder.updateUnread(-mData.unreadCount);
            folder.updateUnread(mData.unreadCount);
        }
        // moving a message (etc.) to Spam removes it from its conversation
        if (!inSpam() && folder.inSpam())
            detach();

        DbMailItem.setFolder(this, folder);
        folderChanged(folder, 0);
    }

    /** Records all relevant changes to the in-memory object for when an item
     *  gets moved to a new {@link Folder}.  Does <u>not</u> persist those
     *  changes to the database.
     * 
     * @param newFolder  The folder the item is being moved to.
     * @param imapId     The new IMAP ID for the item after the operation.
     * @throws ServiceException if we're not in a transaction */
    void folderChanged(Folder newFolder, int imapId) throws ServiceException {
        if (mData.folderId == newFolder.getId())
            return;
        markItemModified(Change.MODIFIED_FOLDER);
        mData.metadataChanged(mMailbox);
        mData.folderId = newFolder.getId();
        mData.imapId   = mMailbox.isTrackingImap() ? imapId : mData.imapId;
    }

    void addChild(MailItem child) throws ServiceException {
        markItemModified(Change.MODIFIED_CHILDREN);
        if (!canParent(child))
            throw MailServiceException.CANNOT_PARENT();
        if (mMailbox != child.getMailbox())
            throw MailServiceException.WRONG_MAILBOX();

        // update child list
        if (mData.children == null)
            (mData.children = new ArrayList<Integer>()).add(child.getId());
        else if (!mData.children.contains(child.getId()))
            mData.children.add(child.getId());

        // update unread counts
        updateUnread(child.mData.unreadCount);
    }

    void removeChild(MailItem child) throws ServiceException {
        markItemModified(Change.MODIFIED_CHILDREN);

        // update child list
        if (mData.children == null || !mData.children.contains(child.getId()))
            throw MailServiceException.IS_NOT_CHILD();
        mData.children.remove((Integer) child.getId());
        
        // remove parent reference from the child 
        child.mData.parentId = -1;

        // update unread counts
        updateUnread(-child.mData.unreadCount);
    }

    /** A record of all the relevant data about a set of items that we're
     *  in the process of deleting via a call to {@link MailItem#delete}. */
    public static class PendingDelete {
        /** The id of the item that {@link MailItem#delete} was called on. */
        public int  rootId;
        /** Whether some of the item's children are not being deleted. */
        public boolean incomplete;
        /** The total size of all the items being deleted. */
        public long size;
        /** The ids of all items being deleted. */
        public List<Integer> itemIds   = new ArrayList<Integer>();
        /** The ids of all unread items being deleted.  This is a subset of
         *  {@link #itemIds}. */
        public List<Integer> unreadIds = new ArrayList<Integer>();
        /** The ids of all items that must be deleted but whose deletion
         *  must be deferred because of foreign key constraints. (E.g.
         *  {@link Conversation}s whose messages are all deleted during a
         *  {@link Folder} delete.) */
        public List<Integer> cascadeIds;
        /** The document ids that need to be removed from the index. */
        public List<Integer> indexIds  = new ArrayList<Integer>();
        /** The ids of all items with the {@link Flag#BITMASK_COPIED} flag being
         *  deleted.  Items in <code>sharedIndex</code> whose last copies are
         *  being removed are added to {@link #indexIds} via a call to
         *  {@link DbMailItem#resolveSharedIndex}. */
        public Set<Integer> sharedIndex;
        /** The {@link com.zimbra.cs.store.Blob}s for all items being deleted that have content
         *  persisted in the store. */
        public List<MailboxBlob> blobs = new ArrayList<MailboxBlob>();
        /** The number of {@link Contact}s being deleted. */
        public int  contacts  = 0;
        /** Maps {@link Folder} ids to {@link DbMailItem.LocationCount}s
         *  tracking various per-folder counts for items being deleted. */
        public Map<Integer, DbMailItem.LocationCount> messages = new HashMap<Integer, DbMailItem.LocationCount>();

        /** Combines the data from another <code>PendingDelete</code> into
         *  this object.  The other <code>PendingDelete</code> is unmodified.
         * 
         * @return this item */
        PendingDelete add(PendingDelete other) {
            if (other != null) {
                size     += other.size;
                contacts += other.contacts;
                itemIds.addAll(other.itemIds);
                unreadIds.addAll(other.unreadIds);
                blobs.addAll(other.blobs);
            }
            return this;
        }
    }

    static final boolean DELETE_ITEM = false, DELETE_CONTENTS = true;

    void delete() throws ServiceException {
        delete(DELETE_ITEM);
    }
    void delete(boolean childrenOnly) throws ServiceException {
        if (!childrenOnly && !isDeletable())
            throw MailServiceException.IMMUTABLE_OBJECT(mId);

        // get the full list of things that are being removed
        PendingDelete info = getDeletionInfo();
        assert(info != null && info.itemIds != null);
        if (childrenOnly || info.incomplete) {
            // make sure to take the container's ID out of the list of deleted items
            info.itemIds.remove(new Integer(mId));
        } else {
            // update parent item's child list
            MailItem parent = getParent();
            if (parent != null)
                parent.removeChild(this);
        }

        // short-circuit now if nothing's actually being deleted
        if (info.itemIds.isEmpty())
            return;

        mMailbox.markItemDeleted(info.itemIds);
        // when applicable, record the deleted MailItem (rather than just its id)
        if (!childrenOnly && !info.incomplete)
            markItemDeleted();

        // update the mailbox's size
        mMailbox.updateSize(-info.size);
        mMailbox.updateContactCount(-info.contacts);

        // update conversations and unread counts on folders and tags
        propagateDeletion(info);

        // actually delete the item from the DB
        if (info.incomplete)
            DbMailItem.delete(mMailbox, info.itemIds);
        else if (childrenOnly)
            DbMailItem.deleteContents(this);
        else
            DbMailItem.delete(this);

        // remove the deleted item(s) from the mailbox's cache
        purgeCache(info, !childrenOnly && !info.incomplete);

        // cascade any other deletes
        if (info.cascadeIds != null && !info.cascadeIds.isEmpty()) {
            DbMailItem.delete(mMailbox, info.cascadeIds);
            mMailbox.markItemDeleted(info.cascadeIds);
            info.itemIds.addAll(info.cascadeIds);
        }

        // deal with index sharing
        if (info.sharedIndex != null && !info.sharedIndex.isEmpty())
            DbMailItem.resolveSharedIndex(mMailbox, info);

        mMailbox.markOtherItemDirty(info);

        // write a deletion record for later sync
        if (mMailbox.isTrackingSync() && info.itemIds.size() > 0)
            DbMailItem.writeTombstone(mMailbox, info);

        // don't actually delete the blobs or index entries here; wait until after the commit
    }

    /** Determines the set of items to be deleted.  Assembles a new
     *  {@link PendingDelete} object encapsulating the data on the items
     *  to be deleted.  If the caller has specified the maximum change
     *  number they know about, this set will also exclude any item for
     *  which the (modification/content) change number is greater.
     * 
     * @perms {@link ACL#RIGHT_DELETE} on the item
     * @return A fully-populated <code>PendingDelete</code> object. */
    PendingDelete getDeletionInfo() throws ServiceException {
        if (!canAccess(ACL.RIGHT_DELETE))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the item");

        Integer id = new Integer(mId);
        PendingDelete info = new PendingDelete();
        info.rootId = mId;
        info.size   = mData.size;
        info.itemIds.add(id);
        if (mData.unreadCount != 0 && mMailbox.mUnreadFlag.canTag(this))
            info.unreadIds.add(id);
        if (mData.indexId > 0) {
            if (!isTagged(mMailbox.mCopiedFlag))
                info.indexIds.add(new Integer(mData.indexId));
            else
                (info.sharedIndex = new HashSet<Integer>()).add(mData.indexId);
        }
        if (mData.blobDigest != null && mData.blobDigest.length() > 0)
            try {
                MailboxBlob mblob = StoreManager.getInstance().getMailboxBlob(mMailbox, mId, mData.modContent, mData.volumeId);
                if (mblob == null)
                    ZimbraLog.mailbox.error("missing blob for id: " + mId + ", change: " + mData.modContent);
                else
                    info.blobs.add(mblob);
            } catch (Exception e) { }
        int isMessage = (this instanceof Message ? 1 : 0);
        info.messages.put(new Integer(getFolderId()), new DbMailItem.LocationCount(isMessage, getSize()));
        return info;
    }

    void propagateDeletion(PendingDelete info) throws ServiceException {
        for (Map.Entry<Integer, DbMailItem.LocationCount> entry : info.messages.entrySet()) {
            Folder folder = mMailbox.getFolderById(entry.getKey());
            DbMailItem.LocationCount lc = entry.getValue();
            folder.updateSize(-lc.count);
        }

        if (info.unreadIds.isEmpty())
            return;
        // FIXME: try to get these from cache (use mMailbox.getItemById[])
        for (UnderlyingData data : DbMailItem.getById(mMailbox, info.unreadIds, TYPE_MESSAGE))
            mMailbox.getItem(data).updateUnread(-1);
    }

    void purgeCache(PendingDelete info, boolean purgeItem) throws ServiceException {
        // uncache cascades to uncache children
        if (purgeItem)
            mMailbox.uncache(this);
    }


    String encodeMetadata() {
        return encodeMetadata(new Metadata()).toString();
    }
    abstract Metadata encodeMetadata(Metadata meta);
    static Metadata encodeMetadata(Metadata meta, byte color) {
        if (color != DEFAULT_COLOR)
            meta.put(Metadata.FN_COLOR, color);
        return meta;
    }

    void decodeMetadata(String metadata) throws ServiceException {
        decodeMetadata(new Metadata(metadata, this));
    }
    void decodeMetadata(Metadata meta) throws ServiceException {
        if (meta == null)
            return;
        mColor = (byte) meta.getLong(Metadata.FN_COLOR, DEFAULT_COLOR);
    }


    protected void saveMetadata() throws ServiceException {
        saveMetadata(encodeMetadata());
    }
    protected void saveMetadata(String metadata) throws ServiceException {
        mData.metadataChanged(mMailbox);
        DbMailItem.saveMetadata(this, metadata);
    }

    protected void saveSubject() throws ServiceException {
        mData.contentChanged(mMailbox);
        DbMailItem.saveSubject(this);
    }

    protected void saveData(String sender) throws ServiceException {
        saveData(sender, encodeMetadata());
    }
    protected void saveData(String sender, String metadata) throws ServiceException {
        mData.metadataChanged(mMailbox);
        DbMailItem.saveData(this, sender, metadata);
    }


    private static final String CN_ID           = "id";
    private static final String CN_TYPE         = "type";
    private static final String CN_PARENT_ID    = "parent_id";
    private static final String CN_FOLDER_ID    = "folder_id";
    private static final String CN_DATE         = "date";
    private static final String CN_SIZE         = "size";
    private static final String CN_REVISION     = "rev";
    private static final String CN_BLOB_DIGEST  = "digest";
    private static final String CN_UNREAD_COUNT = "unread";
    private static final String CN_FLAGS        = "flags";
    private static final String CN_TAGS         = "tags";
    private static final String CN_SUBJECT      = "subject";
    private static final String CN_CHILDREN     = "children";
    private static final String CN_COLOR        = "color";
    private static final String CN_IMAP_ID      = "imap_id";

    protected StringBuffer appendCommonMembers(StringBuffer sb) {
        sb.append(CN_ID).append(": ").append(mId).append(", ");
        sb.append(CN_TYPE).append(": ").append(mData.type).append(", ");
        if (mData.parentId > 0)
            sb.append(CN_PARENT_ID).append(": ").append(mData.parentId).append(", ");
        sb.append(CN_FOLDER_ID).append(": ").append(mData.folderId).append(", ");
        sb.append(CN_DATE).append(": ").append(mData.date).append(", ");
        sb.append(CN_SIZE).append(": ").append(mData.size).append(", ");
        sb.append(CN_REVISION).append(": ").append(mData.modContent).append(", ");
        sb.append(CN_UNREAD_COUNT).append(": ").append(mData.unreadCount).append(", ");
        sb.append(CN_COLOR).append(": ").append(mColor).append(", ");
        if (mData.flags != 0)
            sb.append(CN_FLAGS).append(": ").append(getFlagString()).append(", ");
        if (mData.tags != 0)
            sb.append(CN_TAGS).append(": [").append(getTagString()).append("], ");
        if (mData.subject != null)
            sb.append(CN_SUBJECT).append(": ").append(mData.subject).append(", ");
        if (mData.children != null)
            sb.append(CN_CHILDREN).append(": [").append(mData.children.toString()).append("], ");
        if (mData.blobDigest != null)
            sb.append(CN_BLOB_DIGEST).append(": ").append(mData.blobDigest);
        if (mData.imapId > 0)
            sb.append(CN_IMAP_ID).append(": ").append(mData.imapId).append(", ");
        return sb;
    }
}
