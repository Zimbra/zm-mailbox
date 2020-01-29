/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;

/**
 * @since Oct 15, 2004
 */
public final class Flag extends Tag {

    private static final char HIDDEN = '\0';
    private static final FlagInfo[] INDEX2FLAG = new FlagInfo[31];
    private static final FlagInfo[] CHAR2FLAG = new FlagInfo[127];
    private static final Map<String, FlagInfo> NAME2FLAG = new HashMap<String, FlagInfo>();

    public enum FlagInfo {
        FROM_ME(-1, "\\Sent", 's'),
        ATTACHED(-2, "\\Attached", 'a'),
        REPLIED(-3, "\\Answered", 'r'),
        FORWARDED(-4, "\\Forwarded", 'w'),
        COPIED(-5, "\\Copied", HIDDEN),
        FLAGGED(-6, "\\Flagged", 'f'),
        DRAFT(-7, "\\Draft", 'd'),
        DELETED(-8, "\\Deleted", 'x'),
        NOTIFIED(-9, "\\Notified", 'n'),
        /**
         * Callers of {@link Mailbox} methods treat {@code UNREAD} like the other flags. Internally, we break it out into
         * a separate variable, {@code unreadCount}. It's also persisted in a separate indexed column for fast lookups of
         * unread {@link MailItem}s.
         */
        UNREAD(-10, "\\Unread", 'u'),
        HIGH_PRIORITY(-11, "\\Urgent", '!'),
        LOW_PRIORITY(-12, "\\Bulk", '?'),
        VERSIONED(-13, "\\Versioned", '/'),
        /**
         * @deprecated Use indexId = 0
         */
        @Deprecated
        INDEXING_DEFERRED(-14, "\\IdxDeferred", HIDDEN),
        POPPED(-15, "\\Popped", 'p'),
        NOTE(-16, "\\Note", 't'),
        PRIORITY(-17, "\\Priority", '+'),
        POST(-18, "\\Post", '^'),
        MUTED(-19, "\\Muted", '('),
        SUBSCRIBED(-20, "\\Subscribed", '*'),
        EXCLUDE_FREEBUSY(-21, "\\ExcludeFB", 'b'),
        CHECKED(-22, "\\Checked", '#'),
        NO_INHERIT(-23, "\\NoInherit", 'i'),
        INVITE(-24, "\\Invite", 'v'),
        SYNCFOLDER(-25, "\\SyncFolder", 'y'),
        SYNC(-26, "\\Sync", '~'),
        NO_INFERIORS(-27, "\\Noinferiors", 'o'),
        /**
         * @deprecated support for ZD 1.x local data migration
         */
        @Deprecated
        ARCHIVED(-28, "\\Archived", '@'),
        GLOBAL(-29, "\\Global", 'g'),
        IN_DUMPSTER(-30,  "\\InDumpster", HIDDEN),
        UNCACHED(-31, "\\Uncached", HIDDEN)
        // -32 reserved for Tag.NONEXISTENT_TAG
        ;

        final String flagName;
        final int id;
        final char ch;
        final int bitmask;

        private FlagInfo(int id, String name, char ch) {
            this.id = id;
            this.flagName = name;
            this.ch = ch;
            this.bitmask = 1 << (-id - 1);

            INDEX2FLAG[getIndex(id)] = this;
            NAME2FLAG.put(name.toLowerCase(), this);
            if (ch != HIDDEN) {
                CHAR2FLAG[ch] = this;
            }
        }

        Flag toFlag(Mailbox mbox) throws ServiceException {
            UnderlyingData data = new UnderlyingData();
            data.id = id;
            data.type = MailItem.Type.FLAG.toByte();
            data.folderId = Mailbox.ID_FOLDER_TAGS;
            data.setFlags(BITMASK_UNCACHED);
            data.name = flagName;
            return new Flag(mbox, data, this);
        }

        @VisibleForTesting
        boolean isHidden() {
            return ch == HIDDEN;
        }

        public int toId() {
            return id;
        }

        public int toBitmask() {
            return bitmask;
        }

        public char getChar() {
            return this.ch;
        }

        @Override
        public String toString() {
            return flagName;
        }

        static FlagInfo of(String fname) {
            return NAME2FLAG.get(fname.toLowerCase());
        }

        static FlagInfo of(int id) {
            int index = getIndex(id);
            return index < 0 || index >= INDEX2FLAG.length ? null : INDEX2FLAG[index];
        }
    }

