/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.mailbox;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zimbra.common.mailbox.Color;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.mailbox.MailItemType;
import com.zimbra.common.mailbox.ZimbraMailItem;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.EmailUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.SystemUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbPendingAclPush;
import com.zimbra.cs.db.DbTag;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata.CustomMetadataList;
import com.zimbra.cs.mailbox.MailItemState.AccessMode;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.MailboxIndex.ItemIndexDeletionInfo;
import com.zimbra.cs.mailbox.util.TypedIdList;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.StagedBlob;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.volume.Volume;

/**
 * @since Aug 12, 2004
 */
public abstract class MailItem implements Comparable<MailItem>, ScheduledTaskResult, ZimbraMailItem {

    public enum Type {
        UNKNOWN(-1, MailItemType.UNKNOWN),
        /** Item type is unknown, but cannot be a folder or tag */
        UNKNOWN_SEARCHABLE(-2, MailItemType.UNKNOWN),
        /** Item is a standard {@link Folder}. */
        FOLDER(1, MailItemType.FOLDER),
        /** Item is a saved search {@link SearchFolder}. */
        SEARCHFOLDER(2, MailItemType.SEARCHFOLDER),
        /** Item is a user-created {@link Tag}. */
        TAG(3, MailItemType.TAG),
        /** Item is a real, persisted {@link Conversation}. */
        CONVERSATION(4, MailItemType.CONVERSATION),
        /** Item is a mail {@link Message}. */
        MESSAGE(5, MailItemType.MESSAGE),
        /** Item is a {@link Contact}. */
        CONTACT(6, MailItemType.CONTACT),
        /** Item is a {@link InviteMessage} with a {@code text/calendar} MIME part. */
        @Deprecated INVITE(7, MailItemType.INVITE),
        /** Item is a bare {@link Document}. */
        DOCUMENT(8, MailItemType.DOCUMENT),
        /** Item is a {@link Note}. */
        NOTE(9, MailItemType.NOTE),
        /** Item is a memory-only system {@link Flag}. */
        FLAG(10, MailItemType.FLAG),
        /** Item is a calendar {@link Appointment}. */
        APPOINTMENT(11, MailItemType.APPOINTMENT),
        /** Item is a memory-only, 1-message {@link VirtualConversation}. */
        VIRTUAL_CONVERSATION(12, MailItemType.VIRTUAL_CONVERSATION),
        /** Item is a {@link Mountpoint} pointing to a {@link Folder}, possibly in another user's {@link Mailbox}. */
        MOUNTPOINT(13, MailItemType.MOUNTPOINT),
        /** Item is a {@link WikiItem} */
        @Deprecated WIKI(14, MailItemType.WIKI),
        /** Item is a {@link Task} */
        TASK(15, MailItemType.TASK),
        /** Item is a {@link Chat} */
        CHAT(16, MailItemType.CHAT),
        /** Item is a {@link Comment} */
        COMMENT(17, MailItemType.COMMENT),
        /** Item is a {@link Link} pointing to a {@link Document} */
        LINK(18, MailItemType.LINK),
        /** Item is a {@link SmartFolder} */
        SMARTFOLDER(19, MailItemType.SMARTFOLDER);

        private static final Map<Byte, Type> BYTE2TYPE;
        static {
            ImmutableMap.Builder<Byte, Type> builder = ImmutableMap.builder();
            for (Type type : Type.values()) {
                builder.put(type.toByte(), type);
            }
            BYTE2TYPE = builder.build();
        }

        private final byte btype;
        private final MailItemType miType;

        private Type(int b, MailItemType mit) {
            btype = (byte) b;
            miType = mit;
        }

        public byte toByte() {
            return btype;
        }

        /**
         * Returns the human-readable name (e.g. "tag") for the item type.
         */
        @Override
        public String toString() {
            return name().toLowerCase();
        }

        public static String toString(Set<Type> types) {
            return Joiner.on(',').skipNulls().join(types);
        }

        public static Type of(byte b) {
            Type result = BYTE2TYPE.get(b);
            return result != null ? result : UNKNOWN;
        }

        public static Type fromCommon(MailItemType commGT) {
            for (Type typ :Type.values()) {
                if (typ.miType == commGT) {
                    return typ;
                }
            }
            throw new IllegalArgumentException("Unrecognised MailItemType:" + commGT);
        }

        public MailItemType toCommon() {
            return miType;
        }

        public static Set<MailItemType> toCommon(Set<Type> typs) {
            if (null == typs) {
                return Collections.emptySet();
            }
            Set<MailItemType> mits = Sets.newHashSetWithExpectedSize(typs.size());
            for (Type typ : typs) {
                mits.add(typ.miType);
            }
            return mits;
        }

        public static Set<Type> fromCommon(Set<MailItemType> mits) {
            if (null == mits) {
                return Collections.emptySet();
            }
            Set<Type> types = Sets.newHashSetWithExpectedSize(mits.size());
            for (MailItemType mit : mits) {
                types.add(Type.fromCommon(mit));
            }
            return types;
        }

        /**
         * Returns the item type for the specified human-readable type name.
         *
         * @param name string representation of a type
         * @return type
         */
        public static Type of(String name) {
            if (Strings.isNullOrEmpty(name)) {
                return UNKNOWN;
            }
            try {
                return Type.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException e) {
                if ("briefcase".equalsIgnoreCase(name)) { // synonym of document
                    return DOCUMENT;
                } else {
                    return UNKNOWN;
                }
            }
        }

        /**
         * Parses a CSV of type names.
         *
         * @param csv comma-separated types
         * @return set of types
         * @throws IllegalArgumentException if the CSV contains an invalid type name
         */
        public static Set<Type> setOf(String csv) {
            Set<Type> result = EnumSet.noneOf(Type.class);
            for (String token : Splitter.on(',').trimResults().split(csv)) {
                Type type = Type.of(token);
                if (type != UNKNOWN) {
                    result.add(type);
                } else {
                    throw new IllegalArgumentException(token);
                }
            }
            return result;
        }

        public boolean isLeafNode() {
            switch (this) {
                case FOLDER:
                case SEARCHFOLDER:
                case MOUNTPOINT:
                case FLAG:
                case TAG:
                case SMARTFOLDER:
                case CONVERSATION:
                case VIRTUAL_CONVERSATION:
                case UNKNOWN:
                    return false;
                default:
                    return true;
            }
        }
    }

    public static final int FLAG_UNCHANGED = 0x80000000;
    public static final int MAX_FLAG_COUNT = 31;

    public static final String[] TAG_UNCHANGED = null;

    public static final byte DEFAULT_COLOR = 0;
    public static final Color DEFAULT_COLOR_RGB = new Color(DEFAULT_COLOR);

    public enum IndexStatus {
        /** Not indexable. */
        NO(-1),
        /** Not indexed yet (add only). */
        DEFERRED(0),
        /** Not re-indexed yet (delete & add). */
        STALE(1),
        /** Indexed. */
        DONE(Integer.MAX_VALUE);

        private final int id;

        private IndexStatus(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        static IndexStatus of(int id) {
            switch (id) {
                case -1:
                    return NO;
                case 0:
                    return DEFERRED;
                case 1:
                    return STALE;
                default:
                    return DONE;
            }
        }
    }

    public static final class UnderlyingData implements Cloneable {
        public int id;
        public byte type;
        public int parentId = -1;
        public int folderId = -1;
        private String prevFolders; /* semicolon separated modseq to prev_folderId mappings */
        public int indexId  = IndexStatus.NO.id();
        public int imapId   = -1;
        public String locator;
        private String blobDigest;
        public int date; /* Seconds since 1970-01-01 00:00:00 UTC */
        public long size;
        public int unreadCount;
        private int flags;
        private String[] tags = NO_TAGS;
        private String[] smartFolders = NO_TAGS;
        private String subject;
        public String name;
        public String metadata;
        public int modMetadata;
        public int dateChanged; /* Seconds since 1970-01-01 00:00:00 UTC */
        public int modContent;
        public String uuid;
        public byte eventFlag;

        public String getSubject() {
            return subject;
        }

        public UnderlyingData setSubject(String value) {
            this.subject = DbMailItem.normalize(value, DbMailItem.MAX_SUBJECT_LENGTH);
            return this;
        }

        public String getPrevFolders() {
            return prevFolders;
        }

        public UnderlyingData setPrevFolders(String value) {
            this.prevFolders = value;
            return this;
        }

        /** Returns the item's blob digest, or <tt>null</tt> if the item has no blob. */
        public String getBlobDigest() {
            return blobDigest;
        }

        public UnderlyingData setBlobDigest(String digest) {
            this.blobDigest = "".equals(digest) ? null : digest;
            return this;
        }

        public boolean isUnread() {
            return (unreadCount > 0);
        }

        public int getFlags() {
            return flags;
        }

        public UnderlyingData setFlag(Flag flag) {
            return setFlags(flags | flag.toBitmask());
        }

        public UnderlyingData setFlag(Flag.FlagInfo flag) {
            return setFlags(flags | flag.toBitmask());
        }

        public UnderlyingData unsetFlag(Flag flag) {
            return setFlags(flags & ~flag.toBitmask());
        }

        public UnderlyingData unsetFlag(Flag.FlagInfo flag) {
            return setFlags(flags & ~flag.toBitmask());
        }

        /** Sets all flags to the values specified in the given bit field. */
        public UnderlyingData setFlags(int bitfield) {
            assert (bitfield & ~Flag.FLAGS_ALL) == 0 : "Invalid flag bitfield: " + bitfield;
            this.flags = bitfield & Flag.FLAGS_ALL;
            return this;
        }

        public boolean isSet(Flag.FlagInfo flag) {
            return flag != null && (flags & flag.toBitmask()) != 0;
        }

        private static final String[] NO_TAGS = new String[0];

        public UnderlyingData setTags(Tag.NormalizedTags ntags) {
            if (ntags != null) {
                this.tags = Arrays.stream(ntags.getTags()).filter(t -> !t.startsWith(Tag.SMARTFOLDER_NAME_PREFIX)).toArray(String[]::new);
                this.smartFolders = Arrays.stream(ntags.getTags()).filter(t -> t.startsWith(Tag.SMARTFOLDER_NAME_PREFIX)).toArray(String[]::new);
            } else {
                this.tags = NO_TAGS;
                this.smartFolders = NO_TAGS;
            }
            return this;
        }

        public String[] getTags() {
            return tags;
        }

        public String[] getSmartFolders() {
            return smartFolders;
        }

        public void setTags(String[] tags) {
            this.tags = tags == null ? NO_TAGS : tags;
        }

        public void setSmartFolders(String[] smartFolders) {
            this.smartFolders = smartFolders == null ? NO_TAGS : smartFolders;
        }

        UnderlyingData duplicate(int newId, String newUuid, int newFolder, String newLocator) {
            UnderlyingData data = new UnderlyingData();
            data.id = newId;
            data.uuid = newUuid;
            data.type = this.type;
            data.parentId = this.parentId;
            data.folderId = newFolder;
            data.indexId = this.indexId;
            data.imapId = this.imapId <= 0 ? this.imapId : newId;
            data.locator = newLocator;
            data.blobDigest = this.blobDigest;
            data.date = this.date;
            data.size = this.size;
            data.flags = this.flags;
            data.tags = this.tags;
            data.subject = this.subject;
            data.name = this.name;
            data.unreadCount = this.unreadCount;
            data.eventFlag = this.eventFlag;
            // TODO: What about smartFolders?  Currently not supported though.
            return data;
        }

        @Override
        protected UnderlyingData clone() {
            try {
                return (UnderlyingData) super.clone();
            } catch (CloneNotSupportedException cnse) {
                return null;
            }
        }

        void metadataChanged(Mailbox mbox, boolean updateFolderMODSEQ) throws ServiceException {
            modMetadata = mbox.getOperationChangeID();
            dateChanged = mbox.getOperationTimestamp();
            if (updateFolderMODSEQ && !isAcceptableType(Type.FOLDER, Type.of(type)) && !isAcceptableType(Type.TAG, Type.of(type))) {
                mbox.getFolderById(folderId).updateHighestMODSEQ();
            }
        }

        void contentChanged(Mailbox mbox, boolean updateFolderMODSEQ) throws ServiceException {
            metadataChanged(mbox, updateFolderMODSEQ);
            modContent = modMetadata;
        }

        void contentChanged(Mailbox mbox) throws ServiceException {
            contentChanged(mbox, true);
        }

        private static final String FN_ID           = "id";
        private static final String FN_UUID         = "uuid";
        private static final String FN_TYPE         = "tp";
        private static final String FN_PARENT_ID    = "pid";
        private static final String FN_FOLDER_ID    = "fid";
        private static final String FN_PREV_FOLDER  = "pfid";
        private static final String FN_INDEX_ID     = "idx";
        private static final String FN_IMAP_ID      = "imap";
        private static final String FN_LOCATOR      = "loc";
        private static final String FN_BLOB_DIGEST  = "dgst";
        private static final String FN_DATE         = "dt";
        private static final String FN_SIZE         = "sz";
        private static final String FN_UNREAD_COUNT = "uc";
        private static final String FN_FLAGS        = "fg";
        private static final String FN_TAGS         = "tg";
        private static final String FN_SUBJECT      = "sbj";
        private static final String FN_NAME         = "nm";
        private static final String FN_METADATA     = "meta";
        private static final String FN_MOD_METADATA = "modm";
        private static final String FN_MOD_CONTENT  = "modc";
        private static final String FN_DATE_CHANGED = "dc";
        private static final String FN_EVENT_FLAG   = "ef";

        public Metadata serialize() {
            Metadata meta = new Metadata();
            meta.put(FN_ID, id);
            meta.put(FN_TYPE, type);
            meta.put(FN_PARENT_ID, parentId);
            meta.put(FN_FOLDER_ID, folderId);
            meta.put(FN_PREV_FOLDER, prevFolders);
            meta.put(FN_INDEX_ID, indexId);
            meta.put(FN_IMAP_ID, imapId);
            meta.put(FN_LOCATOR, locator);
            meta.put(FN_BLOB_DIGEST, blobDigest);
            meta.put(FN_DATE, date);
            meta.put(FN_SIZE, size);
            meta.put(FN_UNREAD_COUNT, unreadCount);
            meta.put(FN_FLAGS, flags);
            meta.put(FN_TAGS, DbTag.serializeTags(tags));
            meta.put(FN_SUBJECT, subject);
            meta.put(FN_NAME, name);
            meta.put(FN_METADATA, metadata);
            meta.put(FN_MOD_METADATA, modMetadata);
            meta.put(FN_MOD_CONTENT, modContent);
            meta.put(FN_UUID, uuid);
            meta.put(FN_DATE_CHANGED, dateChanged);
            meta.put(FN_EVENT_FLAG, eventFlag);
            // TODO: What about smartFolders?  Currently not supported though.
            return meta;
        }

        public void deserialize(Metadata meta) throws ServiceException {
            this.id = (int) meta.getLong(FN_ID, 0);
            this.type = (byte) meta.getLong(FN_TYPE, 0);
            this.parentId = (int) meta.getLong(FN_PARENT_ID, -1);
            this.folderId = (int) meta.getLong(FN_FOLDER_ID, -1);
            this.prevFolders = meta.get(FN_PREV_FOLDER, null);
            this.indexId = meta.getInt(FN_INDEX_ID, IndexStatus.NO.id());
            this.imapId = (int) meta.getLong(FN_IMAP_ID, -1);
            this.locator = meta.get(FN_LOCATOR, null);
            this.blobDigest = meta.get(FN_BLOB_DIGEST, null);
            this.date = (int) meta.getLong(FN_DATE, 0);
            this.size = meta.getLong(FN_SIZE, 0);
            this.unreadCount = (int) meta.getLong(FN_UNREAD_COUNT, 0);
            setFlags((int) meta.getLong(FN_FLAGS, 0));
            // are the tags ever non-null? we're assuming that they aren't...
            setTags(new Tag.NormalizedTags(DbTag.deserializeTags(meta.get(FN_TAGS, null))));
            this.subject = meta.get(FN_SUBJECT, null);
            this.name = meta.get(FN_NAME, null);
            this.metadata = meta.get(FN_METADATA, null);
            this.modMetadata = (int) meta.getLong(FN_MOD_METADATA, 0);
            this.modContent = (int) meta.getLong(FN_MOD_CONTENT, 0);
            this.uuid = meta.get(FN_UUID, null);
            this.dateChanged = (int) meta.getLong(FN_DATE_CHANGED, 0);
            this.eventFlag = (byte) meta.getInt(FN_EVENT_FLAG, 0);
            // TODO: What about smartFolders?  Currently not supported though.
        }

        @Override
        public String toString() {
            MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(this)
                    .add("id", id)
                    .add("type", Type.of(type))
                    .add("size", size)
                    .add("date", date)
                    .add("dateChanged", dateChanged)
                    .add("subject", subject)
                    .add("name", name)
                    .add("unreadCount", unreadCount)
                    .add("flags", flags)
                    .add("prevFolders", prevFolders)
                    .add("locator", locator)
                    .add("modMetadata", modMetadata)
                    .add("modContent", modContent)
                    .add("uuid", uuid)
                    .add("eventFlag", eventFlag);
            if (imapId != -1) { helper.add("imapId", imapId); }
            if (parentId != -1) { helper.add("parentId", parentId); }
            if (folderId != -1) { helper.add("folderId", folderId); }
            if (indexId != IndexStatus.NO.id()) { helper.add("indexId", indexId); }
            if (tags != null && tags != NO_TAGS) { helper.add("tags", Lists.newArrayList(tags)); }
            if (smartFolders != null && smartFolders != NO_TAGS) { helper.add("smartFolders",
                    Lists.newArrayList(smartFolders)); }
            return helper.omitNullValues().toString();
        }
    }

    public static final class TargetConstraint {
        public static final short INCLUDE_TRASH  = 0x01;
        public static final short INCLUDE_SPAM   = 0x02;
        public static final short INCLUDE_SENT   = 0x04;
        public static final short INCLUDE_DRAFTS = 0x08;
        public static final short INCLUDE_OTHERS = 0x20;
        public static final short INCLUDE_QUERY  = 0x10;
        private static final short ALL_LOCATIONS = INCLUDE_TRASH | INCLUDE_SPAM | INCLUDE_SENT | INCLUDE_DRAFTS | INCLUDE_OTHERS;

        private static final char ENC_TRASH = 't';
        private static final char ENC_SPAM  = 'j';
        private static final char ENC_SENT  = 's';
        private static final char ENC_OTHER = 'o';
        private static final char ENC_QUERY = 'q';
        private static final char ENC_DRAFTS = 'd';

        private short inclusions;
        private String query;

        private final Mailbox mailbox;
        private int sentFolder = -1;

        public TargetConstraint(Mailbox mbox, short include) {
            this(mbox, include, null);
        }

        public TargetConstraint(Mailbox mbox, String includeQuery) {
            this(mbox, INCLUDE_QUERY, includeQuery);
        }