    public static final int ID_FROM_ME = FlagInfo.FROM_ME.id;
    public static final int ID_ATTACHED = FlagInfo.ATTACHED.id;
    public static final int ID_REPLIED = FlagInfo.REPLIED.id;
    public static final int ID_FORWARDED = FlagInfo.FORWARDED.id;
    public static final int ID_COPIED = FlagInfo.COPIED.id;
    public static final int ID_FLAGGED = FlagInfo.FLAGGED.id;
    public static final int ID_DRAFT = FlagInfo.DRAFT.id;
    public static final int ID_DELETED = FlagInfo.DELETED.id;
    public static final int ID_NOTIFIED = FlagInfo.NOTIFIED.id;
    public static final int ID_UNREAD = FlagInfo.UNREAD.id;
    public static final int ID_HIGH_PRIORITY = FlagInfo.HIGH_PRIORITY.id;
    public static final int ID_LOW_PRIORITY = FlagInfo.LOW_PRIORITY.id;
    public static final int ID_VERSIONED = FlagInfo.VERSIONED.id;
    @Deprecated
    public static final int ID_INDEXING_DEFERRED = FlagInfo.INDEXING_DEFERRED.id;
    public static final int ID_POPPED = FlagInfo.POPPED.id;
    public static final int ID_NOTE = FlagInfo.NOTE.id;
    public static final int ID_PRIORITY = FlagInfo.PRIORITY.id;
    public static final int ID_POST = FlagInfo.POST.id;
    public static final int ID_MUTED = FlagInfo.MUTED.id;
    public static final int ID_SUBSCRIBED = FlagInfo.SUBSCRIBED.id;
    public static final int ID_EXCLUDE_FREEBUSY = FlagInfo.EXCLUDE_FREEBUSY.id;
    public static final int ID_CHECKED = FlagInfo.CHECKED.id;
    public static final int ID_NO_INHERIT = FlagInfo.NO_INHERIT.id;
    public static final int ID_INVITE = FlagInfo.INVITE.id;
    public static final int ID_SYNCFOLDER = FlagInfo.SYNCFOLDER.id;
    public static final int ID_SYNC = FlagInfo.SYNC.id;
    public static final int ID_NO_INFERIORS = FlagInfo.NO_INFERIORS.id;
    @Deprecated
    public static final int ID_ARCHIVED = FlagInfo.ARCHIVED.id;
    public static final int ID_GLOBAL = FlagInfo.GLOBAL.id;
    public static final int ID_IN_DUMPSTER = FlagInfo.IN_DUMPSTER.id;
    public static final int ID_UNCACHED = FlagInfo.UNCACHED.id;

    public static final int BITMASK_FROM_ME = FlagInfo.FROM_ME.bitmask;
    public static final int BITMASK_ATTACHED = FlagInfo.ATTACHED.bitmask;
    public static final int BITMASK_REPLIED = FlagInfo.REPLIED.bitmask;
    public static final int BITMASK_FORWARDED = FlagInfo.FORWARDED.bitmask;
    public static final int BITMASK_COPIED = FlagInfo.COPIED.bitmask;
    public static final int BITMASK_FLAGGED = FlagInfo.FLAGGED.bitmask;
    public static final int BITMASK_DRAFT = FlagInfo.DRAFT.bitmask;
    public static final int BITMASK_DELETED = FlagInfo.DELETED.bitmask;
    public static final int BITMASK_NOTIFIED = FlagInfo.NOTIFIED.bitmask;
    public static final int BITMASK_UNREAD = FlagInfo.UNREAD.bitmask;
    public static final int BITMASK_HIGH_PRIORITY = FlagInfo.HIGH_PRIORITY.bitmask;
    public static final int BITMASK_LOW_PRIORITY = FlagInfo.LOW_PRIORITY.bitmask;
    public static final int BITMASK_VERSIONED = FlagInfo.VERSIONED.bitmask;
    @Deprecated
    public static final int BITMASK_INDEXING_DEFERRED = FlagInfo.INDEXING_DEFERRED.bitmask;
    public static final int BITMASK_POPPED = FlagInfo.POPPED.bitmask;
    public static final int BITMASK_NOTE = FlagInfo.NOTE.bitmask;
    public static final int BITMASK_PRIORITY = FlagInfo.PRIORITY.bitmask;
    public static final int BITMASK_POST = FlagInfo.POST.bitmask;
    public static final int BITMASK_MUTED = FlagInfo.MUTED.bitmask;
    public static final int BITMASK_SUBSCRIBED = FlagInfo.SUBSCRIBED.bitmask;
    public static final int BITMASK_EXCLUDE_FREEBUSY = FlagInfo.EXCLUDE_FREEBUSY.bitmask;
    public static final int BITMASK_CHECKED = FlagInfo.CHECKED.bitmask;
    public static final int BITMASK_NO_INHERIT = FlagInfo.NO_INHERIT.bitmask;
    public static final int BITMASK_INVITE = FlagInfo.INVITE.bitmask;
    public static final int BITMASK_SYNCFOLDER = FlagInfo.SYNCFOLDER.bitmask;
    public static final int BITMASK_SYNC = FlagInfo.SYNC.bitmask;
    public static final int BITMASK_NO_INFERIORS = FlagInfo.NO_INFERIORS.bitmask;
    @Deprecated
    public static final int BITMASK_ARCHIVED = FlagInfo.ARCHIVED.bitmask;
    public static final int BITMASK_GLOBAL = FlagInfo.GLOBAL.bitmask;
    public static final int BITMASK_IN_DUMPSTER = FlagInfo.IN_DUMPSTER.bitmask;
    public static final int BITMASK_UNCACHED = FlagInfo.UNCACHED.bitmask;

    static final String UNREAD_FLAG_ONLY = String.valueOf(FlagInfo.UNREAD.ch);

    public static final int FLAGS_SYSTEM =
        BITMASK_FROM_ME | BITMASK_ATTACHED | BITMASK_COPIED | BITMASK_DRAFT | BITMASK_HIGH_PRIORITY |
        BITMASK_LOW_PRIORITY | BITMASK_VERSIONED | BITMASK_INDEXING_DEFERRED | BITMASK_INVITE | BITMASK_ARCHIVED |
        BITMASK_GLOBAL | BITMASK_IN_DUMPSTER | BITMASK_UNCACHED | BITMASK_NOTE | BITMASK_POST;
    public static final int FLAGS_FOLDER  =
        BITMASK_SUBSCRIBED | BITMASK_EXCLUDE_FREEBUSY | BITMASK_CHECKED | BITMASK_NO_INHERIT | BITMASK_SYNCFOLDER |
        BITMASK_SYNC | BITMASK_NO_INFERIORS | BITMASK_GLOBAL;
    public static final int FLAGS_MESSAGE =
        BITMASK_FROM_ME | BITMASK_REPLIED | BITMASK_FORWARDED | BITMASK_DRAFT | BITMASK_NOTIFIED | BITMASK_UNREAD |
        BITMASK_HIGH_PRIORITY | BITMASK_LOW_PRIORITY | BITMASK_POPPED | BITMASK_INVITE | BITMASK_PRIORITY |
        BITMASK_POST | BITMASK_MUTED;
    public static final int FLAGS_CALITEM =
        BITMASK_DRAFT | BITMASK_HIGH_PRIORITY | BITMASK_LOW_PRIORITY;
    public static final int FLAGS_GENERIC =
        BITMASK_ATTACHED | BITMASK_COPIED | BITMASK_FLAGGED | BITMASK_DELETED | BITMASK_VERSIONED |
        BITMASK_INDEXING_DEFERRED | BITMASK_ARCHIVED | BITMASK_IN_DUMPSTER | BITMASK_UNCACHED | BITMASK_NOTE;

    /**
     * Bitmask of all valid flags <b>except</b> {@link #BITMASK_UNREAD}.
     */
    public static final int FLAGS_ALL = (FLAGS_FOLDER | FLAGS_MESSAGE | FLAGS_CALITEM | FLAGS_GENERIC) & ~BITMASK_UNREAD;

    private final FlagInfo info;

    Flag(Mailbox mbox, UnderlyingData ud, FlagInfo info) throws ServiceException {
        super(mbox, ud);
        if (mData.type != Type.FLAG.toByte()) {
            throw new IllegalArgumentException();
        }
        this.info = info;
    }

    public static byte getIndex(int flagId) {
        return (byte) (flagId > 0 || flagId < -MailItem.MAX_FLAG_COUNT ? -1 : -flagId - 1);
    }