        public TargetConstraint(Mailbox mbox, short include, String includeQuery) {
            mailbox = mbox;
            if (includeQuery == null || includeQuery.trim().length() == 0) {
                inclusions = (short) (include & ~INCLUDE_QUERY);
            } else {
                inclusions = (short) (include | INCLUDE_QUERY);
                query = includeQuery;
            }
        }

        public static TargetConstraint parseConstraint(Mailbox mbox, String encoded) throws ServiceException {
            if (encoded == null) {
                return null;
            }

            boolean invert = false;
            short inclusions = 0;
            String query = null;
            loop: for (int i = 0; i < encoded.length(); i++) {
                switch (encoded.charAt(i)) {
                    case ENC_TRASH:  inclusions |= INCLUDE_TRASH;       break;
                    case ENC_SPAM:   inclusions |= INCLUDE_SPAM;        break;
                    case ENC_SENT:   inclusions |= INCLUDE_SENT;        break;
                    case ENC_DRAFTS:  inclusions |= INCLUDE_DRAFTS;     break;
                    case ENC_OTHER:  inclusions |= INCLUDE_OTHERS;      break;
                    case ENC_QUERY:  inclusions |= INCLUDE_QUERY;
                                     query = encoded.substring(i + 1);  break loop;
                    case '-':  if (i == 0 && encoded.length() > 1)  { invert = true;  break; }
                        // fall through...
                    default:  throw ServiceException.INVALID_REQUEST("invalid encoded constraint: " + encoded, null);
                }
            }
            if (invert) {
                inclusions ^= ALL_LOCATIONS;
            }
            return new TargetConstraint(mbox, inclusions, query);
        }

        @Override
        public String toString() {
            if (inclusions == 0) {
                return "";
            }

            StringBuilder sb = new StringBuilder();
            if ((inclusions & INCLUDE_TRASH) != 0) {
                sb.append(ENC_TRASH);
            }
            if ((inclusions & INCLUDE_SPAM) != 0) {
                sb.append(ENC_SPAM);
            }
            if ((inclusions & INCLUDE_SENT) != 0) {
                sb.append(ENC_SENT);
            }
            if ((inclusions & INCLUDE_DRAFTS) != 0) {
                sb.append(ENC_DRAFTS);
            }
            if ((inclusions & INCLUDE_OTHERS) != 0) {
                sb.append(ENC_OTHER);
            }
            if ((inclusions & INCLUDE_QUERY) != 0) {
                sb.append(ENC_QUERY).append(query);
            }
            return sb.toString();
        }

        public static boolean checkItem(TargetConstraint tcon, MailItem item) throws ServiceException {
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
            if ((inclusions & INCLUDE_DRAFTS) != 0 && item.inDrafts())
                return true;
            if ((inclusions & INCLUDE_OTHERS) != 0 && !item.inTrash() && !item.inSpam() && !inSent(item) && !item.inDrafts())
                return true;
            return false;
        }

        /** Returns whether an item is in the user's sent folder.  Returns
         *  <tt>false</tt> if the user has set their sent folder to be
         *  any folder other than the default "/Sent" folder, folder 5.<p>
         *
         *  The reason we don't just compare the item's folder against the
         *  user's configured sent folder is that when the user sets their
         *  sent folder to be "/Inbox", *all* Inbox messages will be skipped
         *  when the "sent" folder is excluded via tcon, which is not what
         *  we want.  See bug 3972 for details. */
        private boolean inSent(MailItem item) {
            // only count as "in sent" if the item's in the real "/Sent" folder
            if (item.getFolderId() != Mailbox.ID_FOLDER_SENT) {
                return false;
            }

            if (sentFolder == -1) {
                sentFolder = Mailbox.ID_FOLDER_SENT;
                try {
                    String sent = mailbox.getAccount().getAttr(Provisioning.A_zimbraPrefSentMailFolder, null);
                    if (sent != null) {
                        sentFolder = mailbox.getFolderByPath(null, sent).getId();
                    }
                } catch (ServiceException e) { }
            }
            // only count as "in sent" if the user's sent folder is 5 and
            //   the item's in there
            return sentFolder == Mailbox.ID_FOLDER_SENT && sentFolder == item.getFolderId();
            // return sentFolder == item.getFolderId();
        }
    }

    public static final class CustomMetadata extends HashMap<String, String> {
        private static final long serialVersionUID = -3866150929202858077L;

        private final String sectionKey;

        public CustomMetadata(String section) {
            super(8);
            this.sectionKey = section.trim();
        }

        public CustomMetadata(String section, String serialized) throws ServiceException {
            this(section);

            if (!StringUtil.isNullOrEmpty(serialized)) {
                for (Map.Entry<String, ?> entry : new Metadata(serialized).asMap().entrySet()) {
                    put(entry.getKey(), entry.getValue().toString());
                }
            }
        }

        public String getSectionKey() {
            return sectionKey;
        }

        public String getSerializedValue() {
            remove(null);
            return new Metadata(this).toString();
        }

        @Override
        public String toString() {
            return sectionKey + ": " + super.toString();
        }

        public CustomMetadataList asList() {
            return isEmpty() ? null : new CustomMetadataList(this);
        }

        public static final class CustomMetadataList extends ArrayList<Pair<String, String>> {
            private static final long serialVersionUID = 3213399133413270157L;

            public CustomMetadataList() {
                super(1);
            }

            public CustomMetadataList(CustomMetadata custom) {
                this();
                addSection(custom);
            }

            public void addSection(CustomMetadata custom) {
                if (custom.isEmpty()) {
                    removeSection(custom.getSectionKey());
                } else {
                    addSection(custom.getSectionKey(), custom.getSerializedValue());
                }
            }

            public void addSection(String key, String encoded) {
                removeSection(key);
                if (key != null && encoded != null) {
                    add(new Pair<String, String>(key, encoded));
                }
            }

            public CustomMetadata getSection(String key) throws ServiceException {
                if (!isEmpty()) {
                    for (Pair<String, String> entry : this) {
                        if (key.equals(entry.getFirst())) {
                            return new CustomMetadata(entry.getFirst(), entry.getSecond());
                        }
                    }
                }
                return null;
            }

            public List<String> listSections() {
                List<String> sections = new ArrayList<String>(size());
                for (Pair<String, String> entry : this) {
                    sections.add(entry.getFirst());
                }
                return sections;
            }

            public void removeSection(String key) {
                if (key != null && !isEmpty()) {
                    for (Iterator<Pair<String, String>> it = iterator(); it.hasNext(); ) {
                        if (key.equals(it.next().getFirst())) {
                            it.remove();
                        }
                    }
                }
            }

            public long guessSize() {
                long size = 0;
                if (!isEmpty()) {
                    for (Pair<String, String> entry : this) {
                        size += entry.getFirst().length() + entry.getSecond().length();
                    }
                }
                return size;
            }
        }
    }

    protected int            mId;
    protected String         uuid;
    protected byte           type;
    protected Mailbox        mMailbox;
    protected Mailbox.MailboxData  mMailboxData;
    protected Account        mAccount;
    protected MailboxBlob    mBlob;
    protected List<MailItem> mRevisions;
    protected CustomMetadataList mExtendedData;
    protected final MailItemState state;

    MailItem(Mailbox mbox, UnderlyingData data) throws ServiceException {
        this(mbox, data, false);
    }

    MailItem(Mailbox mbox, UnderlyingData data, boolean skipCache) throws ServiceException {
        if (data == null) {
            throw new IllegalArgumentException();
        }
        mId      = data.id;
        uuid     = data.uuid;
        type     = data.type;
        if (mbox != null) {
            mMailboxData = mbox.getData();
            mAccount = mbox.getAccount();
            mMailbox = mbox;
        }
        state = initFieldCache(data);
        decodeMetadata(data.metadata);
        checkItemCreationAllowed(); // this check may rely on decoded metadata
        data.metadata = null;

        if (!skipCache && ((data.getFlags() & Flag.BITMASK_UNCACHED) == 0)) {
            mbox.cache(this); // store the item in the mailbox's cache
        }
    }

    MailItem(Account acc, UnderlyingData data, int mboxId) throws ServiceException {
        if (data == null) {
            throw new IllegalArgumentException();
        }
        mMailboxData = new Mailbox.MailboxData();
        mMailboxData.accountId = acc.getId();
        mMailboxData.id = mboxId;
        mAccount = acc;
        mId      = data.id;
        uuid     = data.uuid;
        type     = data.type;
        state = initFieldCache(data);
        decodeMetadata(data.metadata);
        checkItemCreationAllowed(); // this check may rely on decoded metadata
        data.metadata = null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof MailItem) {
            MailItem item = (MailItem) obj;
            return mId == item.mId && StringUtil.equal(mAccount.getId(), item.mAccount.getId());
        }
        return false;
    }

    protected void checkItemCreationAllowed() throws ServiceException {
        // not allowed in external account mailbox
        if (getAccount().isIsExternalVirtualAccount()) {
            throw ServiceException.PERM_DENIED("permission denied for external account");
        }
    }

    /** Returns the item's ID.  IDs are unique within a {@link Mailbox} and
     *  are assigned in increasing (though not necessarily gap-free) order. */
    public int getId() {
        return mId;
    }

    /** Returns the item's ID.  IDs are unique within a {@link Mailbox} and
     *  are assigned in increasing (though not necessarily gap-free) order. */
    @Override
    public int getIdInMailbox() {
        return getId();
    }

    /** Returns the item's UUID.  UUIDs are globally unique. */
    public String getUuid() {
        return uuid;
    }

    /** Returns the item's type. */
    public Type getType() {
        return Type.of(type);
    }

    @Override
    public MailItemType getMailItemType() {
        return Type.of(type).toCommon();
    }

    /** Returns the numeric ID of the {@link Mailbox} this item belongs to. */
    public int getMailboxId() {
        return mMailboxData.id;
    }

    /** Returns the {@link Mailbox} this item belongs to. */
    public Mailbox getMailbox() throws ServiceException {
        if(mMailbox != null) {
            return mMailbox;
        } else if(mMailboxData != null) {
            mMailbox = MailboxManager.getInstance().getMailboxById(mMailboxData.id);
        }
        if(mMailbox == null ){
            throw ServiceException.NOT_FOUND("Failed to find Mailbox for MailItem or MailItem object was not properly initialized");
        }
        return mMailbox;
    }

    /** Returns the {@link Account} this item's Mailbox belongs to. */
    public Account getAccount() throws ServiceException {
        return mAccount;
    }

    /** Returns the item's color.  If not specified, defaults to
     *  {@link #DEFAULT_COLOR}.  No "color inheritance" (e.g. from the
     *  item's folder or tags) is performed. */
    public byte getColor() {
        return state.getColor().getMappedColor();
    }

    /** Returns the item's color represented in RGB. */
    public Color getRgbColor() {
        return state.getColor();
    }

    /** Returns the item's name.  If the item doesn't have a name (e.g.
     *  messages, contacts, appointments), returns <tt>""</tt>.
     *  If not <tt>""</tt>, this name should be unique across all item
     *  types within the parent folder. */
    public String getName() {
        String name = state.getName();
        return name == null ? "" : StringUtil.trimTrailingSpaces(name);
    }

    /** Returns the ID of the item's parent.  Not all items have parents;
     *  some that do include {@link Message} (parent is {@link Conversation})
     *  and {@link Folder} (parent is Folder). */
    public int getParentId() {
        return state.getParentId();
    }

    /** Returns the ID of the {@link Folder} the item lives in.  All items
     *  must have a non-<tt>null</tt> folder. */
    public int getFolderId() {
        return state.getFolderId();
    }

    /** Return (modseq->previous folder id) pair separated by semocolon
     */
    public String getPrevFolders() {
        return state.getPrevFolders();
    }

    /**Returns the ID of the {@link Folder} the item lived in at given mod sequence.
     */
    public int getPrevFolderAtModseq(int modseq) {
        if (StringUtil.isNullOrEmpty(getPrevFolders())) {
            return -1;
        }
        String[] modseq2FolderId = getPrevFolders().split(";"); //modseq from low to high
        if (modseq2FolderId.length > 0) {
            int index = 0;
            try {
                while (index < modseq2FolderId.length
                        && index < this.getAccount().getServer().getPrevFoldersToTrackMax()) {
                    String md2id = modseq2FolderId[index++];
                    String[] pair = md2id.split(":");
                    int md = Integer.parseInt(pair[0]);
                    if (modseq < md) {
                        return Integer.parseInt(pair[1]);
                    }
                }
            } catch (Exception e) {}
        }
        return -1;
    }

    public String getFolderUuid() throws ServiceException {
        Folder f = getFolder();
        return f != null ? f.getUuid() : null;
    }

    /** Returns the path to the MailItem.  If the item is in a hidden folder
     *  or is of a type that does not have a name (e.g. {@link Message}s,
     *  {@link Contact}s, etc.), this method returns <tt>null</tt>. */
    public String getPath() throws ServiceException {
        Folder folder = !inDumpster() ? getFolder() : getMailbox().getFolderById(Mailbox.ID_FOLDER_TRASH);
        if (folder == null)
            return null;

        String path = folder.getPath(), name = getName();
        if (name == null || path == null)
            return null;
        return path + (path.endsWith("/") ? "" : "/") + name;
    }

    /** Returns the ID the item is referenced by in the index.  Returns -1
     *  for non-indexed items.  For indexed items, the "index ID" will be the
     *  same as the item ID unless the item is a copy of another item; in that
     *  case, the "index ID" is the same as the original item's "index ID". */
    public int getIndexId() {
        return state.getIndexId();
    }

    public IndexStatus getIndexStatus() {
        return IndexStatus.of(getIndexId());
    }

    /**
     * @return the UID the item is referenced by in the IMAP server.  Returns <tt>0</tt> for items that require
     * renumbering because of moves.
     * The "IMAP UID" will be the same as the item ID unless the item has been moved after the mailbox owner's first
     * IMAP session. */
    @Override
    public int getImapUid() {
        return state.getImapId();
    }

    /** Returns the ID of the {@link Volume} the item's blob is stored on.
     *  Returns <tt>null</tt> for items that have no stored blob. */
    public String getLocator() {
        return state.getLocator();
    }

    /** Returns the SHA-1 hash of the item's uncompressed blob.
     *
     * @return the blob digest, or <tt>null</tt> if no blob exists */
    public String getDigest() {
        return state.getBlobDigest();
    }

    /** Returns the 1-based version number on the item's metadata.  Each time the item's
     *  metadata changes, this counter is incremented. */
    public int getMetadataVersion() {
        return state.getMetadataVersion();
    }

    /** Returns the 1-based version number on the item's content.  Each time the item's
     *  "content" changes (e.g. editing a {@link Document} or a draft), this
     *  counter is incremented. */
    public int getVersion() {
        return state.getVersion();
    }

    /** Returns the date the item's content was last modified as number of milliseconds since 1970-01-01 00:00:00 UTC.
     *  For immutable objects (e.g. received messages), this will be the same as the date the item was created. */
    @Override
    public long getDate() {
        return state.getDate() * 1000L;
    }

    /** Returns the change ID corresponding to the last time the item's
     *  content was modified.  For immutable objects (e.g. received messages),
     *  this will be the same change ID as when the item was created. */
    public int getSavedSequence() {
        return state.getModContent();
    }

    /** Returns the date the item's metadata and/or content was last modified as number of milliseconds since
     * 1970-01-01 00:00:00 UTC. Includes changes in tags and flags as well as folder-to-folder moves and recoloring. */
    public long getChangeDate() {
        return state.getDateChanged() * 1000L;
    }

    /** Returns the change ID corresponding to the last time the item's
     *  metadata and/or content was modified.  This includes changes in tags
     *  and flags as well as folder-to-folder moves and recoloring. */
    @Override
    public int getModifiedSequence() {
        return state.getModMetadata();
    }

    /** Returns the item's size as it counts against mailbox quota.  For items
     *  that have a blob, this is the size in bytes of the raw blob. */
    @Override
    public long getSize() {
        return state.getSize();
    }

    /** Returns the item's total count against mailbox quota including all old
     *  revisions.  For items that have a blob, this is the sum of the size in
     *  bytes of the raw blobs. */
    public long getTotalSize() throws ServiceException {
        long size = getSize();
        if (isTagged(Flag.FlagInfo.VERSIONED)) {
            for (MailItem revision : loadRevisions()) {
                size += revision.getSize();
            }
        }
        return size;
    }

    public String getSubject() {
        String subject = state.getSubject();
        return Strings.nullToEmpty(subject);
    }

    /** Returns the item's underlying storage data so that it may be persisted
     *  somewhere besides the database - usually in encoded form. */
    public UnderlyingData getUnderlyingData() throws ServiceException {
        UnderlyingData ud = state.getUnderlyingData();
        ud.metadata = encodeMetadata().toString();
        return ud;
    }

    public abstract String getSender();

    /** Returns the SORT-FORM (maybe truncated, etc.) of the subject of this mail item. */
    public String getSortSubject() {
        return getSubject();
    }

    /** Returns the SORT-FORM (maybe truncated) of the sender of this mail item. */
    public String getSortSender() {
        return getSender();
    }

    /** Returns the SORT-FORM (maybe truncated) of the recipients of this mail item. */
    public String getSortRecipients() {
        return null;
    }

    /** Returns the "external" flag bitmask, which includes
     *  {@link Flag#BITMASK_UNREAD} when the item is unread. */
    @Override
    public int getFlagBitmask() {
        int flags = state.getFlags();
        if (isUnread()) {
            flags = flags | Flag.BITMASK_UNREAD;
        }
        return flags;
    }

    public List<String> getCustomDataSections() {
        if (mExtendedData == null || mExtendedData.isEmpty()) {
            return Collections.emptyList();
        }
        return mExtendedData.listSections();
    }

    /** Returns the requested set of non-Zimbra-standard metadata values in
     *  the requested {@code section}.  If no set of custom metadata is
     *  associated with the {@code section}, returns <tt>null</tt>.
     * @see #setCustomData(CustomMetadata) */
    public CustomMetadata getCustomData(String section) throws ServiceException {
        if (section == null || mExtendedData == null) {
            return null;
        }
        return mExtendedData.getSection(section);
    }

    private static final int TOTAL_METADATA_LIMIT = 10000;

    /** Updates the requested set of non-Zimbra-standard metadata values in
     *  the requested section.  If the provided set of {@code custom}
     *  metdata contains no metadata key/value pairs, the section is deleted.
     * @see #getCustomData(String) */
    void setCustomData(CustomMetadata custom) throws ServiceException {
        if (custom == null)
            return;

        if (!canAccess(ACL.RIGHT_WRITE)) {
            throw ServiceException.PERM_DENIED("you do not have the necessary permissions on the item");
        }

        markItemModified(Change.METADATA);
        // first add the new section to the list
        if (mExtendedData != null) {
            mExtendedData.addSection(custom);
        } else if (!custom.isEmpty()) {
            mExtendedData = custom.asList();
        }
        // then check to make sure we're not overflowing our limit
        if (mExtendedData != null && !custom.isEmpty() && mExtendedData.guessSize() > TOTAL_METADATA_LIMIT) {
            throw MailServiceException.TOO_MUCH_METADATA(TOTAL_METADATA_LIMIT);
        }
        // and finally write the new data to the database
        saveMetadata();
    }


    /** Returns the "internal" flag bitmask, which does not include
     *  {@link Flag#BITMASK_UNREAD} and {@link Flag#BITMASK_IN_DUMPSTER}.
     *  This is the same bitmask as is stored in the database's {@code
     *  MAIL_ITEM.FLAGS} column. */
    public int getInternalFlagBitmask() {
        int flags = state.getFlags();
        return flags & ~Flag.BITMASK_IN_DUMPSTER;
    }

    /** Returns the external string representation of this item's flags.
     *  This string includes the state of {@link Flag#BITMASK_UNREAD} and is
     *  formed by concatenating the appropriate {@link Flag#FLAG_REP}
     *  characters for all flags set on the item. */
    public String getFlagString() {
        int flags = state.getFlags();
        if (flags == 0) {
            return isUnread() ? Flag.UNREAD_FLAG_ONLY : "";
        } else {
            return Flag.toString(flags | (isUnread() ? Flag.BITMASK_UNREAD : 0));
        }
    }

    @Override
    public String[] getTags() {
        String[] tags = state.getTags(), copy = tags.length == 0 ? tags : new String[tags.length];
        System.arraycopy(tags, 0, copy, 0, tags.length);
        return copy;
    }

    public String[] getSmartFolders() {
        String[] smartFolders = state.getSmartFolders(), copy = smartFolders.length == 0 ? smartFolders : new String[smartFolders.length];
        System.arraycopy(smartFolders, 0, copy, 0, smartFolders.length);
        return copy;
    }

    public boolean isTagged(Flag.FlagInfo finfo) {
        return state.isSet(finfo);
    }

    public boolean isTagged(Tag tag) {
        if (tag instanceof Flag) {
            return (getFlagBitmask() & ((Flag) tag).toBitmask()) != 0;
        } else {
            return Arrays.asList(getTags()).contains(tag.getName());
        }
    }

    public boolean isTagged(String tagName) {
        if (StringUtil.isNullOrEmpty(tagName)) {
            return false;
        } else if (tagName.startsWith(Tag.FLAG_NAME_PREFIX)) {
            return state.isSet(Flag.FlagInfo.of(tagName));
        } else {
            return Arrays.asList(getTags()).contains(tagName);
        }
    }

    /** Returns whether the given flag bitmask applies to the object.<p>
     *
     *  Equivalent to {@code ((getFlagBitmask() & <b>mask</b>) != 0)}. */
    boolean isFlagSet(long mask) {
        return ((getFlagBitmask() & mask) != 0);
    }

    /** Returns whether the item's unread count is >0.
     * @see #getUnreadCount() */
    public boolean isUnread() {
        return getUnreadCount() > 0;
    }

    /** Returns the item's unread count.  For "leaf items", this will be either
     *  <tt>0</tt> or <tt>1</tt>; for aggregates like {@link Folder}s and
     *  {@link Tag}s and {@link Conversation}s, it's the total number of unread
     *  aggregated "leaf items".  {@link Mountpoint}s will always have an
     *  unread count of <tt>0</tt>. */
    public int getUnreadCount() {
        return state.getUnreadCount();
    }

    public boolean isFlagged() {
        return isTagged(Flag.FlagInfo.FLAGGED);
    }

    public boolean hasAttachment() {
        return isTagged(Flag.FlagInfo.ATTACHED);
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
        int folderId = getFolderId();
        if (folderId <= Mailbox.HIGHEST_SYSTEM_ID) {
            return (folderId == Mailbox.ID_FOLDER_TRASH);
        }
        Folder folder = mMailbox.getFolderById(null, getFolderId());
        return folder.inTrash();
    }

    /** Returns whether the item is in the Junk folder.  (The Junk folder
     *  may not have subfolders.) */
    public boolean inSpam() {
        return getFolderId() == Mailbox.ID_FOLDER_SPAM;
    }

    /** Returns whether the item is in the Drafts folder.  (The Drafts folder
     *  may not have subfolders.) */
    public boolean inDrafts() {
        return getFolderId() == Mailbox.ID_FOLDER_DRAFTS;
    }

    public boolean inDumpster() {
        return (getFlagBitmask() & Flag.BITMASK_IN_DUMPSTER) != 0;
    }