    public char toChar() {
        return toChar(mId);
    }

    public static char toChar(int flagId) {
        int index = getIndex(flagId);
        if (index < 0 || index >= INDEX2FLAG.length) {
            return HIDDEN;
        }

        FlagInfo finfo = INDEX2FLAG[index];
        return finfo == null ? HIDDEN : finfo.ch;
    }

    /**
     * Returns the "external" flag bitmask for the given flag string, which includes {@link Flag#BITMASK_UNREAD}.
     */
    public static int toBitmask(String flags) {
        if (Strings.isNullOrEmpty(flags)) {
            return 0;
        }

        int bitmask = 0;
        for (int i = 0, len = flags.length(); i < len; i++) {
            char c = flags.charAt(i);
            FlagInfo flag = c > 0 && c < 127 ? CHAR2FLAG[c] : null;
            if (flag != null) {
                bitmask |= flag.bitmask;
            }
        }
        return bitmask;
    }

    public static String toString(int bitmask) {
        if (bitmask == 0) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (FlagInfo flag : FlagInfo.values()) {
            if ((bitmask & flag.bitmask) != 0) {
                if (flag.ch != HIDDEN) {
                    result.append(flag.ch);
                }
            }
        }
        return result.toString();
    }

    public static List<Integer> toId(int bitmask) {
        if (bitmask == 0) {
            return Collections.emptyList();
        }

        List<Integer> result = new ArrayList<Integer>(5);
        for (FlagInfo flag : FlagInfo.values()) {
            if ((bitmask & flag.bitmask) != 0) {
                result.add(flag.id);
            }
        }
        return result;
    }

    /**
     * Returns the {@link Flag} with the given id, or {@code null} if there is no {@link Flag} with that id.
     */
    static Flag of(Mailbox mbox, int id) throws ServiceException {
        int index = getIndex(id);
        FlagInfo flag = index < 0 || index >= INDEX2FLAG.length ? null : INDEX2FLAG[index];
        return flag == null ? null : flag.toFlag(mbox);
    }

    /**
     * Returns the {@link Flag} with the given name, or {@code null} if there is no {@link Flag} with that name.
     *
     * @param mbox mailbox
     * @param name flag name, case-insensitive
     */
    static Flag of(Mailbox mbox, String name) throws ServiceException {
        FlagInfo flag = NAME2FLAG.get(name.toLowerCase());
        return flag == null ? null : flag.toFlag(mbox);
    }

    /**
     * Returns all the possible {@link Flag} objects for the given {@link Mailbox}.
     */
    static List<Flag> allOf(Mailbox mbox) throws ServiceException {
        FlagInfo[] flags = FlagInfo.values();
        List<Flag> result = new ArrayList<Flag>(flags.length);
        for (FlagInfo flag : flags) {
            result.add(flag.toFlag(mbox));
        }
        return result;
    }

    public byte getIndex() {
        return getIndex(mId);
    }

    @Override
    boolean canTag(MailItem item) {
        if ((info.bitmask & FLAGS_FOLDER) != 0 && item instanceof Folder) {
            return true;
        }
        if (!item.isTaggable()) {
            return false;
        }
        if ((info.bitmask & FLAGS_MESSAGE) != 0 && !(item instanceof Message)) {
            return false;
        }
        if ((info.bitmask & FLAGS_CALITEM) != 0 && !(item instanceof CalendarItem)) {
            return false;
        }
        return true;
    }

    @Override
    boolean canAccess(short rightsNeeded) {
        return true;
    }

    @Override
    boolean canAccess(short rightsNeeded, Account authuser, boolean asAdmin) {
        return true;
    }

    @Override
    boolean isMutable() {
        return false;
    }

    @Override
    boolean trackUnread() {
        return false;
    }

    @Override
    void decodeMetadata(Metadata meta) {
    }

    @Override
    Metadata encodeMetadata(Metadata meta) {
        return meta;
    }

    public int toBitmask() {
        return info.bitmask;
    }

    public boolean isSystemFlag() {
        return (toBitmask() & Flag.FLAGS_SYSTEM) != 0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Flag && info == ((Flag) o).info;
    }

    @Override
    public int hashCode() {
        return info.hashCode();
    }

    @Override
    protected void checkItemCreationAllowed() throws ServiceException {
        // check nothing
        // external account mailbox can have flags on mountpoints
    }
}