    /** Returns whether the caller has the requested access rights on this
     *  item.  The owner of the {@link Mailbox} has all rights on all items
     *  in the Mailbox, as do all admin accounts.  All other users must be
     *  explicitly granted access.  <i>(Tag sharing and negative rights not
     *  yet implemented.)</i>  The authenticated user is fetched from the
     *  transaction's {@link OperationContext} via a call to
     *  {@link Mailbox#getAuthenticatedAccount}.
     *
     * @param rightsNeeded  A set of rights (e.g. {@link ACL#RIGHT_READ}
     *                      and {@link ACL#RIGHT_DELETE}).
     * @throws ServiceException on errors fetching LDAP entries or
     *         retrieving the item's folder
     * @see ACL
     * @see Folder#checkRights(short, Account, boolean) */
    boolean canAccess(short rightsNeeded) throws ServiceException {
        return canAccess(rightsNeeded, mMailbox.getAuthenticatedAccount(), mMailbox.isUsingAdminPrivileges());
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
     * @param asAdmin       Whether to use admin priviliges (if any).
     * @throws ServiceException on errors fetching LDAP entries or
     *         retrieving the item's folder
     * @see ACL
     * @see Folder#canAccess(short) */
    boolean canAccess(short rightsNeeded, Account authuser, boolean asAdmin) throws ServiceException {
        if (rightsNeeded == 0) {
            return true;
        }
        return checkRights(rightsNeeded, authuser, asAdmin) == rightsNeeded;
    }

    private boolean isAllowedExternalDomain(String extUserEmail) throws ServiceException {
        if (!getAccount().isExternalShareDomainWhitelistEnabled()) {
            return true;
        }
        if (extUserEmail == null) {
            return false;
        }
        String[] localPartAndDomain = EmailUtil.getLocalPartAndDomain(extUserEmail);
        if (localPartAndDomain == null) {
            return false;
        }
        String extUserDomain = localPartAndDomain[1];
        String[] whitelistDomains = getAccount().getExternalShareWhitelistDomain();
        for (String domain : whitelistDomains) {
            if (domain.equalsIgnoreCase(extUserDomain)) {
                return true;
            }
        }
        return false;
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
     * @param asAdmin       Whether to use admin priviliges (if any).
     * @see ACL
     * @see Folder#checkRights(short, Account, boolean) */
    short checkRights(short rightsNeeded, Account authuser, boolean asAdmin) throws ServiceException {
        // authuser has full permission
        if (hasFullPermission(authuser))
            return rightsNeeded;
        // if it is an anonymous user, check is public sharing is enabled
        if (GuestAccount.ANONYMOUS_ACCT.equals(authuser) && !getAccount().isPublicSharingEnabled()) {
            return 0;
        }
        // if it is an external/guest user, do necessary checks
        if (authuser.isIsExternalVirtualAccount() &&
                (!getAccount().isExternalSharingEnabled() ||
                        !isAllowedExternalDomain(authuser.getExternalUserMailAddress()))) {
            return 0;
        }
        // check to see what access has been granted on the enclosing folder
        Folder folder = !inDumpster() ? getFolder() : getMailbox().getFolderById(Mailbox.ID_FOLDER_TRASH);
        short granted = 0;
        // leaf nodes rely on the parent folder's ACL for iwda rights
        // even if the ACL is explicitly set on the item, so just
        // combine rights on the item and parent together.  this works
        // because we don't have negative rights.
        if (isLeafNode() && isTagged(Flag.FlagInfo.NO_INHERIT)) {
            granted = (short)(checkACL(rightsNeeded, authuser, asAdmin) | folder.checkRights(rightsNeeded, authuser, asAdmin));
        } else if (isTagged(Flag.FlagInfo.NO_INHERIT)) {
            granted = checkACL(rightsNeeded, authuser, asAdmin);
        } else {
            granted = folder.checkRights(rightsNeeded, authuser, asAdmin);
        }
        short subset = (short) (granted & rightsNeeded);
        // FIXME: check to see what access has been granted on the item's tags
        //   granted |= getTags().getGrantedRights(rightsNeeded, authuser);
        // and see if the granted rights are sufficient
        if (ZimbraLog.acl.isTraceEnabled()) {
            ZimbraLog.acl.trace("checkRights '%s' returned=%s granted=%s needed=%s",
                    getPath(), subset, granted, rightsNeeded);
        }
        return subset;
    }

    /**
     * If the operating context is null or auth user is the owner of the mailbox
     * the user has full permission.
     *
     * @param authuser
     * @return
     */
    protected boolean hasFullPermission(Account authuser) {
        if (authuser == null || authuser.getId().equals(mMailbox.getAccountId()))
            return true;
        return false;
    }

    /** Returns the {@link MailboxBlob} corresponding to the item's on-disk
     *  representation.  If the item is memory- or database-only, returns
     *  <tt>null</tt>.
     *
     * @throws MailServiceException.NO_SUCH_BLOB if the file cannot be found.
     * @throws ServiceException
     * */
    public synchronized MailboxBlob getBlob() throws ServiceException {
        if (mBlob == null && getDigest() != null) {
            mBlob = StoreManager.getInstance().getMailboxBlob(this);
            if (mBlob == null) {
                throw MailServiceException.NO_SUCH_BLOB(mMailbox.getId(), mId, getSavedSequence());
            }
        }
        return mBlob;
    }

    /** Returns an {@link InputStream} of the raw, uncompressed content of the message.  This is the message body as
     * received via SMTP; no postprocessing has been performed to make opaque attachments (e.g. TNEF) visible.
     *
     * @return The data stream, or <tt>null</tt> if the item has no blob
     * @throws ServiceException when the message file does not exist.
     * @see #getMimeMessage()
     * @see #getContent() */
    @Override
    public InputStream getContentStream() throws ServiceException {
        if (getDigest() == null) {
            return null;
        }

        try {
            MailboxBlob mblob = getBlob();
            if (mblob == null) {
                throw ServiceException.FAILURE("missing blob for id: " + getId() + ", change: " + getModifiedSequence(), null);
            }
            return StoreManager.getInstance().getContent(mblob);
        } catch (IOException e) {
            String msg = String.format("Unable to get content for %s %d", getClass().getSimpleName(), getId());
            throw ServiceException.FAILURE(msg, e);
        }
    }

    /** Returns the raw, uncompressed content of the item's blob as a byte
     *  array.  For messages, this is the message body as received via SMTP;
     *  no postprocessing has been performed to make opaque attachments
     *  (e.g. TNEF) visible.  When possible, this content is cached in the
     *
     * @return The blob content, or <tt>null</tt> if the item has no blob.
     * @throws ServiceException when the blob file does not exist.
     * @see #getMimeMessage()
     * @see #getContentStream() */
    public byte[] getContent() throws ServiceException {
        if (getDigest() == null) {
            return null;
        }

        try {
            return ByteUtil.getContent(getContentStream(), (int) getSize());
        } catch (IOException e) {
            throw ServiceException.FAILURE("Unable to get content for item " + getId(), e);
        }
    }

    @Override public int compareTo(MailItem that) {
        if (this == that) {
            return 0;
        }
        return mId - that.getId();
    }

    public static final class SortIdAscending implements Comparator<MailItem> {
        @Override public int compare(MailItem m1, MailItem m2) {
            return m1.getId() - m2.getId();
        }
    }

    public static final class SortIdDescending implements Comparator<MailItem> {
        @Override public int compare(MailItem m1, MailItem m2) {
            return m2.getId() - m1.getId();
        }
    }

    public static final class SortModifiedSequenceAscending implements Comparator<MailItem> {
        @Override public int compare(MailItem m1, MailItem m2) {
            return m1.getModifiedSequence() - m2.getModifiedSequence();
        }
    }

    public static final class SortDateAscending implements Comparator<MailItem> {
        @Override public int compare(MailItem m1, MailItem m2) {
            long t1 = m1.getDate(), t2 = m2.getDate();

            if (t1 < t2)        return -1;
            else if (t1 == t2)  return 0;
            else                return 1;
        }
    }

    public static final class SortDateDescending implements Comparator<MailItem> {
        @Override public int compare(MailItem m1, MailItem m2) {
            long t1 = m1.getDate(), t2 = m2.getDate();

            if (t1 < t2)        return 1;
            else if (t1 == t2)  return 0;
            else                return -1;
        }
    }

    public static final class SortSizeAscending implements Comparator<MailItem> {
        @Override public int compare(MailItem m1, MailItem m2) {
            long t1 = m1.getSize(), t2 = m2.getSize();

            if (t1 < t2)        return -1;
            else if (t1 == t2)  return 0;
            else                return 1;
        }
    }

    public static final class SortSizeDescending implements Comparator<MailItem> {
        @Override public int compare(MailItem m1, MailItem m2) {
            long t1 = m1.getSize(), t2 = m2.getSize();

            if (t1 < t2)        return 1;
            else if (t1 == t2)  return 0;
            else                return -1;
        }
    }

    public static final class SortImapUid implements Comparator<MailItem> {
        @Override public int compare(MailItem m1, MailItem m2) {
            return m1.getImapUid() - m2.getImapUid();
        }
    }

    public static final class SortSubjectAscending implements Comparator<MailItem> {
        @Override public int compare(MailItem m1, MailItem m2) {
            return m1.getSubject().compareToIgnoreCase(m2.getSubject());
        }
    }

    public static final class SortSubjectDescending implements Comparator<MailItem> {
        @Override public int compare(MailItem m1, MailItem m2) {
            return -m1.getSubject().compareToIgnoreCase(m2.getSubject());
        }
    }

    public static abstract class SortNameNaturalOrder implements Comparator<MailItem> {
        private static class Name {
            public char[] buf;
            public int    pos;
            public int    len;

            public Name(String n) {
                buf = n.toCharArray();
                pos = 0;
                len = buf.length;
            }

            public char getChar() {
                if (pos < len)
                    return buf[pos];
                return 0;
            }

            public Name next() {
                if (pos < len)
                    pos++;
                return this;
            }
        }

        @Override
        public int compare(MailItem m1, MailItem m2) {
            if (m1.getName() == null) {
                return returnResult(1);
            } else if (m2.getName() == null) {
                return returnResult(-1);
            }
            return compareString(new Name(m1.getName()), new Name(m2.getName()));
        }

        public int compareString(Name n1, Name n2) {
            char first = n1.getChar();
            char second = n2.getChar();

            if (isDigit(first) && isDigit(second)) {
                return compareNumeric(n1, n2);
            } else if (first != second) {
                return returnResult(first - second);
            } else if (first == 0 && second == 0) {
                return 0;
            }

            return compareString(n1.next(), n2.next());
        }

        public int compareNumeric(Name n1, Name n2) {
            int firstNum = readInt(n1);
            int secondNum = readInt(n2);

            if (firstNum != secondNum) {
                return returnResult(firstNum - secondNum);
            }

            return compareString(n1.next(), n2.next());
        }

        public int readInt(Name n) {
            int start = n.pos;
            int end = 0;
            while (isDigit(n.getChar())) {
                n.next();
            }
            end = n.pos;
            if (end == start) {
                return 0;
            }
            try {
                return Integer.parseInt(new String(n.buf, start, end - start));
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        public boolean isDigit(char c) {
            return Character.isDigit(c);
        }

        protected abstract int returnResult(int result);
    }

    public static final class SortNameNaturalOrderAscending extends SortNameNaturalOrder {
        @Override protected int returnResult(int result) {
            return result;
        }
    }

    public static final class SortNameNaturalOrderDescending extends SortNameNaturalOrder {
        @Override protected int returnResult(int result) {
            return -result;
        }
    }

    static Comparator<MailItem> getComparator(SortBy sort) {
        boolean asc = sort.getDirection() == SortBy.Direction.ASC;
        switch (sort.getKey()) {
            case ID:
                return asc ? new SortIdAscending() : new SortIdDescending();
            case DATE:
                return asc ? new SortDateAscending() : new SortDateDescending();
            case SIZE:
                return asc ? new SortSizeAscending() : new SortSizeDescending();
            case SUBJECT:
                return asc ? new SortSubjectAscending() : new SortSubjectDescending();
            case NAME_NATURAL_ORDER:
                return asc ? new SortNameNaturalOrderAscending() : new SortNameNaturalOrderDescending();
            default:
                return null;
        }
    }

    /**
     * Exception could be propagated outside the package, one example could be
     * IndexingService using generateIndexDataAsync
     */
    public static class TemporaryIndexingException extends Exception {
        private static final long serialVersionUID = 730987946876783701L;
    }

    /**
     * Returns the indexable data to be passed into index. Subclasses that support indexing must override.
     * <p>
     * This API is generally to be called during asynchronous indexing when the MailItem is being indexed without holding a reference to a Mailbox instance
     * and without Mailbox lock
     *
     * @return a list of IndexDocument to be added to the index for this item
     * @param boolean flag indicating whether to index attachments
     * @throws TemporaryIndexingException recoverable index error
     */
    public List<IndexDocument> generateIndexDataAsync(boolean indexAttachments) throws TemporaryIndexingException {
        return null;
    }

    protected List<IndexDocument> checkNumIndexDocs(List<IndexDocument> docs) {
        int expected = state.getNumIndexDocs();
        int actual = docs == null ? 0 : docs.size();
        if (expected != actual) {
            ZimbraLog.index.warn("mbox %s - incorrect numIndexDocs for %s %s: (expected=%s, actual=%s)", mMailboxData.id, getType(), mId, expected, actual);
        }
        return docs;
    }

    /** Returns the item's parent.  Returns <tt>null</tt> if the item
     *  does not have a parent.
     *
     * @throws ServiceException if there is an error retrieving the
     *         Mailbox's item cache or fetching the parent's data from
     *         the database. */
    MailItem getParent() throws ServiceException {
        int parentId = getParentId();
        if (parentId == -1 || inDumpster())
            return null;
        return mMailbox.getItemById(parentId, Type.UNKNOWN);
    }

    /** Returns the item's {@link Folder}.  All non-dumpstered items in the system must
     *  have a containing folder.  Returns null for dumpstered items.
     *
     * @throws ServiceException should never be thrown, as the set of all
     *                          folders must already be cached. */
    Folder getFolder() throws ServiceException {
        return !inDumpster() ? mMailbox.getFolderById(getFolderId()) : null;
    }

    abstract boolean isTaggable();
    abstract boolean isCopyable();
    abstract boolean isMovable();
    abstract boolean isMutable();
    abstract boolean canHaveChildren();
    boolean isDeletable()             { return true; }
    boolean isLeafNode()              { return true; }
    boolean trackUnread()             { return true; }
    boolean canParent(MailItem child) { return canHaveChildren(); }


    static MailItem getById(Mailbox mbox, int id) throws ServiceException {
        return getById(mbox, id, Type.UNKNOWN);
    }

    static MailItem getById(Mailbox mbox, int id, Type type) throws ServiceException {
        return getById(mbox, id, type, false);
    }

    static MailItem getById(Mailbox mbox, int id, Type type, boolean fromDumpster) throws ServiceException {
        return mbox.getItem(DbMailItem.getById(mbox, id, type, fromDumpster));
    }

    static MailItem getByUuid(Mailbox mbox, String uuid, Type type) throws ServiceException {
        return getByUuid(mbox, uuid, type, false);
    }

    static MailItem getByUuid(Mailbox mbox, String uuid, Type type, boolean fromDumpster) throws ServiceException {
        return mbox.getItem(DbMailItem.getByUuid(mbox, uuid, type, fromDumpster));
    }

    static List<MailItem> getById(Mailbox mbox, Collection<Integer> ids, Type type) throws ServiceException {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<MailItem> items = new ArrayList<MailItem>();
        for (UnderlyingData ud : DbMailItem.getById(mbox, ids, type)) {
            items.add(mbox.getItem(ud));
        }
        return items;
    }

    static MailItem getByImapId(Mailbox mbox, int id, int folderId) throws ServiceException {
        return mbox.getItem(DbMailItem.getByImapId(mbox, id, folderId));
    }

    /** Instantiates the appropriate subclass of <tt>MailItem</tt> for
     *  the item described by the {@link MailItem.UnderlyingData}.  Will
     *  not create memory-only <tt>MailItem</tt>s like {@link Flag}
     *  and {@link VirtualConversation}.
     *
     * @param mbox  The {@link Mailbox} the item is created in.
     * @param data  The contents of a <tt>MAIL_ITEM</tt> database row. */
    public static MailItem constructItem(Mailbox mbox, UnderlyingData data, boolean skipCache) throws ServiceException {
        if (data == null) {
            throw noSuchItem(-1, Type.UNKNOWN);
        }
        switch (Type.of(data.type)) {
            case FOLDER:       return new Folder(mbox, data, skipCache);
            case SEARCHFOLDER: return new SearchFolder(mbox, data, skipCache);
            case TAG:          return new Tag(mbox, data, skipCache);
            case CONVERSATION: return new Conversation(mbox,data, skipCache);
            case MESSAGE:      return new Message(mbox, data, skipCache);
            case CONTACT:      return new Contact(mbox,data, skipCache);
            case DOCUMENT:     return new Document(mbox, data, skipCache);
            case NOTE:         return new Note(mbox, data, skipCache);
            case APPOINTMENT:  return new Appointment(mbox, data, skipCache);
            case TASK:         return new Task(mbox, data, skipCache);
            case MOUNTPOINT:   return new Mountpoint(mbox, data, skipCache);
            case WIKI:         return new WikiItem(mbox, data, skipCache);
            case CHAT:         return new Chat(mbox, data, skipCache);
            case COMMENT:      return new Comment(mbox, data, skipCache);
            case VIRTUAL_CONVERSATION: return new VirtualConversation(mbox,data, skipCache);
            case SMARTFOLDER:  return new SmartFolder(mbox, data, skipCache);
            default:           return null;
        }
    }

    /** Instantiates the appropriate subclass of <tt>MailItem</tt> for
     *  the item described by the {@link MailItem.UnderlyingData}.  Will
     *  not create memory-only <tt>MailItem</tt>s like {@link Flag}
     *  and {@link VirtualConversation}. This item will not hold a reference to its Mailbox and therefore will not be cached
     *
     * @param acc  The {@link Account} the item is created for.
     * @param data  The contents of a <tt>MAIL_ITEM</tt> database row. */
    public static MailItem constructItem(Account acc, UnderlyingData data, int mailboxId) throws ServiceException {
        if (data == null) {
            throw noSuchItem(-1, Type.UNKNOWN);
        }
        switch (Type.of(data.type)) {
            case FOLDER:       return new Folder(acc, data, mailboxId);
            case SEARCHFOLDER: return new SearchFolder(acc, data, mailboxId);
            case TAG:          return new Tag(acc, data, mailboxId);
            case CONVERSATION: return new Conversation(acc, data, mailboxId);
            case MESSAGE:      return new Message(acc, data, mailboxId);
            case CONTACT:      return new Contact(acc, data, mailboxId);
            case DOCUMENT:     return new Document(acc, data, mailboxId);
            case NOTE:         return new Note(acc, data, mailboxId);
            case APPOINTMENT:  return new Appointment(acc, data, mailboxId);
            case TASK:         return new Task(acc, data, mailboxId);
            case MOUNTPOINT:   return new Mountpoint(acc, data, mailboxId);
            case WIKI:         return new WikiItem(acc, data, mailboxId);
            case CHAT:         return new Chat(acc, data, mailboxId);
            case COMMENT:      return new Comment(acc, data, mailboxId);
            case VIRTUAL_CONVERSATION: return new VirtualConversation(acc, data, mailboxId);
            case SMARTFOLDER:  return new SmartFolder(acc, data, mailboxId);
            default:           return null;
        }
    }

    public static MailItem constructItem(Mailbox mbox, UnderlyingData data) throws ServiceException {
        return constructItem(mbox, data, false);
    }

    /** Returns {@link MailServiceException.NoSuchItemException} tailored
     *  for the given type.  Does not actually <u>throw</u> the exception;
     *  that's the caller's job.
     *
     * @param id    The id of the missing item.
     * @param type  The type of the missing item (e.g. {@link #Type.Tag}). */
    public static MailServiceException noSuchItem(int id, Type type) {
        switch (type) {
            case SEARCHFOLDER:
            case MOUNTPOINT:
            case FOLDER:
                return MailServiceException.NO_SUCH_FOLDER(id);
            case FLAG:
            case TAG:
            case SMARTFOLDER:
                return MailServiceException.NO_SUCH_TAG(id);
            case VIRTUAL_CONVERSATION:
            case CONVERSATION:
                return MailServiceException.NO_SUCH_CONV(id);
            case CHAT:
            case MESSAGE:
                return MailServiceException.NO_SUCH_MSG(id);
            case CONTACT:
                return MailServiceException.NO_SUCH_CONTACT(id);
            case WIKI:
            case DOCUMENT:
                return MailServiceException.NO_SUCH_DOC(id);
            case NOTE:
                return MailServiceException.NO_SUCH_NOTE(id);
            case APPOINTMENT:
                return MailServiceException.NO_SUCH_APPT(id);
            case TASK:
                return MailServiceException.NO_SUCH_TASK(id);
            default:
                return MailServiceException.NO_SUCH_ITEM(id);
        }
    }

    /** Returns {@link MailServiceException.NoSuchItemException} tailored
     *  for the given type.  A generic NO_SUCH_ITEM exception is returned
     *  if the type does not support UUID.  Does not actually <u>throw</u> the exception;
     *  that's the caller's job.
     *
     * @param uuid    The UUID of the missing item.
     * @param type  The type of the missing item (e.g. {@link #Type.Document}). */
    public static MailServiceException noSuchItemUuid(String uuid, Type type) {
        switch (type) {
            case SEARCHFOLDER:
            case MOUNTPOINT:
            case FOLDER:
                return MailServiceException.NO_SUCH_FOLDER_UUID(uuid);
            case WIKI:
            case DOCUMENT:
                return MailServiceException.NO_SUCH_DOC_UUID(uuid);
            default:
                return MailServiceException.NO_SUCH_ITEM_UUID(uuid);
        }
    }

    /**
     * Returns whether an item type is a "subclass" of another item type.
     * <p>
     * For instance, returns {@code true} if you have an item of {@link Type#FLAG} and you wanted things of
     * {@link Type#TAG}. The exception to this rule is that a desired {@link Type#UNKNOWN} matches any actual item type.
     *
     * @param desired  The type of item that you wanted.
     * @param actual   The type of item that you've got.
     * @return {@code true} if the types match, if {@code desired} is {@link Type#UNKNOWN}, or if the {@code actual}
     * class is a subclass of the {@code desired} class.
     */
    public static boolean isAcceptableType(Type desired, Type actual) {
        // standard case: exactly what we're asking for
        if (desired == actual || desired == Type.UNKNOWN || desired == Type.UNKNOWN_SEARCHABLE) {
            return true;
        // exceptions: ask for Tag and get Flag, ask for Folder and get SearchFolder or Mountpoint,
        //             ask for Conversation and get VirtualConversation, ask for Document and get Wiki
        } else if (desired == Type.FOLDER && actual == Type.SEARCHFOLDER) {
            return true;
        } else if (desired == Type.FOLDER && actual == Type.MOUNTPOINT) {
            return true;
        } else if (desired == Type.TAG && (actual == Type.FLAG || actual == Type.SMARTFOLDER)) {
            return true;
        } else if (desired == Type.CONVERSATION && actual == Type.VIRTUAL_CONVERSATION) {
            return true;
        } else if (desired == Type.DOCUMENT && actual == Type.WIKI) {
            return true;
        } else if (desired == Type.MESSAGE && actual == Type.CHAT) {
            return true;
        // failure: found something, but it's not the type you were looking for
        } else {
            return false;
        }
    }

    /** Returns whether the item is a "subclass" of another item type.  For
     *  instance, returns <tt>true</tt> if the item is a {@link Flag} and you
     *  wanted things of type {@link #TYPE_TAG}.  The exception to this rule
     *  is that a desired {@link #TYPE_UNKNOWN} matches any actual item type.
     *
     * @param desired  The type of item that you wanted.
     * @return <tt>true</tt> if the types match, if <tt>desired</tt> is
     *         {@link #TYPE_UNKNOWN}, or if the item is a subclass of the
     *         <tt>desired</tt> class. */
    public boolean isAcceptableType(Type desired) {
        return isAcceptableType(desired, getType());
    }

    boolean checkChangeID() throws ServiceException {
        return mMailbox.checkItemChangeID(this);
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
    void markItemModified(int reason) throws ServiceException {
        mMailbox.markItemModified(this, reason);
    }

    /** Adds this item to the {@link Mailbox}'s list of blobs to be removed
     *  upon <u>successful</u> completion of the current transaction. */
    void markBlobForDeletion() {
        try {
            markBlobForDeletion(getBlob());
        } catch (ServiceException e) {
            ZimbraLog.mailbox.warn("error queuing blob for deletion for id: " + mId + ", change: " + getSavedSequence(), e);
        }
    }

    /** Adds this {@link MailboxBlob} to the {@link Mailbox}'s list of blobs
     *  to be removed upon <u>successful</u> completion of the current
     *  transaction. */
    void markBlobForDeletion(MailboxBlob mblob) {
        if (mblob == null)
            return;
        PendingDelete info = new PendingDelete();
        info.blobs.add(mblob);
        mMailbox.markOtherItemDirty(info);
    }

    /** Updates various lists and counts as the result of item creation.  This
     *  method should always be called immediately after a new item is created
     *  and persisted to the database.
     *
     * @param parent  The created item's parent.  The parent's addChild()
     *                method will be called during the function. */
    protected void finishCreation(MailItem parent) throws ServiceException {
        markItemCreated();

        // let the parent know it's got a new child
        if (parent != null) {
            parent.addChild(this);
        }

        // sanity-check the location of the newly-created item
        Folder folder = getFolder();
        if (!folder.canContain(this)) {
            throw MailServiceException.CANNOT_CONTAIN();
        }

        // update mailbox and folder sizes
        if (isLeafNode()) {
            boolean isDeleted = isTagged(Flag.FlagInfo.DELETED);

            long size = getSize();
            mMailbox.updateSize(size, isQuotaCheckRequired());
            folder.updateSize(1, isDeleted ? 1 : 0, size);
            updateTagSizes(1, isDeleted ? 1 : 0, size);

            // let the folder and tags know if the new item is unread
            int unread = getUnreadCount();
            folder.updateUnread(unread, isDeleted ? unread : 0);
            updateTagUnread(unread, isDeleted ? unread : 0);
        }
    }

    /**
     * Returns {@code true} if a quota check is required when creating
     * this item.  See bug 15666.
     */
    @SuppressWarnings("unused")
    protected boolean isQuotaCheckRequired() throws ServiceException {
        return true;
    }

    /** Changes the item's color.  Color is specified in RGB, with
     *  one byte each for red, blue, and green.  The highest byte
     *  is unused.
     *
     * @param color  The item's new color.
     * @perms {@link ACL#RIGHT_WRITE} on the item
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><tt>service.PERM_DENIED</tt> - if you don't have sufficient
     *        permissions</ul> */
    void setColor(Color color) throws ServiceException {
        if (!canAccess(ACL.RIGHT_WRITE)) {
            throw ServiceException.PERM_DENIED("you do not have the necessary permissions on the item");
        } else if (color.equals(state.getColor())) {
            return;
        }
        markItemModified(Change.COLOR);
        state.setColor(color);
        saveMetadata();
    }

    /** Changes the item's color.  The server does no value-to-color mapping;
     *  the supplied color is treated as an opaque byte.  Note than even
     *  "immutable" items can have their color changed.
     *
     * @param color  The item's new color.
     * @perms {@link ACL#RIGHT_WRITE} on the item
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><tt>service.PERM_DENIED</tt> - if you don't have sufficient
     *        permissions</ul> */
    @Deprecated
    void setColor(byte color) throws ServiceException {
        if (!canAccess(ACL.RIGHT_WRITE)) {
            throw ServiceException.PERM_DENIED("you do not have the necessary permissions on the item");
        }

        if (color == state.getColor().getMappedColor())
            return;

        markItemModified(Change.COLOR);
        state.getColor().setColor(color);
        saveMetadata();
    }

    /** Changes the item's date.
     *
     * @param date  The item's new date - as number of milliseconds since 1970-01-01 00:00:00 UTC (i.e. Java Date).
     * @perms {@link ACL#RIGHT_WRITE} on the item
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><tt>service.PERM_DENIED</tt> - if you don't have sufficient
     *        permissions</ul> */
    void setDate(long date) throws ServiceException {
        int dateSecs = (int) (date / 1000L);
        if (getDate() == dateSecs) {
            return;
        }

        if (!canAccess(ACL.RIGHT_WRITE)) {
            throw ServiceException.PERM_DENIED("you do not have the necessary permissions on the item");
        }

        if (ZimbraLog.mailop.isDebugEnabled()) {
            ZimbraLog.mailop.debug("Setting date of %s to %d.", getMailopContext(this), date);
        }
        markItemModified(Change.DATE);
        state.setDate(dateSecs);
        metadataChanged();
        DbMailItem.saveDate(this);
    }

    /** Sets the IMAP UID for the item and persists it to the database.  Does
     *  not update the containing folder's IMAP UID highwater mark; that is
     *  done implicitly whenever the folder size increases. */
    void setImapUid(int imapId) throws ServiceException {
        if (getImapUid() == imapId)
            return;

        if (ZimbraLog.mailop.isDebugEnabled()) {
            ZimbraLog.mailop.debug("Setting imapId of %s to %d.", getMailopContext(this), imapId);
        }
        markItemModified(Change.IMAP_UID);
        state.setImapId(imapId);
        metadataChanged();
        DbMailItem.saveImapUid(this);

        getFolder().updateUIDNEXT();
    }

    MailboxBlob setContent(StagedBlob staged, Object content) throws ServiceException, IOException {
        addRevision(false);

        // update the item's relevant attributes
        markItemModified(Change.CONTENT  | Change.DATE | Change.IMAP_UID | Change.SIZE);

        // delete the old blob *unless* we've already rewritten it in this transaction
        if (getSavedSequence() != mMailbox.getOperationChangeID()) {
            if (!canAccess(ACL.RIGHT_WRITE)) {
                throw ServiceException.PERM_DENIED("you do not have the necessary permissions on the item");
            }
            boolean delete = true;
            // don't delete blob if last revision uses it
            if (isTagged(Flag.FlagInfo.VERSIONED)) {
                List<MailItem> revisions = loadRevisions();
                if (!revisions.isEmpty()) {
                    MailItem lastRev = revisions.get(revisions.size() - 1);
                    if (lastRev.getSavedSequence() == getSavedSequence()) {
                        delete = false;
                    }
                }
            }
            if (delete) {
                markBlobForDeletion();
            }
        }

        // remove the content from the cache
        MessageCache.purge(this);

        // update the object to reflect its new contents
        long size = staged == null ? 0 : staged.getSize();
        long oldSize = getSize();
        if (oldSize  != size) {
            mMailbox.updateSize(size - oldSize, isQuotaCheckRequired());
            state.setSize(size);
        }
        getFolder().updateSize(0, 0, size - oldSize);

        state.setBlobDigest(staged == null ? null : staged.getDigest());
        state.setDate(mMailbox.getOperationTimestamp());
        state.setImapId(mMailbox.isTrackingImap() ? 0 : mId);
        contentChanged();

        // write the content (if any) to the store
        MailboxBlob mblob = null;
        if (staged != null) {
            StoreManager sm = StoreManager.getInstance();
            // under windows, a rename will fail if the incoming file is open
            if (SystemUtil.ON_WINDOWS)
                mblob = sm.link(staged, mMailbox, mId, getSavedSequence());
            else
                mblob = sm.renameTo(staged, mMailbox, mId, getSavedSequence());
            mMailbox.markOtherItemDirty(mblob);
        }
        mBlob = null;
        state.setLocator(mblob == null ? null : mblob.getLocator());

        // rewrite the DB row to reflect our new view (MUST call saveData)
        reanalyze(content, size);
        mMailbox.cache(this);

        return mblob;
    }

    @SuppressWarnings("unused")
    int getMaxRevisions() throws ServiceException {
        return 1;
    }

    List<MailItem> loadRevisions() throws ServiceException {
        if (mRevisions == null) {
            mRevisions = new ArrayList<MailItem>();

            if (isTagged(Flag.FlagInfo.VERSIONED)) {
                for (UnderlyingData data : DbMailItem.getRevisionInfo(this, inDumpster()))
                    mRevisions.add(constructItem(mMailbox, data));
            }
        }

        return mRevisions;
    }

    void addRevision(boolean persist) throws ServiceException {
        addRevision(persist, true);
    }

    void addRevision(boolean persist, boolean updateFolderMODSEQ) throws ServiceException {
        // don't take two revisions for the same data
        if (getModifiedSequence() == mMailbox.getOperationChangeID())
            return;

        Folder folder = getFolder();
        int maxNumRevisions = getMaxRevisions();

        // record the current version as a revision
        if (maxNumRevisions != 1) {
            loadRevisions();

            // Don't take two revisions for the same data.
            if (!mRevisions.isEmpty()) {
                MailItem lastRev = mRevisions.get(mRevisions.size() - 1);
                if (lastRev.getSavedSequence() == getSavedSequence() && lastRev.getModifiedSequence() == getModifiedSequence())
                    return;

                int maxVer = 0;
                for (MailItem rev : mRevisions)
                    maxVer = Math.max(maxVer, rev.getVersion());

                int curVersion = state.getVersion();
                if (curVersion <= maxVer) {
                    ZimbraLog.mailop.info("Item's current version is not greater than highest revision; " +
                                          "adjusting to " + (maxVer + 1) + " (was " + curVersion + ")");
                    state.setVersion(maxVer + 1);
                }
            }

            UnderlyingData data = state.getUnderlyingData().clone();
            data.metadata = encodeMetadata().toString();
            data.setFlag(Flag.FlagInfo.UNCACHED);
            mRevisions.add(constructItem(mMailbox, data));

            long size = getSize();
            mMailbox.updateSize(size, isQuotaCheckRequired());
            folder.updateSize(0, 0, size);

            int version = state.getVersion();
            ZimbraLog.mailop.debug("saving revision %d for %s", version, getMailopContext(this));

            DbMailItem.snapshotRevision(this, version);
            if (!isTagged(Flag.FlagInfo.VERSIONED)) {
                tagChanged(mMailbox.getFlagById(Flag.ID_VERSIONED), true);
            }
        }

        // now that we've made a copy of the item, we can increment the version number
        state.incrementVersion();

        // Purge revisions and their blobs beyond revision count limit.
        if (maxNumRevisions > 0 && isTagged(Flag.FlagInfo.VERSIONED)) {
            List<MailItem> revisions = loadRevisions();
            int numRevsToPurge = revisions.size() - (maxNumRevisions - 1);  // -1 for main item
            if (numRevsToPurge > 0) {
                List<MailItem> toPurge = new ArrayList<MailItem>();
                int numPurged = 0;
                for (Iterator<MailItem> it = revisions.iterator(); it.hasNext() && numPurged < numRevsToPurge; numPurged++) {
                    MailItem revision = it.next();
                    toPurge.add(revision);
                    it.remove();
                }

                // The following logic depends on version, mod_metadata and mod_content each being
                // monotonically increasing in the revisions list. (f(n) <= f(n+1))

                // Filter out blobs that are still in use; mark the rest for deletion.
                int oldestRemainingSavedSequence =
                    revisions.isEmpty() ? getSavedSequence() : revisions.get(0).getSavedSequence();
                for (MailItem revision : toPurge) {
                    if (revision.getSavedSequence() < oldestRemainingSavedSequence) {
                        mMailbox.updateSize(-revision.getSize());
                        folder.updateSize(0, 0, -revision.getSize());
                        revision.markBlobForDeletion();
                    }
                }
                // Purge revisions from db.
                int highestPurgedVer = toPurge.get(toPurge.size() - 1).getVersion();
                DbMailItem.purgeRevisions(this, highestPurgedVer, true);
            }
            if (revisions.isEmpty()) {
                tagChanged(mMailbox.getFlagById(Flag.ID_VERSIONED), false);
            }
        }

        metadataChanged(updateFolderMODSEQ);
        if (persist) {
            saveData(new DbMailItem(mMailbox));
        }
    }

    // do *not* make this public, as it'd skirt Mailbox-level synchronization and caching
    MailItem getRevision(int version) throws ServiceException {
        int curVersion = state.getVersion();
        if (version == curVersion) {
            return this;
        }
        if (version <= 0 || version > curVersion || !isTagged(Flag.FlagInfo.VERSIONED)) {
            return null;
        }
        for (MailItem revision : loadRevisions()) {
            if (revision.state.getVersion() == version)
                return revision;
        }
        return null;
    }

    void purgeRevision(int version, boolean includeOlderRevisions) throws ServiceException {
        if (!canAccess(ACL.RIGHT_WRITE))
            throw ServiceException.PERM_DENIED("you do not have the necessary permissions on the item");
        PendingDelete info = new PendingDelete();
        for (MailItem revision : loadRevisions()) {
            if (revision.getVersion() == version || (includeOlderRevisions && revision.getVersion() <= version)) {
                try {
                    info.blobs.add(revision.getBlob());
                    info.blobDigests.add(revision.getDigest());
                    info.size += revision.getSize();
                } catch (Exception e) {
                    ZimbraLog.mailbox.error("missing blob for id: " + revision.getId() + ", revision: " + revision.getVersion());
                }
            }
        }
        DbMailItem.purgeRevisions(this, version, includeOlderRevisions);
        getMailbox().markOtherItemDirty(info);
        getMailbox().updateSize(-info.size);
        mRevisions = null;
    }

    static public int purgeRevisions(Mailbox mbx, long before) throws ServiceException {
        HashSet<Integer>  outdatedIds = DbMailItem.getItemsWithOutdatedRevisions( mbx, (int)(before/1000) );
        int numberofpurgedrevisions = 0;
        for(Iterator<Integer> iter= outdatedIds.iterator(); iter.hasNext();)  {
            MailItem  item = getById(mbx, iter.next());

            // Purge revisions and their blobs .
            if ( item != null && item.getType() == Type.DOCUMENT && item.isTagged(Flag.FlagInfo.VERSIONED)) {
                List<MailItem> revisions = item.loadRevisions();
                List<MailItem> toPurge = new ArrayList<MailItem>();

                Folder folder = item.getFolder();
                for (Iterator<MailItem> it = revisions.iterator(); it.hasNext();) {
                    MailItem revision = it.next();
                    if(revision.getDate() < before) {
                        toPurge.add(revision);
                        it.remove();
                    }
                }

                // The following logic depends on version, mod_metadata and mod_content each being
                // monotonically increasing in the revisions list. (f(n) <= f(n+1))

                // Filter out blobs that are still in use; mark the rest for deletion.
                int oldestRemainingSavedSequence =
                    revisions.isEmpty() ? item.getSavedSequence() : revisions.get(0).getSavedSequence();
                for (MailItem revision : toPurge) {
                    if (revision.getSavedSequence() < oldestRemainingSavedSequence) {
                        item.mMailbox.updateSize(-revision.getSize());
                        folder.updateSize(0, 0, -revision.getSize());
                        revision.markBlobForDeletion();
                    }
                    numberofpurgedrevisions++;
                }

                // Purge revisions from db.
                int highestPurgedVer = toPurge.get(toPurge.size() - 1).getVersion();
                DbMailItem.purgeRevisions(item, highestPurgedVer, true);

                if (revisions.isEmpty()) {
                    item.tagChanged(item.mMailbox.getFlagById(Flag.ID_VERSIONED), false);
                }
            }
        }

        return numberofpurgedrevisions;
    }

    /** Recalculates the size, metadata, etc. for an existing MailItem and
     *  persists that information to the database.  Maintains any existing
     *  mutable metadata.  Updates mailbox and folder sizes appropriately.
     *
     * @param data  The (optional) extra item data for indexing (e.g.
     *              a Message's {@link com.zimbra.cs.index.ParsedMessage}. */
    void reanalyze(Object data, long newSize) throws ServiceException {
        throw ServiceException.FAILURE("reanalysis of " + getType() + "s not supported", null);
    }

    protected void updateIndexedDocCount(int newCount) {
        int curCount = state.getNumIndexDocs();
        if (curCount != newCount) {
            ZimbraLog.index.debug("updating numIndexDocs for %s %s: %s -> %s", getType(), mId, curCount, newCount);
            state.setNumIndexDocs(newCount);
        }
    }
    @SuppressWarnings("unused") void detach() throws ServiceException  { }

    /** Updates the item's unread state.  Persists the change to the
     *  database and cache, and also updates the unread counts for the
     *  item's {@link Folder} and {@link Tag}s appropriately.
     *
     * @param unread  <tt>true</tt> to mark the item unread,
     *                <tt>false</tt> to mark it as read.
     * @perms {@link ACL#RIGHT_WRITE} on the item
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><tt>mail.CANNOT_TAG</tt> - if the item can't be marked unread
     *    <li><tt>service.FAILURE</tt> - if there's a database failure
     *    <li><tt>service.PERM_DENIED</tt> - if you don't have sufficient
     *        permissions</ul> */
    void alterUnread(boolean unread) throws ServiceException {
        // detect NOOPs and bail
        if (unread == isUnread()) {
            return;
        }
        Flag unreadFlag = Flag.FlagInfo.UNREAD.toFlag(mMailbox);
        if (!unreadFlag.canTag(this)) {
            throw MailServiceException.CANNOT_TAG(unreadFlag, this);
        } else if (!canAccess(ACL.RIGHT_WRITE)) {
            throw ServiceException.PERM_DENIED("you do not have the required rights on the item");
        }

        markItemModified(Change.UNREAD);
        int delta = unread ? 1 : -1;
        updateUnread(delta, isTagged(Flag.FlagInfo.DELETED) ? delta : 0);
        metadataChanged();
        DbMailItem.alterUnread(getMailbox(), ImmutableList.of(getId()), unread);
    }

    /** Tags or untags an item.  Persists the change to the database and
     *  cache.  If the item is unread and its tagged state is changing,
     *  updates the {@link Tag}'s unread count appropriately.  Note that the
     *  parent is not fetched from the database, so notifications may be off
     *  in the case of uncached {@link Conversation}s when a {@link Message}
     *  changes state.<p>
     *
     *  You must use {@link #alterUnread} to change an item's unread state.
     *
     * @param tag  The tag or flag to add or remove from the item.
     * @param add  <tt>true</tt> to tag the item, <tt>false</tt> to untag it.
     * @perms {@link ACL#RIGHT_WRITE} on the item
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><tt>mail.CANNOT_TAG</tt> - if the item can't be tagged with the
     *        specified tag
     *    <li><tt>service.FAILURE</tt> - if there's a database failure or if
     *        an invalid Tag is supplied
     *    <li><tt>service.PERM_DENIED</tt> - if you don't have sufficient
     *        permissions</ul>
     * @see #alterUnread(boolean) */
    void alterTag(Tag tag, boolean add) throws ServiceException {
        if (tag == null) {
            throw ServiceException.FAILURE("no tag supplied when trying to tag item " + mId, null);
        } else if (!isTaggable() || (add && !tag.canTag(this))) {
            throw MailServiceException.CANNOT_TAG(tag, this);
        } else if (tag.getId() == Flag.ID_UNREAD) {
            throw ServiceException.FAILURE("unread state must be set with alterUnread", null);
        } else if (!canAccess(ACL.RIGHT_WRITE)) {
            throw ServiceException.PERM_DENIED("you do not have the required rights on the item");
        }
        // detect NOOPs and bail
        if (add == isTagged(tag)) {
            return;
        }
        // don't let the user tag things as "has attachments" or "draft"
        if (tag instanceof Flag && ((Flag) tag).isSystemFlag()) {
            throw MailServiceException.CANNOT_TAG(tag, this);
        }
        // grab the parent *before* we make any other changes
        MailItem parent = getParent();

        // change our cached tags
        tagChanged(tag, add);

        // since we're adding/removing a tag, the tag's unread count may change
        int unreadDelta = (add ? 1 : -1) * getUnreadCount();
        if (tag.trackUnread() && unreadDelta != 0) {
            tag.updateUnread(unreadDelta, isTagged(Flag.FlagInfo.DELETED) ? unreadDelta : 0);
        }

        int countDelta = (add ? 1 : -1) * (isLeafNode() ? 1 : (int) getSize());
        tag.updateSize(countDelta, isTagged(Flag.FlagInfo.DELETED) ? countDelta : 0);

        // if we're adding/removing the \Deleted flag, update the folder and tag "deleted" and "deleted unread" counts
        if (tag.getId() == Flag.ID_DELETED) {
            getFolder().updateSize(0, add ? 1 : -1, 0);
            updateTagSizes(0, add ? 1 : -1, 0);
            // note that Message.updateUnread() calls updateTagUnread()
            if (unreadDelta != 0) {
                updateUnread(0, unreadDelta);
            }
        }

        if (ZimbraLog.mailop.isDebugEnabled()) {
            ZimbraLog.mailop.debug("Setting %s for %s.", getMailopContext(tag), getMailopContext(this));
        }
        // alter our tags in the DB
//        DbTag.alterTag(this, tag, add);
        DbTag.alterTag(tag, Arrays.asList(getId()), add);

        // tell our parent about the tag change (note: must happen after DbMailItem.alterTag)
        if (parent != null) {
            parent.inheritedTagChanged(tag, add);
        }
    }

    final void alterSystemFlag(Flag flag, boolean newValue) throws ServiceException {
        if (flag == null) {
            throw ServiceException.FAILURE("no tag supplied when trying to tag item " + mId, null);
        } else if ((flag.toBitmask() & Flag.FLAGS_SYSTEM) == 0) {
            throw ServiceException.FAILURE("requested to alter a non-system tag", null);
        } else if (newValue && !flag.canTag(this)) {
            throw MailServiceException.CANNOT_TAG(flag, this);
        } else if (newValue == isTagged(flag)) {
            return;
        }

        // grab the parent *before* we make any other changes
        MailItem parent = getParent();

        // change our cached tags
        tagChanged(flag, newValue);

        // alter our tags in the DB
        DbTag.alterTag(flag, Arrays.asList(getId()), newValue);

        // tell our parent about the tag change (note: must happen after DbMailItem.alterTag)
        if (parent != null) {
            parent.inheritedTagChanged(flag, newValue);
        }
    }

    /** Updates the object's in-memory state to reflect a {@link Tag} change.
     *  Does not update the database.
     *
     * @param tag  The tag that was added or rmeoved from this object.
     * @param add  <tt>true</tt> if the item was tagged,
     *             <tt>false</tt> if the item was untagged. */
    protected void tagChanged(Tag tag, boolean add) throws ServiceException {
        boolean isFlag = tag instanceof Flag;
        // changing a system flag is not a syncable event
        if (!isFlag || !((Flag) tag).isSystemFlag()) {
            metadataChanged();
        }

        if (isFlag) {
            if (add) {
                state.setFlag((Flag) tag);
            } else {
                state.unsetFlag((Flag) tag);
            }
        } else {
            Set<String> tags = Sets.newLinkedHashSet();
            Collections.addAll(tags, state.getTags());
            Collections.addAll(tags, state.getSmartFolders());
            if (add) {
                tags.add(tag.getName());
            } else {
                tags.remove(tag.getName());
            }
            state.setTags(tags.isEmpty() ? null : new Tag.NormalizedTags(tags));
        }
        markItemModified(isFlag ? Change.FLAGS : Change.TAGS);
    }

    @SuppressWarnings("unused")
    protected void inheritedTagChanged(Tag tag, boolean add) throws ServiceException  { }

    /** Updates the in-memory unread count for the item.  The base-class
     *  implementation does not cascade the change to the item's parent,
     *  folder, and tags, as {@link Message#updateUnread(int,int)} does.
     *
     * @param delta  The change in unread count for this item. */
    protected void updateUnread(int delta, int deletedDelta) throws ServiceException {
        if (delta == 0 || !trackUnread()) {
            return;
        }
        // update our unread count (should we check that we don't have too many unread?)
        markItemModified(Change.UNREAD);
        int newUnread = getUnreadCount() + delta;
        state.setUnreadCount(newUnread);
        if (newUnread < 0) {
            mMailbox.setIsDirtyTransaction(true);
            ZimbraLog.mailbox.warn("inconsistent state: unread < 0 for item %d, Folder and tags count will be recalculated", mId);
        }
    }

    /** Adds <tt>delta</tt> to the unread count of each {@link Tag}
     *  assigned to this {@code MailItem}.
     *
     * @param delta  The (signed) change in number unread.
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><tt>mail.NO_SUCH_FOLDER</tt> - if there's an error fetching the
     *        item's {@link Folder}</ul> */
    protected void updateTagUnread(int delta, int deletedDelta) throws ServiceException {
        if ((delta == 0 && deletedDelta == 0) || !isTaggable())
            return;

        String[] tags = state.getTags();
        for (String name : tags) {
            try {
                mMailbox.getTagByName(name).updateUnread(delta, deletedDelta);
            } catch (MailServiceException.NoSuchItemException nsie) {
                ZimbraLog.mailbox.warn("item %d has nonexistent tag %s", mId, name);
                continue;
            }
        }
    }

    /** Adds <tt>delta</tt> to the unread count of each {@link Tag}
     *  assigned to this {@code MailItem}.
     *
     * @param delta  The (signed) change in number unread.
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><tt>mail.NO_SUCH_FOLDER</tt> - if there's an error fetching the
     *        item's {@link Folder}</ul> */
    protected void updateTagSizes(int countDelta, int deletedDelta, long sizeDelta) throws ServiceException {
        if ((countDelta == 0 && deletedDelta == 0 && sizeDelta == 0) || !isTaggable())
            return;

        String[] tags = state.getTags();
        for (String name : tags) {
            try {
                mMailbox.getTagByName(name).updateSize(countDelta, deletedDelta);
            } catch (MailServiceException.NoSuchItemException nsie) {
                ZimbraLog.mailbox.warn("item %d has nonexistent tag %s", mId, name);
                continue;
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
     * @param ntags  The set of tag names to apply.
     * @perms {@link ACL#RIGHT_WRITE} on the item
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><tt>service.FAILURE</tt> - if there's a database failure
     *    <li><tt>service.PERM_DENIED</tt> - if you don't have sufficient
     *        permissions</ul> */
    void setTags(int flags, Tag.NormalizedTags ntags) throws ServiceException {
        if (!canAccess(ACL.RIGHT_WRITE)) {
            throw ServiceException.PERM_DENIED("you do not have the required rights on the item");
        }

        // FIXME: more optimal would be to do this with a single db UPDATE...

        // make sure the caller can't change immutable flags
        flags = (flags & ~Flag.FLAGS_SYSTEM) | (getFlagBitmask() & Flag.FLAGS_SYSTEM);
        // handle flags first...
        int curFlags = state.getFlags();
        if (flags != curFlags) {
            markItemModified(Change.FLAGS);
            for (int flagId : Flag.toId(flags ^ curFlags)) {
                Flag flag = Flag.of(mMailbox, flagId);
                if (flag != null) {
                    alterTag(flag, !isTagged(flag));
                }
            }
        }

        // then handle tags...
        String[] curTags = getTags();
        if (ntags.getTags() != curTags) {
            Set<String> removed = Sets.newHashSet(curTags), added = Sets.newHashSet(ntags.getTags());
            removed.removeAll(added);
            added.removeAll(Arrays.asList(curTags));

            for (String tagName : removed) {
                try {
                    alterTag(mMailbox.getTagByName(tagName), false);
                } catch (MailServiceException.NoSuchItemException nsie) { }
            }
            for (String tagName : added) {
                try {
                    alterTag(mMailbox.getTagByName(tagName), true);
                } catch (MailServiceException.NoSuchItemException nsie) { }
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
     * @param folder    The folder to copy the item to.
     * @param copyId    The item id for the newly-created copy.
     * @param parent    The target parent MailItem for the new copy.
     * @perms {@link ACL#RIGHT_INSERT} on the target folder,
     *        {@link ACL#RIGHT_READ} on the original item
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><tt>mail.CANNOT_COPY</tt> - if the item is not copyable
     *    <li><tt>mail.CANNOT_CONTAIN</tt> - if the target folder can't hold
     *        the copy of the item
     *    <li><tt>service.FAILURE</tt> - if there's a database failure
     *    <li><tt>service.PERM_DENIED</tt> - if you don't have sufficient
     *        permissions</ul> */
    MailItem copy(Folder folder, int copyId, String copyUuid, MailItem parent) throws IOException, ServiceException {
        if (!isCopyable())
            throw MailServiceException.CANNOT_COPY(mId);
        if (!folder.canContain(this))
            throw MailServiceException.CANNOT_CONTAIN();

        if (!canAccess(ACL.RIGHT_READ))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the item");
        if (!folder.canAccess(ACL.RIGHT_INSERT))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the target folder");

        // We'll share the index entry if this item can't change out from under us. Re-index the copy if existing item
        // (a) wasn't indexed or (b) is mutable or (c) existing item is in dumpster (which implies copy is not in
        // dumpster)
        boolean inDumpster = inDumpster();
        boolean shareIndex = !inDumpster && !isMutable() && getIndexStatus() == IndexStatus.DONE && !folder.inSpam();

        // if the copy or original is in Spam, put the copy in its own conversation
        boolean detach = parent == null || isTagged(Flag.FlagInfo.DRAFT) || inSpam() != folder.inSpam();
        parent = detach ? null : parent;

        if (shareIndex && !isTagged(Flag.FlagInfo.COPIED)) {
            alterSystemFlag(mMailbox.getFlagById(Flag.ID_COPIED), true);
            if (ZimbraLog.mailop.isDebugEnabled()) {
                ZimbraLog.mailop.debug("setting copied flag for %s", getMailopContext(this));
            }
        }
        StoreManager sm = StoreManager.getInstance();

        // main item
        String locator = null;
        MailboxBlob srcMblob = getBlob();
        if (srcMblob != null) {
            MailboxBlob mblob = sm.copy(srcMblob, mMailbox, copyId, mMailbox.getOperationChangeID());
            mMailbox.markOtherItemDirty(mblob);
            locator = mblob.getLocator();
        }

        UnderlyingData data = state.getUnderlyingData().duplicate(copyId, copyUuid, folder.getId(), locator);
        data.parentId = detach ? -1 : parent.mId;
        data.indexId = shareIndex ? getIndexId() : IndexStatus.DEFERRED.id();
        if (!shareIndex) {
            data.unsetFlag(Flag.FlagInfo.COPIED);
        }
        data.unsetFlag(Flag.FlagInfo.IN_DUMPSTER);  // The copy is never in dumpster.
        data.metadata = encodeMetadata().toString();
        data.contentChanged(mMailbox);

        ZimbraLog.mailop.info("Copying %s: copyId=%d, folderId=%d, folderName=%s, parentId=%d.",
                              getMailopContext(this), copyId, folder.getId(), folder.getName(), data.parentId);
        String prevFolders = DbMailItem.copy(this, copyId, copyUuid, folder, data.indexId, data.parentId, data.locator, data.metadata, inDumpster);
        if (!StringUtil.isNullOrEmpty(prevFolders)) {
            data.setPrevFolders(prevFolders);
        }
        if (this instanceof CalendarItem)
            DbMailItem.copyCalendarItem((CalendarItem) this, copyId, inDumpster);

        // older revisions
        // Copy revisions only when recovering from dumpster.  When copying from one non-dumpster folder to another,
        // it is never desirable to copy old revisions. (bug 55070)
        if (inDumpster) {
            for (MailItem revision : loadRevisions()) {
                MailboxBlob srcRevBlob = revision.getBlob();
                String revLocator = null;
                if (srcRevBlob != null) {
                    MailboxBlob copyRevBlob = sm.copy(srcRevBlob, mMailbox, copyId, revision.getSavedSequence());
                    mMailbox.markOtherItemDirty(copyRevBlob);
                    revLocator = copyRevBlob.getLocator();
                }
                DbMailItem.copyRevision(revision, copyId, revLocator, inDumpster);
            }
        }

        MailItem copy = constructItem(mMailbox, data);
        copy.finishCreation(parent);

        if (!shareIndex) {
            mMailbox.indexItem(copy);
        }

        return copy;
    }

    /** Copies the item to the target folder.  Persists the new item to the
     *  database and the in-memory cache.  Copies to the same folder as the
     *  original item will succeed, but it is strongly suggested that
     *  {@link #copy(Folder, int, int, short)} be used in that case.<p>
     *
     *  Immutable copied items (both the original and the target) share the
     *  same entry in the index and get the {@link Flag#BITMASK_COPIED} flag to
     *  facilitate garbage collection of index entries.  (Mutable copied items
     *  are indexed separately.)  They do not share the same blob on disk,
     *  although the system will use a hard link where possible.  Copied
     *  {@link Message}s are remain in the same {@link Conversation}, but the
     *  <b>original</b> Message is placed in a new {@link VirtualConversation}
     *  rather than being grouped with the copied Message.
     *
     * @param target  The folder to copy the item to.
     * @param copyId  The item id for the newly-created copy.
     * @perms {@link ACL#RIGHT_INSERT} on the target folder,
     *        {@link ACL#RIGHT_READ} on the original item
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><tt>mail.CANNOT_COPY</tt> - if the item is not copyable
     *    <li><tt>mail.CANNOT_CONTAIN</tt> - if the target folder can't hold
     *        the copy of the item
     *    <li><tt>service.FAILURE</tt> - if there's a database failure
     *    <li><tt>service.PERM_DENIED</tt> - if you don't have sufficient
     *        permissions</ul> */
    MailItem icopy(Folder target, int copyId, String copyUuid) throws IOException, ServiceException {
        if (!isCopyable())
            throw MailServiceException.CANNOT_COPY(mId);
        if (!target.canContain(this))
            throw MailServiceException.CANNOT_CONTAIN();

        // permissions required are the same as for copy()
        if (!canAccess(ACL.RIGHT_READ))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the item");
        if (!target.canAccess(ACL.RIGHT_INSERT))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the target folder");

        // fetch the parent *before* changing the DB
        MailItem parent = getParent();

        // first, copy the item to the target folder while setting:
        //   - FLAGS -> FLAGS | Flag.BITMASK_COPIED
        //   - INDEX_ID -> old index id
        //   - FOLDER_ID -> new folder
        //   - IMAP_ID -> new IMAP uid
        //   - VOLUME_ID -> target volume ID
        // then, update the original item
        //   - PARENT_ID -> NULL
        //   - FLAGS -> FLAGS | Flag.BITMASK_COPIED
        // finally, update OPEN_CONVERSATION if PARENT_ID was NULL
        //   - ITEM_ID = copy's id for hash

        String locator = null;
        MailboxBlob srcMblob = getBlob();
        if (srcMblob != null) {
            StoreManager sm = StoreManager.getInstance();
            MailboxBlob mblob = sm.copy(srcMblob, mMailbox, copyId, mMailbox.getOperationChangeID());
            mMailbox.markOtherItemDirty(mblob);
            locator = mblob.getLocator();
        }

        // We'll share the index entry if this item can't change out from under us. Re-index the copy if existing item
        // (a) wasn't indexed or (b) is mutable.
        boolean shareIndex = !isMutable() && getIndexStatus() == IndexStatus.DONE && !target.inSpam();

        UnderlyingData data = state.getUnderlyingData().duplicate(copyId, copyUuid, target.getId(), locator);
        data.metadata = encodeMetadata().toString();
        data.imapId = copyId;
        data.indexId = shareIndex ? getIndexId() : IndexStatus.DEFERRED.id();
        data.contentChanged(mMailbox);

        ZimbraLog.mailop.info("Performing IMAP copy of %s: copyId=%d, folderId=%d, folderName=%s, parentId=%d.",
            getMailopContext(this), copyId, target.getId(), target.getName(), data.parentId);
        DbMailItem.icopy(this, data, shareIndex);

        MailItem copy = constructItem(mMailbox, data);
        copy.finishCreation(null);

        if (shareIndex && !isTagged(Flag.FlagInfo.COPIED)) {
            Flag copiedFlag = mMailbox.getFlagById(Flag.ID_COPIED);
            tagChanged(copiedFlag, true);
            copy.tagChanged(copiedFlag, true);
            if (parent != null)
                parent.inheritedTagChanged(copiedFlag, true);
        }

        if (parent != null && parent.getId() > 0) {
            markItemModified(Change.PARENT);
            parent.markItemModified(Change.CHILDREN);
            state.setParentId(type == Type.MESSAGE.toByte() ? -mId : -1);
            metadataChanged();
        }

        if (!shareIndex) {
            mMailbox.indexItem(copy);
        }

        return copy;
    }

    /** The regexp defining printable characters not permitted in item
     *  names.  These are: ':', '/', '"', '\t', '\r', and '\n'. */
    private static final String INVALID_NAME_CHARACTERS = "[:/\"\t\r\n]";

    private static final String INVALID_NAME_PATTERN = ".*" + INVALID_NAME_CHARACTERS + ".*";

    private static final Set<String> RESERVED_NAMES = ImmutableSet.of(".", "..");

    /** The maximum length for an item name.  This is not the maximum length
     *  of a <u>path</u>, just the maximum length of a single item or folder's
     *  name. */
    public static final int MAX_NAME_LENGTH = 255;

    /** Validates a proposed item name.  Names must be less than
     *  {@link #MAX_NAME_LENGTH} characters long, must contain non-whitespace
     *  characters, and may not contain any characters banned in XML or
     *  contained in {@link #INVALID_NAME_CHARACTERS} (':', '/', '"', '\t',
     *  '\r', '\n').
     *
     * @param name  The proposed item name.
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><tt>mail.INVALID_NAME</tt> - if the name is not acceptable</ul>
     * @return the passed-in name with trailing whitespace stripped.
     * @see StringUtil#stripControlCharacters(String) */
    static String validateItemName(String name) throws ServiceException {
        // reject invalid characters in the name
        if (name == null || name != StringUtil.stripControlCharacters(name) || name.matches(INVALID_NAME_PATTERN)) {
            throw MailServiceException.INVALID_NAME(name);
        }
        // strip trailing whitespace and validate length of resulting name
        //   (also, rule out "." and ".." due to UNIX directory confusion)
        String trimmed = StringUtil.trimTrailingSpaces(name);
        if (trimmed.isEmpty() || trimmed.length() > MAX_NAME_LENGTH || RESERVED_NAMES.contains(trimmed)) {
            throw MailServiceException.INVALID_NAME(name);
        }
        return trimmed;
    }

    public static String normalizeItemName(String name) {
        try {
            return validateItemName(name);
        } catch (ServiceException e) {
            String normalized = StringUtil.stripControlCharacters(name);
            if (normalized == null) {
                normalized = "";
            }
            if (normalized.length() > MailItem.MAX_NAME_LENGTH) {
                normalized = normalized.substring(0, MailItem.MAX_NAME_LENGTH);
            }
            if (normalized.matches(INVALID_NAME_PATTERN)) {
                normalized = normalized.replaceAll(INVALID_NAME_CHARACTERS, "");
            }
            normalized = StringUtil.trimTrailingSpaces(normalized);
            if (RESERVED_NAMES.contains(normalized)) {
                normalized = "";
            }

            if (normalized.trim().equals("")) {
                normalized = "item" + System.currentTimeMillis();
            }
            return normalized;
        }
    }

    /** Renames the item in place.  Altering an item's name's case (e.g.
     *  from <tt>foo</tt> to <tt>FOO</tt>) is allowed.
     *
     * @param name  The new name for this item.
     * @perms {@link ACL#RIGHT_WRITE} on the item
     * @throws ServiceException   The following error codes are possible:<ul>
     *    <li><tt>mail.IMMUTABLE_OBJECT</tt> - if the item can't be renamed
     *    <li><tt>mail.ALREADY_EXISTS</tt> - if a different item by that name
     *        already exists in the current folder
     *    <li><tt>mail.INVALID_NAME</tt> - if the new item's name is invalid
     *    <li><tt>service.FAILURE</tt> - if there's a database failure
     *    <li><tt>service.PERM_DENIED</tt> - if you don't have sufficient
     *        permissions</ul>
     * @see #validateItemName(String) */
    void rename(String name) throws ServiceException {
        rename(name, getFolder());
    }

    /** Renames the item and optionally moves it.  Altering an item's case
     *  (e.g. from <tt>foo</tt> to <tt>FOO</tt>) is allowed.  If you don't
     *  want the item to be moved, you must pass <tt>folder.getFolder()</tt>
     *  as the second parameter.
     *
     * @param newName  The new name for this item.
     * @param target   The new parent folder to move this item to.
     * @perms {@link ACL#RIGHT_WRITE} on the item to rename it,
     *        {@link ACL#RIGHT_DELETE} on the parent folder and
     *        {@link ACL#RIGHT_INSERT} on the target folder to move it
     * @throws ServiceException   The following error codes are possible:<ul>
     *    <li><tt>mail.IMMUTABLE_OBJECT</tt> - if the item can't be renamed
     *    <li><tt>mail.ALREADY_EXISTS</tt> - if a different item by that name
     *        already exists in the target folder
     *    <li><tt>mail.INVALID_NAME</tt> - if the new item's name is invalid
     *    <li><tt>service.FAILURE</tt> - if there's a database failure
     *    <li><tt>service.PERM_DENIED</tt> - if you don't have sufficient
     *        permissions</ul>
     * @see #validateItemName(String)
     * @see #move(Folder) */
    void rename(String newName, Folder target) throws ServiceException {
        String name = validateItemName(newName);

        boolean renamed = !name.equals(getName());
        boolean moved   = target != getFolder();

        if (!renamed && !moved)
            return;

        if (moved && target.getId() != Mailbox.ID_FOLDER_TRASH && target.getId() != Mailbox.ID_FOLDER_SPAM && !target.canAccess(ACL.RIGHT_INSERT)) {
            throw ServiceException.PERM_DENIED("you do not have the required rights on the target item");
        } else if (moved && !canAccess(ACL.RIGHT_DELETE)) {
            throw ServiceException.PERM_DENIED("you do not have the required rights on the item");
        } else if (renamed && !canAccess(ACL.RIGHT_WRITE)) {
            throw ServiceException.PERM_DENIED("you do not have the required rights on the item");
        }

        if (renamed) {
            if (getName() == null) {
                throw MailServiceException.CANNOT_RENAME(getType());
            } else if (!isMutable()) {
                throw MailServiceException.IMMUTABLE_OBJECT(mId);
            }

            try {
                MailItem conflict = mMailbox.getItemByPath(null, name, target.getId());
                if (conflict != null && conflict != this) {
                    throw MailServiceException.ALREADY_EXISTS(name);
                }
            } catch (MailServiceException.NoSuchItemException nsie) { }

            if (ZimbraLog.mailop.isDebugEnabled()) {
                ZimbraLog.mailop.debug("renaming " + getMailopContext(this) + " to " + name);
            }

            // XXX: note that we don't update mData.folderId here, as we need the subsequent
            //   move() to execute (it does several things that this code does not)

            markItemModified(Change.NAME);
            state.setName(name);
            state.setSubject(name);
            state.setDateChanged(mMailbox.getOperationTimestamp());
            metadataChanged();

            saveName(target.getId());
        }

        if (moved) {
            move(target);
        }
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
     * @param target  The folder to move the item to.
     * @perms {@link ACL#RIGHT_INSERT} on the target folder,
     *        {@link ACL#RIGHT_DELETE} on the source folder
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><tt>mail.IMMUTABLE_OBJECT</tt> - if the item is not movable
     *    <li><tt>mail.CANNOT_CONTAIN</tt> - if the target folder can't
     *        hold the item
     *    <li><tt>service.FAILURE</tt> - if there's a database failure
     *    <li><tt>service.PERM_DENIED</tt> - if you don't have sufficient
     *        permissions</ul>
     * @return whether anything was actually moved */
    boolean move(Folder target) throws ServiceException {
        if (getFolderId() == target.getId()) {
            return false;
        }
        markItemModified(Change.FOLDER);
        if (!isMovable()) {
            throw MailServiceException.IMMUTABLE_OBJECT(mId);
        }
        if (!target.canContain(this)) {
            throw MailServiceException.CANNOT_CONTAIN();
        }
        Folder oldFolder = getFolder();
        if (!oldFolder.canAccess(ACL.RIGHT_DELETE)) {
            throw ServiceException.PERM_DENIED("you do not have the required rights on the source folder");
        }
        if (target.getId() != Mailbox.ID_FOLDER_TRASH && target.getId() != Mailbox.ID_FOLDER_SPAM &&
                !target.canAccess(ACL.RIGHT_INSERT)) {
            throw ServiceException.PERM_DENIED("you do not have the required rights on the target folder");
        }
        if (isLeafNode()) {
            boolean isDeleted = isTagged(Flag.FlagInfo.DELETED);
            oldFolder.updateSize(-1, isDeleted ? -1 : 0, -getTotalSize());
            target.updateSize(1, isDeleted ? 1 : 0, getTotalSize());
        }

        if (!inTrash() && target.inTrash()) {
            // moving something to Trash also marks it as read
            if (isUnread()) {
                alterUnread(false);
            }
        } else {
            boolean isDeleted = isTagged(Flag.FlagInfo.DELETED);
            int unread = getUnreadCount();
            oldFolder.updateUnread(-unread, isDeleted ? -unread : 0);
            target.updateUnread(unread, isDeleted? unread : 0);
        }
        // moving a message (etc.) to Spam removes it from its conversation
        if (!inSpam() && target.inSpam()) {
            detach();
        }
        // item moved out of spam, so update the index id (will be written to DB in DbMailItem.setFolder());
        if (inSpam() && !target.inSpam() && getIndexStatus() == IndexStatus.DONE) {
            mMailbox.indexItem(this);
        }

        ZimbraLog.mailop.info("moving " + getMailopContext(this) + " to " + getMailopContext(target));
        DbMailItem.setFolder(this, target);
        folderChanged(target, 0);
        return true;
    }

    /** Records all relevant changes to the in-memory object for when an item
     *  gets moved to a new {@link Folder}.  Does <u>not</u> persist those
     *  changes to the database.
     *
     * @param newFolder  The folder the item is being moved to.
     * @param imapId     The new IMAP ID for the item after the operation.
     * @throws ServiceException if we're not in a transaction */
    void folderChanged(Folder newFolder, int imapId) throws ServiceException {
        if (getFolderId() == newFolder.getId()) {
            return;
        }
        markItemModified(Change.FOLDER);
        state.setFolderId(newFolder.getId());
        state.setImapId(mMailbox.isTrackingImap() ? imapId : getImapUid());
        metadataChanged();
    }

    void addChild(MailItem child) throws ServiceException {
        markItemModified(Change.CHILDREN);
        if (!canParent(child)) {
            throw MailServiceException.CANNOT_PARENT();
        }
        if (mMailbox != child.getMailbox()) {
            throw MailServiceException.WRONG_MAILBOX();
        }
    }

    /**
     * @throws ServiceException subclass may throw
     */
    void removeChild(MailItem child) throws ServiceException {
        markItemModified(Change.CHILDREN);

        // remove parent reference from the child
        if (child.getParentId() == mId) {
            child.state.setParentId(-1);
        }
    }

    /** A record of all the relevant data about a set of items that we're
     *  in the process of deleting via a call to {@link MailItem#delete}. */
    public static class PendingDelete {
        /** a bitmask of all the types of MailItems that are being deleted.
         * see {@link MailItem#typeToBitmask} */
        public int deletedTypes;

        /** Whether some of the item's children are not being deleted. */
        public boolean incomplete;

        /** The total size of all the items being deleted. */
        public long size;

        /** The number of {@link Contact}s being deleted. */
        public int contacts;

        /** The ids of all items being deleted. */
        public TypedIdList itemIds = new TypedIdList();

        /** The ids of all unread items being deleted.  This is a subset of
         *  {@link #itemIds}. */
        public List<Integer> unreadIds = new ArrayList<Integer>(1);

        /** The ids of all items that must be deleted but whose deletion
         *  must be deferred because of foreign key constraints. (E.g.
         *  {@link Conversation}s whose messages are all deleted during a
         *  {@link Folder} delete.) */
        public List<Integer> cascadeIds;

        /** The ids of all items that have been <u>modified</u> but not deleted
         *  during the delete.  (E.g. {@link Conversation}s whose messages are
         *  <b>not</b> all deleted during a {@link Folder} delete.)  */
        public Set<Integer> modifiedIds = new HashSet<Integer>(2);

        /** The document ids that need to be removed from the index. */
        public final List<ItemIndexDeletionInfo> indexIds = new ArrayList<ItemIndexDeletionInfo>(1);

        /** The ids of all items with the {@link Flag#BITMASK_COPIED} flag being
         *  deleted.  Items in <tt>sharedIndex</tt> whose last copies are
         *  being removed are added to {@link #indexIds} via a call to
         *  {@link DbMailItem#resolveSharedIndex}. */
        public Set<ItemIndexDeletionInfo> sharedIndex;

        /** The {@link com.zimbra.cs.store.Blob}s for all items being deleted that have content
         *  persisted in the store. */
        public List<MailboxBlob> blobs = new ArrayList<MailboxBlob>(1);

        /** Maps {@link Folder} ids to {@link DbMailItem.LocationCount}s
         *  tracking various per-folder counts for items being deleted. */
        public Map<Integer, DbMailItem.LocationCount> folderCounts = Maps.newHashMapWithExpectedSize(1);

        /** Maps {@link Tag} names to {@link DbMailItem.LocationCount}s
         *  tracking various per-tag counts for items being deleted. */
        public Map<String, DbMailItem.LocationCount> tagCounts = Maps.newHashMapWithExpectedSize(1);

        /** Digests of all blobs being deleted. */
        public Set<String> blobDigests = new HashSet<String>(2);

        /** Combines the data from another <tt>PendingDelete</tt> into
         *  this object.  The other <tt>PendingDelete</tt> is unmodified.
         *
         * @return this item */
        PendingDelete add(PendingDelete other) {
            if (other != null) {
                deletedTypes |= other.deletedTypes;
                incomplete   |= other.incomplete;

                size     += other.size;
                contacts += other.contacts;

                itemIds.addAll(other.itemIds);
                unreadIds.addAll(other.unreadIds);
                modifiedIds.addAll(other.modifiedIds);
                indexIds.addAll(other.indexIds);
                blobs.addAll(other.blobs);
                blobDigests.addAll(other.blobDigests);

                if (other.cascadeIds != null) {
                    (cascadeIds == null ? cascadeIds = new ArrayList<Integer>(other.cascadeIds.size()) : cascadeIds).addAll(other.cascadeIds);
                }
                if (other.sharedIndex != null) {
                    (sharedIndex == null ? sharedIndex = new HashSet<ItemIndexDeletionInfo>(other.sharedIndex.size()) : sharedIndex).addAll(other.sharedIndex);
                }

                for (Map.Entry<Integer, DbMailItem.LocationCount> entry : other.folderCounts.entrySet()) {
                    DbMailItem.LocationCount lcount = folderCounts.get(entry.getKey());
                    if (lcount == null) {
                        folderCounts.put(entry.getKey(), new DbMailItem.LocationCount(entry.getValue()));
                    } else {
                        lcount.increment(entry.getValue());
                    }
                }
                for (Map.Entry<String, DbMailItem.LocationCount> entry : other.tagCounts.entrySet()) {
                    DbMailItem.LocationCount lcount = tagCounts.get(entry.getKey());
                    if (lcount == null) {
                        tagCounts.put(entry.getKey(), new DbMailItem.LocationCount(entry.getValue()));
                    } else {
                        lcount.increment(entry.getValue());
                    }
                }
            }
            return this;
        }
    }

    void delete() throws ServiceException {
        delete(true);
    }

    void delete(boolean writeTombstones) throws ServiceException {
        if (!isDeletable()) {
            throw MailServiceException.IMMUTABLE_OBJECT(mId);
        }

        // get the full list of things that are being removed
        PendingDelete info = getDeletionInfo();
        assert(info != null && info.itemIds != null);
        if (info.incomplete) {
            // make sure to take the container's ID out of the list of deleted items
            info.itemIds.remove(getType(), mId);
        }

        delete(mMailbox, info, this, writeTombstones, inDumpster());
    }

    static void delete(Mailbox mbox, PendingDelete info, MailItem item, boolean writeTombstones, boolean fromDumpster)
    throws ServiceException {
        // short-circuit now if nothing's actually being deleted
        if (info.itemIds.isEmpty())
            return;

        mbox.markItemDeleted(info.itemIds);

        MailItem parent = null;
        // when applicable, record the deleted MailItem (rather than just its id)
        if (item != null && !info.incomplete) {
            item.markItemDeleted();
            try {
                parent = item.getParent();
            } catch (NoSuchItemException nsie) {
                ZimbraLog.mailbox.warn("NoSuchItemException while getting the parent folder of '%s' on mailbox '%d', not found in cache.", item.getName(), mbox.getId());
            }
        }

        if (!fromDumpster) {
            // update the mailbox's size
            mbox.updateSize(-info.size);
            mbox.updateContactCount(-info.contacts);

            // update conversations and unread counts on folders and tags
            if (item != null) {
                item.propagateDeletion(info);
            } else {
                // update message counts
                List<UnderlyingData> unreadData = DbMailItem.getById(mbox, info.unreadIds, Type.MESSAGE);
                for (UnderlyingData data : unreadData) {
                    // Note: Previous code was: MailItem unread = mbox.getItem(data.setFlag(Flag.FlagInfo.UNCACHED));
                    //
                    // We used to mark "data" as uncached, but that was only causing more SQL queries to be run
                    // because of virtual conversation logic.  A message that doesn't belong to a conversation
                    // doesn't have a parent, but getParent() tries to create a virtual conversation out of the
                    // message and fetches the message.  If message isn't found in the cache it must hit the
                    // database again.  So by adding the message object here, we prevent a redundant SQL query.
                    // For messages belonging to a real conversation we will have unnecessarily added it to
                    // the cache during deletion, causing cache churn.  But that's probably better than running
                    // extra SQL queries for potentially a very large number of messages being deleted.
                    MailItem unread = mbox.getItem(data);
                    unread.updateUnread(-data.unreadCount, unread.isTagged(Flag.FlagInfo.DELETED) ? -data.unreadCount : 0);
                }

                for (Map.Entry<Integer, DbMailItem.LocationCount> entry : info.folderCounts.entrySet()) {
                    int folderID = entry.getKey();
                    DbMailItem.LocationCount lcount = entry.getValue();
                    mbox.getFolderById(folderID).updateSize(-lcount.count, -lcount.deleted, -lcount.size);
                }
                for (Map.Entry<String, DbMailItem.LocationCount> entry : info.tagCounts.entrySet()) {
                    String tag = entry.getKey();
                    DbMailItem.LocationCount lcount = entry.getValue();
                    mbox.getTagByName(tag).updateSize(-lcount.count, -lcount.deleted);
                }

                // we're doing an old-item expunge or the like rather than a single delete/empty op
                info.cascadeIds = DbMailItem.markDeletionTargets(mbox, info.itemIds.getIds(EnumSet.of(Type.MESSAGE, Type.CHAT)), info.modifiedIds);
                if (info.cascadeIds != null) {
                    info.modifiedIds.removeAll(info.cascadeIds);
                }
            }
        }

        // Log mailop statements if necessary
        if (ZimbraLog.mailop.isInfoEnabled()) {
            if (item != null) {
                if (item instanceof VirtualConversation) {
                    ZimbraLog.mailop.info("Deleting Message (id=%d).", ((VirtualConversation) item).getMessageId());
                } else {
                    ZimbraLog.mailop.info("Deleting %s.", getMailopContext(item));
                }
            }

            // If there are any related items being deleted, log them in blocks of 200.
            int itemId = item == null ? 0 : Math.abs(item.getId()); // Use abs() for VirtualConversations
            Set<Integer> idSet = new TreeSet<Integer>();
            for (MailItem.Type type : info.itemIds.types()) {
                for (int id : info.itemIds.getIds(type)) {
                    id = Math.abs(id); // Use abs() for VirtualConversations
                    if (id != itemId) {
                        idSet.add(id);
                    }
                    if (idSet.size() >= 200) {
                        // More than 200 items.
                        ZimbraLog.mailop.info("Deleting %sS: %s.", type.name(), StringUtil.join(",", idSet));
                        idSet.clear();
                    }
                }
                if (idSet.size() > 0) {
                    // Less than 200 items or remainder.
                    ZimbraLog.mailop.info("Deleting %sS: %s.", type.name(), StringUtil.join(",", idSet));
                }
            }
        }

        // actually delete the items from the DB
        DbMailItem.delete(mbox, item, info, fromDumpster);

        // remove the deleted item(s) from the mailbox's cache
        if (item != null) {
            item.purgeCache(info, !info.incomplete);
            if (parent != null) {
                parent.removeChild(item);
            }
        } else if (!info.itemIds.isEmpty()) {
            // we're doing an old-item expunge or the like rather than a single delete/empty op
            for (int itemId : info.itemIds.getAllIds()) {
                mbox.uncacheItem(itemId);
            }

            if (!info.modifiedIds.isEmpty()) {
                mbox.purge(Type.CONVERSATION);
                // if there are SOAP listeners, instantiate all modified conversations for notification purposes
                if (mbox.hasListeners(Session.Type.SOAP)) {
                    for (MailItem conv : mbox.getItemById(info.modifiedIds, Type.CONVERSATION)) {
                        ((Conversation) conv).getSenderList();
                    }
                }
            }
        }

        // also delete any conversations whose messages have all been removed
        if (info.cascadeIds != null && !info.cascadeIds.isEmpty()) {
            for (Integer convId : info.cascadeIds) {
                mbox.markItemDeleted(Type.CONVERSATION, convId, Mailbox.ID_FOLDER_CONVERSATIONS);
                mbox.uncacheItem(convId);
            }
            try {
                DbMailItem.delete(mbox, info.cascadeIds, false);
            } catch (ServiceException se) {
                MailboxErrorUtil.handleCascadeFailure(mbox, info.cascadeIds, se);
            }
            // conversations don't have UUIDs, so this is safe
            for (Integer id : info.cascadeIds) {
                info.itemIds.add(Type.CONVERSATION, id, null);
            }
        }

        // deal with index sharing
        if (info.sharedIndex != null && !info.sharedIndex.isEmpty()) {
            DbMailItem.resolveSharedIndex(mbox, info);
        }

        mbox.markOtherItemDirty(info);

        // write a deletion record for later sync
        if (writeTombstones && mbox.isTrackingSync() && !info.itemIds.isEmpty() && !fromDumpster) {
            DbMailItem.writeTombstones(mbox, info.itemIds);
        }

        // don't actually delete the blobs or index entries here; wait until after the commit
    }

    static String getMailopContext(MailItem item) {
        if (item == null || !ZimbraLog.mailop.isInfoEnabled()) {
            return "<undefined>";
        } else if (item instanceof Folder || item instanceof Tag || item instanceof WikiItem) {
            return String.format("%s %s (id=%d)", item.getClass().getSimpleName(), item.getName(), item.getId());
        } else if (item instanceof Contact) {
            String email = ((Contact) item).get(ContactConstants.A_email);
            if (StringUtil.isNullOrEmpty(email)) {
                email = "<undefined>";
            }
            return String.format("%s %s (id=%d)", item.getClass().getSimpleName(), email, item.getId());
        } else {
            return String.format("%s (id=%d)", item.getClass().getSimpleName(), item.getId());
        }
    }

    /** Determines the set of items to be deleted.  Assembles a new
     *  {@link PendingDelete} object encapsulating the data on the items
     *  to be deleted.  If the caller has specified the maximum change
     *  number they know about, this set will also exclude any item for
     *  which the (modification/content) change number is greater.
     *
     * @perms {@link ACL#RIGHT_DELETE} on the item
     * @return A fully-populated <tt>PendingDelete</tt> object. */
    PendingDelete getDeletionInfo() throws ServiceException {
        if (!canAccess(ACL.RIGHT_DELETE)) {
            throw ServiceException.PERM_DENIED("you do not have the required rights on the item");
        }

        Integer id = Integer.valueOf(mId);
        PendingDelete info = new PendingDelete();
        info.size   = getTotalSize();
        info.itemIds.add(getType(), id, getFolderId(), uuid);

        if (!inDumpster()) {
            if (getUnreadCount() != 0 && mMailbox.getFlagById(Flag.ID_UNREAD).canTag(this)) {
                info.unreadIds.add(id);
            }
            boolean isDeleted = isTagged(Flag.FlagInfo.DELETED);
            info.folderCounts.put(getFolderId(), new DbMailItem.LocationCount(1, isDeleted ? 1 : 0, info.size));
            for (String tag : getTags()) {
                info.tagCounts.put(tag, new DbMailItem.LocationCount(1, isDeleted ? 1 : 0, info.size));
            }
        }

        // Clean up from blob store and Lucene if:
        //   1) deleting a regular item and dumpster is not in use, OR
        //   2) permantently deleting an item from dumpster
        // In other words, skip the blob/index deletes when soft-deleting item to dumpster.
        if (!getMailbox().dumpsterEnabled() || inDumpster() ||
            getFolderId() == Mailbox.ID_FOLDER_DRAFTS || (inSpam() && !getMailbox().useDumpsterForSpam())) {
            if (getIndexStatus() != IndexStatus.NO) {
                int indexId = getIndexStatus() == IndexStatus.DONE ? getIndexId() : mId;
                int numIndexDocs = state.getNumIndexDocs();
                if (isTagged(Flag.FlagInfo.COPIED)) {
                    info.sharedIndex = Sets.newHashSet(new ItemIndexDeletionInfo(indexId, numIndexDocs, getType()));
                } else {
                    info.indexIds.add(new ItemIndexDeletionInfo(indexId, numIndexDocs, getType()));
                }
            }

            List<MailItem> items = new ArrayList<MailItem>(3);
            items.add(this);
            items.addAll(loadRevisions());
            for (MailItem revision : items) {
                try {
                    info.blobs.add(revision.getBlob());
                } catch (Exception e) {
                    ZimbraLog.mailbox.error("missing blob for id: " + mId + ", change: " + revision.getSavedSequence());
                }
            }
        }

        return info;
    }

    private static final int UNREAD_ITEM_BATCH_SIZE = 500;

    void propagateDeletion(PendingDelete info) throws ServiceException {
        if (!info.unreadIds.isEmpty()) {
            for (int i = 0, count = info.unreadIds.size(); i < count; i += UNREAD_ITEM_BATCH_SIZE) {
                List<Integer> batch = info.unreadIds.subList(i, Math.min(i + UNREAD_ITEM_BATCH_SIZE, count));
                for (UnderlyingData data : DbMailItem.getById(mMailbox, batch, Type.MESSAGE)) {
                    Message msg = (Message) mMailbox.getItem(data);
                    if (msg.isUnread()) {
                        msg.updateUnread(-1, msg.isTagged(Flag.FlagInfo.DELETED) ? -1 : 0);
                    }
                    mMailbox.uncache(msg);
                }
            }
        }

        for (Map.Entry<Integer, DbMailItem.LocationCount> entry : info.folderCounts.entrySet()) {
            Folder folder = mMailbox.getFolderById(entry.getKey());
            DbMailItem.LocationCount lcount = entry.getValue();
            folder.updateSize(-lcount.count, -lcount.deleted, -lcount.size);
        }
        for (Map.Entry<String, DbMailItem.LocationCount> entry : info.tagCounts.entrySet()) {
            Tag tag = mMailbox.getTagByName(entry.getKey());
            DbMailItem.LocationCount lcount = entry.getValue();
            tag.updateSize(-lcount.count, -lcount.deleted);
        }
    }

    void purgeCache(PendingDelete info, boolean purgeItem) throws ServiceException {
        if (purgeItem) {
            mMailbox.uncache(this);
            for (int itemId : info.itemIds.getAllIds()) {
                mMailbox.uncacheItem(itemId);
            }
        }
    }


    private static final String CUSTOM_META_PREFIX = Metadata.FN_EXTRA_DATA + ".";

    protected boolean trackUserAgentInMetadata() {
        return false;
    }

    Metadata encodeMetadata() throws ServiceException {
        Metadata meta = encodeMetadata(new Metadata());
        if (trackUserAgentInMetadata()) {
            OperationContext octxt = getMailbox().getOperationContext();
            if (octxt != null) {
                meta.put(Metadata.FN_USER_AGENT, octxt.getUserAgent());
            }
        }
        return meta;
    }

    abstract Metadata encodeMetadata(Metadata meta);

    static Metadata encodeMetadata(Metadata meta, Color color, ACL rights, int metaVersion, int version, CustomMetadataList extended) {
        return encodeMetadata(meta, color, rights, metaVersion, version, 0, extended);
    }

    static Metadata encodeMetadata(Metadata meta, Color color, ACL rights, int metaVersion, int version, int numIndexDocs, CustomMetadataList extended) {
        if (color != null && color.getMappedColor() != DEFAULT_COLOR) {
            meta.put(Metadata.FN_COLOR, color.toMetadata());
        }
        if (metaVersion > 1) {
            meta.put(Metadata.FN_METADATA_VERSION, metaVersion);
        }
        if (version > 1) {
            meta.put(Metadata.FN_VERSION, version);
        }
        if (extended != null) {
            for (Pair<String, String> mpair : extended) {
                meta.put(CUSTOM_META_PREFIX + mpair.getFirst(), mpair.getSecond());
            }
        }
        if (rights != null) {
            meta.put(Metadata.FN_RIGHTS_MAP, rights.encode());
        }

        if (numIndexDocs > 0) {
            meta.put(Metadata.FN_NUM_INDEX_DOCS, numIndexDocs);
        }
        return meta;
    }

    void decodeMetadata(String metadata) throws ServiceException {
        try {
            decodeMetadata(new Metadata(metadata, mId));
        } catch (ServiceException e) {
            ZimbraLog.mailbox.error("Failed to parse metadata id=%d,type=%s", mId, getType(), e);
            throw e;
        }
    }

    void decodeMetadata(Metadata meta) throws ServiceException {
        if (meta == null)
            return;

        state.setColor(Color.fromMetadata(meta.getLong(Metadata.FN_COLOR, DEFAULT_COLOR)), AccessMode.LOCAL_ONLY);
        state.setMetadataVersion((int) meta.getLong(Metadata.FN_METADATA_VERSION, 1), AccessMode.LOCAL_ONLY);
        state.setVersion((int) meta.getLong(Metadata.FN_VERSION, 1), AccessMode.LOCAL_ONLY);

        mExtendedData = null;
        for (Map.Entry<String, ?> entry : meta.asMap().entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(CUSTOM_META_PREFIX)) {
                if (mExtendedData == null) {
                    mExtendedData = new CustomMetadataList();
                }
                mExtendedData.addSection(key.substring(CUSTOM_META_PREFIX.length()), entry.getValue().toString());
            }
        }

        ACL acl = null;
        if (meta.containsKey(Metadata.FN_RIGHTS_MAP)) {
            //new format
            acl = makeACLFromMap(Metadata.FN_RIGHTS_MAP, meta);
        } else if (meta.containsKey(Metadata.FN_RIGHTS)) {
            try {
                //try the HELIX list format
                MetadataList mlistACL = meta.getList(Metadata.FN_RIGHTS, true);
                if (mlistACL != null) {
                    acl = new ACL(mlistACL);
                }
            } catch (ServiceException se) {
                //map may exist in old attr for a short time between bug 60048 and bug 68928
                ZimbraLog.mailbox.warn("Metadata.FN_RIGHTS exists, but is not list. Should never see this outside CF/DF!");
                acl = makeACLFromMap(Metadata.FN_RIGHTS, meta);
            }
        }
        if (acl != null) {
            state.setRights(acl.isEmpty() ? null : acl, AccessMode.LOCAL_ONLY);
            if (!isTagged(Flag.FlagInfo.NO_INHERIT)) {
                alterTag(mMailbox.getFlagById(Flag.ID_NO_INHERIT), true);
            }
        }
        int numIndexDocs = meta.getInt(Metadata.FN_NUM_INDEX_DOCS, 0);
        if (numIndexDocs > 0) {
            state.setNumIndexDocs(numIndexDocs, AccessMode.LOCAL_ONLY);
        }
    }

    private ACL makeACLFromMap(String key, Metadata meta) throws ServiceException {
        Metadata aclMetaData = meta.getMap(key, true);
        if (aclMetaData != null) {
            return new ACL(aclMetaData);
        } else {
            return null;
        }
    }


    protected void saveMetadata() throws ServiceException {
        saveMetadata(encodeMetadata().toString());
    }

    protected void saveMetadata(String metadata) throws ServiceException {
        metadataChanged();
        if (ZimbraLog.mailop.isDebugEnabled()) {
            ZimbraLog.mailop.debug("saving metadata for " + getMailopContext(this));
        }
        state.saveMetadata(this, metadata);
    }

    protected void saveName() throws ServiceException {
        saveName(getFolderId());
    }

    protected void saveName(int folderId) throws ServiceException {
        DbMailItem.saveName(this, folderId, encodeMetadata());
    }

    protected void saveData(DbMailItem data) throws ServiceException {
        saveData(data, encodeMetadata());
    }

    protected void saveData(DbMailItem data, Metadata metadata) throws ServiceException {
        assert(metadata != null);
        metadataChanged();
        if (ZimbraLog.mailop.isDebugEnabled()) {
            ZimbraLog.mailop.debug("saving data for %s", getMailopContext(this));
        }
        data.update(this, metadata);
    }

    void markMetadataChanged() throws ServiceException {
        saveData(new DbMailItem(mMailbox));
    }

    /**
     * Locks this MailItem with exclusive write lock.
     * When a MailItem is locked, only the user who locked the item
     * can move the item or change the content.
     *
     * @param authuser
     * @throws ServiceException
     */
    void lock(Account authuser) throws ServiceException {
        throw MailServiceException.CANNOT_LOCK(mId);
    }

    /**
     * Unlocks this MailItem.  The user who previously locked
     * the item, or anyone who has admin privilige to this
     * MailItem can perform unlock operation.
     *
     * @param authuser
     * @throws ServiceException
     */
    void unlock(Account authuser) throws ServiceException {
        throw MailServiceException.CANNOT_UNLOCK(mId);
    }

    List<Comment> getComments(SortBy sortBy, int offset, int length) throws ServiceException {
        List<UnderlyingData> listData = DbMailItem.getByParent(this, sortBy, -1, inDumpster());
        ArrayList<Comment> comments = new ArrayList<Comment>();
        for (UnderlyingData data : listData) {
            MailItem item = mMailbox.getItem(data);
            if (item instanceof Comment) {
                comments.add((Comment)item);
            }
        }
        if (comments.size() <= offset) {
            return Collections.<Comment>emptyList();
        }
        int last = length == -1 ? comments.size() : Math.min(comments.size(), offset + length);
        return comments.subList(offset, last);
    }

    public Metadata serializeUnderlyingData() {
        Metadata meta = state.getUnderlyingData().serialize();
        // metadata
        Metadata metaMeta = new Metadata();
        encodeMetadata(metaMeta);
        meta.put(UnderlyingData.FN_METADATA, metaMeta.toString());
        return meta;
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
    private static final String CN_NAME         = "name";
    private static final String CN_COLOR        = "color";
    private static final String CN_METADATA_VERSION = "meta_version";
    private static final String CN_VERSION      = "version";
    private static final String CN_IMAP_ID      = "imap_id";
    private static final String CN_UUID         = "uuid";

    protected MoreObjects.ToStringHelper appendCommonMembers(MoreObjects.ToStringHelper helper) {
        UnderlyingData data = state.getUnderlyingData();
        helper.omitNullValues();
        helper.add(CN_ID, mId);
        helper.add(CN_TYPE, type);
        if (data.name != null) {
            helper.add(CN_NAME, data.name);
        }
        helper.add(CN_UNREAD_COUNT, data.unreadCount);
        if (data.getFlags() != 0) {
            helper.add(CN_FLAGS, getFlagString());
        }
        if (data.getTags().length != 0) {
            helper.add(CN_TAGS, Joiner.on(',').join(data.getTags()));
        }
        helper.add(CN_FOLDER_ID, data.folderId);
        helper.add(CN_SIZE, data.size);
        helper.add(CN_METADATA_VERSION, state.getMetadataVersion());
        helper.add(CN_VERSION, state.getVersion());
        if (data.parentId > 0) {
            helper.add(CN_PARENT_ID, data.parentId);
        }
        Color color = state.getColor();
        if (state.getColor() != null) {
            helper.add(CN_COLOR, color.getMappedColor());
        }
        helper.add(CN_SUBJECT, data.getSubject());
        helper.add(CN_BLOB_DIGEST, getDigest());
        if (data.imapId > 0) {
            helper.add(CN_IMAP_ID, data.imapId);
        }
        helper.add(CN_DATE, data.date);
        helper.add(CN_REVISION, data.modContent);
        helper.add(CN_UUID, uuid);
        return helper;
    }

    public static Set<Integer> toId(Set<? extends MailItem> items) {
        if (items == null)
            return null;

        Set<Integer> result = new HashSet<Integer>(items.size());
        for (MailItem item : items) {
            result.add(item.getId());
        }
        return result;
    }

    public static List<Integer> toId(List<? extends MailItem> items) {
        if (items == null)
            return null;

        List<Integer> result = new ArrayList<Integer>(items.size());
        for (MailItem item : items) {
            result.add(item.getId());
        }
        return result;
    }

    /**
     * Returns a copy of the item with {@link Flag#BITMASK_UNCACHED} set.
     *
     * @return
     * @throws ServiceException
     * @see Mailbox#snapshotItem(MailItem)
     */
    public MailItem snapshotItem() throws ServiceException {
        UnderlyingData data = getUnderlyingData().clone();
        data.setFlag(Flag.FlagInfo.UNCACHED);
        return MailItem.constructItem(mMailbox, data);
    }

    protected short checkACL(short rightsNeeded, Account authuser, boolean asAdmin) throws ServiceException {
        // check the ACLs to see if access has been explicitly granted
        ACL rights = state.getRights();
        Short granted = rights != null ? rights.getGrantedRights(authuser) : null;
        short subset;
        if (granted != null) {
            subset = (short) (granted.shortValue() & rightsNeeded);
            if (ZimbraLog.acl.isTraceEnabled()) {
                ZimbraLog.acl.trace("checkACL '%s' returned=%s granted=%s needed=%s (R)",
                        getPath(), subset, granted, rightsNeeded);
            }
            return (short) (granted.shortValue() & rightsNeeded);
        }
        // no ACLs apply; can we check parent folder for inherited rights?
        if (mId == Mailbox.ID_FOLDER_ROOT) {
            if (ZimbraLog.acl.isTraceEnabled()) {
                ZimbraLog.acl.trace("checkACL '%s' returned=0 granted=0 needed=%s (ROOT)", getPath(), rightsNeeded);
            }
            return 0;
        }
        if (isTagged(Flag.FlagInfo.NO_INHERIT)) {
            if (ZimbraLog.acl.isTraceEnabled()) {
                ZimbraLog.acl.trace("checkACL '%s' returned=0 granted=0 needed=%s (no inherit)",
                        getPath(), rightsNeeded);
            }
            return 0;
        }
        granted = getParent().checkACL(rightsNeeded, authuser, asAdmin);
        subset = (short) (granted.shortValue() & rightsNeeded);
        if (ZimbraLog.acl.isTraceEnabled()) {
            ZimbraLog.acl.trace("checkACL '%s' returned=%s granted=%s needed=%s (P)",
                    getPath(), subset, granted, rightsNeeded);
        }
        return granted;
    }

    /** Grants the specified set of rights to the target and persists them
     *  to the database.
     *
     *
     * @param zimbraId  The zimbraId of the entry being granted rights.
     * @param type      The type of principal the grantee's ID refers to.
     * @param rights    A bitmask of the rights being granted.
     * @param expiry    Time when grant expires. Value of 0 means grant never expires.
     * @perms {@link ACL#RIGHT_ADMIN} on the item
     * @throws ServiceException The following error codes are possible:<ul>
     *    <li><tt>service.FAILURE</tt> - if there's a database failure
     *    <li><tt>service.PERM_DENIED</tt> - if you don't have sufficient
     *        permissions</ul> */
    ACL.Grant grantAccess(String zimbraId, byte type, short rights, String args, long expiry) throws ServiceException {
        Account account = getAccount();
        if (type == ACL.GRANTEE_PUBLIC && !account.isPublicSharingEnabled()) {
            throw ServiceException.PERM_DENIED("public sharing not allowed");
        }
        if (type == ACL.GRANTEE_GUEST &&
                (!account.isExternalSharingEnabled() || !isAllowedExternalDomain(zimbraId))) {
            throw ServiceException.PERM_DENIED("external sharing not allowed");
        }
        if (!canAccess(ACL.RIGHT_ADMIN)) {
            throw ServiceException.PERM_DENIED("you do not have admin rights to item " + getPath());
        }
        if (type == ACL.GRANTEE_USER && zimbraId.equalsIgnoreCase(getMailbox().getAccountId())) {
            throw ServiceException.PERM_DENIED("cannot grant access to the owner of the item");
        }
        // if there's an ACL on the item, the item does not inherit from its parent
        alterTag(mMailbox.getFlagById(Flag.ID_NO_INHERIT), true);

        markItemModified(Change.ACL);
        long intShareLifetime = getMaxAllowedInternalShareLifetime(account);
        long extShareLifetime = getMaxAllowedExternalShareLifetime(account);
        long now = System.currentTimeMillis();
        intShareLifetime = intShareLifetime == 0 ? 0 : now + intShareLifetime;
        extShareLifetime = extShareLifetime == 0 ? 0 : now + extShareLifetime;

        ACL acl = state.getRights();

        if (acl == null) {
            acl = new ACL(intShareLifetime, extShareLifetime);
            state.setRights(acl);
        } else {
            if (extShareLifetime != 0 &&
                (acl.getNumberOfGrantsByType(ACL.GRANTEE_GUEST) == 0 ||  acl.getGuestGrantExpiry() == 0)) {
                acl.setGuestGrantExpiry(extShareLifetime);
            }
            if (intShareLifetime != 0 &&
                (acl.getNumberOfGrantsByType(ACL.GRANTEE_USER) == 0 ||  acl.getInternalGrantExpiry() == 0)) {
                acl.setInternalGrantExpiry(intShareLifetime);
            }
        }

        ACL.Grant grant = acl.grantAccess(zimbraId, type, rights, args, expiry);
        state.setRights(acl, AccessMode.REMOTE_ONLY);
        saveMetadata();

        queueForAclPush();

        return grant;
    }

    /** Returns the maximum allowed validity of a grant to external/guest users for this type of item. */
    protected long getMaxAllowedExternalShareLifetime(Account account) {
        return account.getExternalShareLifetime();
    }

    /** Returns the maximum allowed validity of a grant to internal users for this type of item. */
    protected long getMaxAllowedInternalShareLifetime(Account account) {
        return account.getShareLifetime();
    }

    protected void queueForAclPush() throws ServiceException {
        DbPendingAclPush.queue(mMailbox, mId);
    }

    /** Removes the set of rights granted to the specified (id, type) pair
     *  and updates the database accordingly.
     *
     * @param zimbraId  The zimbraId of the entry being revoked rights.
     * @perms {@link ACL#RIGHT_ADMIN} on the item
     * @throws ServiceException The following error codes are possible:<ul>
     *    <li><tt>service.FAILURE</tt> - if there's a database failure
     *    <li><tt>service.PERM_DENIED</tt> - if you don't have sufficient
     *        permissions</ul> */
    void revokeAccess(String zimbraId) throws ServiceException {
        if (!canAccess(ACL.RIGHT_ADMIN)) {
            throw ServiceException.PERM_DENIED("you do not have admin rights to item " + getPath());
        }
        if (zimbraId.equalsIgnoreCase(getMailbox().getAccountId())) {
            throw ServiceException.PERM_DENIED("cannot revoke access from the owner of the item");
        }
        ACL acl = getEffectiveACL();
        if (acl == null || !acl.revokeAccess(zimbraId)) {
            return;
        }
        // if there's an ACL on the item, the item does not inherit from its parent
        alterTag(mMailbox.getFlagById(Flag.ID_NO_INHERIT), true);

        markItemModified(Change.ACL);
        ACL rights = state.getRights();
        rights.revokeAccess(zimbraId);
        if (rights.isEmpty()) {
            state.setRights(null);
        } else {
            if (rights.getNumberOfGrantsByType(ACL.GRANTEE_USER) == 0) {
                rights.setInternalGrantExpiry(0);
            }
            if (rights.getNumberOfGrantsByType(ACL.GRANTEE_GUEST) == 0) {
                rights.setGuestGrantExpiry(0);
            }
        }
        saveMetadata();

        queueForAclPush();
    }

    /** Replaces the item's {@link ACL} with the supplied one and updates the database accordingly.
     *
     * @param acl  The new ACL being applied (<tt>null</tt> is OK).
     * @perms {@link ACL#RIGHT_ADMIN} on the item
     * @throws ServiceException The following error codes are possible:<ul>
     *    <li><tt>service.FAILURE</tt> - if there's a database failure
     *    <li><tt>service.PERM_DENIED</tt> - if you don't have sufficient
     *        permissions</ul> */
    void setPermissions(ACL acl) throws ServiceException {
        if (!canAccess(ACL.RIGHT_ADMIN)) {
            throw ServiceException.PERM_DENIED("you do not have admin rights to item " + getPath());
        }
        // if we're setting an ACL on the folder, the folder does not inherit from its parent
        alterTag(mMailbox.getFlagById(Flag.ID_NO_INHERIT), true);

        markItemModified(Change.ACL);
        if (acl != null && acl.isEmpty()) {
            acl = null;
        }
        if (acl == null && state.getRights() == null) {
            return;
        }
        state.setRights(acl);
        saveMetadata();

        queueForAclPush();
    }

    /** Returns a copy of the ACL directly set on the item, or <tt>null</tt>
     *  if one is not set. */
    public ACL getACL() {
        ACL acl = state.getRights();
        return acl == null ? null : acl.duplicate();
    }

    /** Returns a copy of the ACL that applies to the item (possibly
     *  inherited from a parent), or <tt>null</tt> if one is not set. */
    public ACL getEffectiveACL() {
        MailItem parent = null;
        try {
            parent = getParent();
        } catch (ServiceException e) {}
        if (mId == Mailbox.ID_FOLDER_ROOT || isTagged(Flag.FlagInfo.NO_INHERIT) || parent == null) {
            return getACL();
        }
        return parent.getEffectiveACL();
    }

    void metadataChanged(boolean updateFolderMODSEQ) throws ServiceException {
        state.incrementMetadataVersion();
        state.metadataChanged(mMailbox, updateFolderMODSEQ);
    }

    void metadataChanged() throws ServiceException {
        metadataChanged(true);
    }

    void contentChanged() throws ServiceException {
        state.incrementMetadataVersion();
        state.contentChanged(mMailbox);
    }

    /**
     * @return the shared folder that this item belongs to, or null if the item is not under a share
     */
    public Folder getShare() throws ServiceException {
        Folder f = getFolder();
        while (f != null) {
            if (f.isShare()) {
                return f;
            } else if (f.getId() == Mailbox.ID_FOLDER_ROOT) {  // must check because the ROOT folder is self-parented
                return null;
            }
            f = f.getFolder();
        }
        return null;
    }

    /**
     * @param folderId
     * @return true if this item is in a subtree under the given folder
     * @throws ServiceException
     */
    boolean isUnder(int folderId) throws ServiceException {
        Folder f = getFolder();
        while (f != null) {
            int fid = f.getId();
            if (fid == folderId) {
                return true;
            } else if (fid == Mailbox.ID_FOLDER_ROOT) {  // must check because the ROOT folder is self-parented
                return false;
            }
            f = f.getFolder();
        }
        return false;
    }

    @Override
    public int getFolderIdInMailbox() throws ServiceException {
        return getFolderId();
    }

    @Override
    public String getAccountId() {
        return mMailboxData.accountId;
    }

    protected MailItemState initFieldCache(UnderlyingData data) {
        return new LocalMailItemState(data);
    }
}
