/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
 * Created on Oct 15, 2004
 */
package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;

public class Flag extends Tag {

    public static final byte FLAG_GENERIC      = 0x00;
    public static final byte FLAG_MESSAGE_ONLY = 0x01;
    public static final byte FLAG_FOLDER_ONLY  = 0x02;

    public static final int ID_FLAG_FROM_ME       = -1;
    public static final int ID_FLAG_ATTACHED      = -2;
    public static final int ID_FLAG_REPLIED       = -3;
    public static final int ID_FLAG_FORWARDED     = -4;
    public static final int ID_FLAG_COPIED        = -5;
    public static final int ID_FLAG_FLAGGED       = -6;
    public static final int ID_FLAG_DRAFT         = -7;
    public static final int ID_FLAG_DELETED       = -8;
    public static final int ID_FLAG_NOTIFIED      = -9;
    /** Callers of {@link Mailbox} methods treat <tt>FLAG_UNREAD</tt> like the
     *  other flags.  Internally, we break it out into a separate variable,
     *  <code>unreadCount</code>.  It's also persisted in a separate indexed
     *  column for fast lookups of unread <tt>MailItem</tt>s. */
    public static final int ID_FLAG_UNREAD        = -10;
    public static final int ID_FLAG_HIGH_PRIORITY = -11;
    public static final int ID_FLAG_LOW_PRIORITY  = -12;
    public static final int ID_FLAG_VERSIONED     = -13;
    public static final int ID_FLAG_INDEXING_DEFERRED = -14;
    public static final int ID_FLAG_SUBSCRIBED    = -20;
    public static final int ID_FLAG_EXCLUDE_FREEBUSY = -21;
    public static final int ID_FLAG_CHECKED       = -22;
    public static final int ID_FLAG_NO_INHERIT    = -23;
    public static final int ID_FLAG_INVITE        = -24;
    public static final int ID_FLAG_SYNCFOLDER    = -25;
    public static final int ID_FLAG_SYNC          = -26;
    public static final int ID_FLAG_NO_INFERIORS  = -27;
    public static final int ID_FLAG_GLOBAL        = -29;
    public static final int ID_FLAG_IN_DUMPSTER   = -30;
    public static final int ID_FLAG_UNCACHED      = -31;

    private static final class FlagInfo {
        private static final FlagInfo sFlagInfo[] = new FlagInfo[31];
        private static final FlagInfo sFlagAbbreviations[] = new FlagInfo[127];
        private static final Map<String, FlagInfo> sFlagNames = new HashMap<String, FlagInfo>();

        private static final char HIDDEN = '\0';

        static {
            new FlagInfo("\\Sent",        's',    FLAG_MESSAGE_ONLY, true,  ID_FLAG_FROM_ME);
            new FlagInfo("\\Attached",    'a',    FLAG_GENERIC,      true,  ID_FLAG_ATTACHED);
            new FlagInfo("\\Answered",    'r',    FLAG_MESSAGE_ONLY, false, ID_FLAG_REPLIED);
            new FlagInfo("\\Forwarded",   'w',    FLAG_MESSAGE_ONLY, false, ID_FLAG_FORWARDED);
            new FlagInfo("\\Copied",      HIDDEN, FLAG_GENERIC,      true,  ID_FLAG_COPIED);
            new FlagInfo("\\Flagged",     'f',    FLAG_GENERIC,      false, ID_FLAG_FLAGGED);
            new FlagInfo("\\Draft",       'd',    FLAG_MESSAGE_ONLY, true,  ID_FLAG_DRAFT);
            new FlagInfo("\\Deleted",     'x',    FLAG_GENERIC,      false, ID_FLAG_DELETED);
            new FlagInfo("\\Notified",    'n',    FLAG_MESSAGE_ONLY, false, ID_FLAG_NOTIFIED);
            new FlagInfo("\\Unread",      'u',    FLAG_MESSAGE_ONLY, false, ID_FLAG_UNREAD);
            new FlagInfo("\\Urgent",      '!',    FLAG_MESSAGE_ONLY, true,  ID_FLAG_HIGH_PRIORITY);
            new FlagInfo("\\Bulk",        '?',    FLAG_MESSAGE_ONLY, true,  ID_FLAG_LOW_PRIORITY);
            new FlagInfo("\\Versioned",   '/',    FLAG_GENERIC,      true,  ID_FLAG_VERSIONED);
            new FlagInfo("\\IdxDeferred", HIDDEN, FLAG_GENERIC,      true,  ID_FLAG_INDEXING_DEFERRED);
            new FlagInfo("\\Subscribed",  '*',    FLAG_FOLDER_ONLY,  false, ID_FLAG_SUBSCRIBED);
            new FlagInfo("\\ExcludeFB",   'b',    FLAG_FOLDER_ONLY,  false, ID_FLAG_EXCLUDE_FREEBUSY);
            new FlagInfo("\\Checked",     '#',    FLAG_FOLDER_ONLY,  false, ID_FLAG_CHECKED);
            new FlagInfo("\\NoInherit",   'i',    FLAG_FOLDER_ONLY,  false, ID_FLAG_NO_INHERIT);
            new FlagInfo("\\Invite",      'v',    FLAG_MESSAGE_ONLY, true,  ID_FLAG_INVITE);
            new FlagInfo("\\SyncFolder",  'y',    FLAG_FOLDER_ONLY,  false, ID_FLAG_SYNCFOLDER);
            new FlagInfo("\\Sync",        '~',    FLAG_FOLDER_ONLY,  false, ID_FLAG_SYNC);
            new FlagInfo("\\Noinferiors", 'o',    FLAG_FOLDER_ONLY,  false, ID_FLAG_NO_INFERIORS);
            new FlagInfo("\\Global",      'g',    FLAG_FOLDER_ONLY,  true,  ID_FLAG_GLOBAL);
            new FlagInfo("\\InDumpster",  HIDDEN, FLAG_GENERIC,      true,  ID_FLAG_IN_DUMPSTER);
            new FlagInfo("\\Uncached",    HIDDEN, FLAG_GENERIC,      true,  ID_FLAG_UNCACHED);
        }

        private final String  mName;
        private final int     mId;
        private final char    mAbbreviation;
        private final boolean mIsSystem;
        private final byte    mAttributes;
        private final int     mBitmask;

        FlagInfo(String name, char abbr, byte attrs, boolean system, int id) {
            mName = name;
            mId = id;
            mAbbreviation = abbr;
            mIsSystem = system;
            mAttributes = attrs;

            int index = Flag.getIndex(id);
            mBitmask = 1 << index;
            sFlagInfo[index] = this;
            sFlagNames.put(name.toLowerCase(), this);
            if (abbr != HIDDEN)
                sFlagAbbreviations[abbr] = this;
        }

        static char getAbbreviation(int flagId) {
            int index = Flag.getIndex(flagId);
            if (index < 0 || index >= sFlagInfo.length)
                return HIDDEN;
            FlagInfo finfo = sFlagInfo[index];
            return finfo == null ? HIDDEN : finfo.mAbbreviation;
        }

        static int getBitmask(int flagId) {
            int index = Flag.getIndex(flagId);
            if (index < 0 || index >= sFlagInfo.length)
                return 0;
            FlagInfo finfo = sFlagInfo[index];
            return finfo == null ? 0 : finfo.mBitmask;
        }

        static int flagsToBitmask(String flags) {
            if (flags == null || flags.length() == 0)
                return 0;

            int bitmask = 0;
            for (int i = 0, len = flags.length(); i < len; i++) {
                char c = flags.charAt(i);
                FlagInfo finfo = c > 0 && c < 127 ? sFlagAbbreviations[c] : null;
                if (finfo != null)
                    bitmask |= finfo.mBitmask;
            }
            return bitmask;
        }

        static String bitmaskToFlags(int bitmask) {
            if (bitmask == 0)
                return "";

            StringBuilder sb = new StringBuilder();
            for (FlagInfo finfo : sFlagInfo) {
                if (finfo != null && (bitmask & finfo.mBitmask) != 0) {
                    if (finfo.mAbbreviation != HIDDEN)
                        sb.append(finfo.mAbbreviation);
                    bitmask &= ~finfo.mBitmask;
                }
            }
            return sb.toString();
        }

        static List<Integer> bitmaskToFlagIds(int bitmask) {
            if (bitmask == 0)
                return Collections.emptyList();

            List<Integer> flagIds = new ArrayList<Integer>(5);
            for (FlagInfo finfo : sFlagInfo) {
                if (finfo != null && (bitmask & finfo.mBitmask) != 0) {
                    flagIds.add(finfo.mId);
                    bitmask &= ~finfo.mBitmask;
                }
            }
            return flagIds;
        }


        Flag instantiate(Mailbox mbox) throws ServiceException {
            UnderlyingData data = new UnderlyingData();
            data.id       = mId;
            data.type     = MailItem.TYPE_FLAG;
            data.folderId = Mailbox.ID_FOLDER_TAGS;
            data.flags    = BITMASK_UNCACHED;
            data.name     = mName;

            Flag flag = new Flag(mbox, data);
            flag.mAttributes = mAttributes;
            return flag;
        }

        static Flag instantiate(Mailbox mbox, int flagId) throws ServiceException {
            int index = Flag.getIndex(flagId);
            FlagInfo finfo = index < 0 || index >= sFlagInfo.length ? null : sFlagInfo[index];
            return finfo == null ? null : finfo.instantiate(mbox);
        }

        static Flag instantiate(Mailbox mbox, String name) throws ServiceException {
            FlagInfo finfo = sFlagNames.get(name.toLowerCase());
            return finfo == null ? null : finfo.instantiate(mbox);
        }

        static List<Flag> instantiateAll(Mailbox mbox) throws ServiceException {
            List<Flag> flags = new ArrayList<Flag>(sFlagInfo.length);
            for (FlagInfo finfo : sFlagInfo) {
                if (finfo != null)
                    flags.add(finfo.instantiate(mbox));
            }
            return flags;
        }


        private static final byte ATTRIBUTE_CONSTRAINT_MASK = FLAG_MESSAGE_ONLY | FLAG_FOLDER_ONLY | FLAG_GENERIC;

        static int aggregateBitmasks(byte subsetMask) {
            int mask = 0;
            for (FlagInfo finfo : sFlagInfo) {
                if (finfo != null && (finfo.mAttributes & ATTRIBUTE_CONSTRAINT_MASK) == subsetMask)
                    mask |= finfo.mBitmask;
            }
            return mask;
        }

        static int getSystemFlagBitmask() {
            int mask = 0;
            for (FlagInfo finfo : sFlagInfo) {
                if (finfo != null && finfo.mIsSystem)
                    mask |= finfo.mBitmask;
            }
            return mask;
        }
    }

    public static final int BITMASK_FROM_ME       = FlagInfo.getBitmask(ID_FLAG_FROM_ME);       // 1
    public static final int BITMASK_ATTACHED      = FlagInfo.getBitmask(ID_FLAG_ATTACHED);      // 2
    public static final int BITMASK_REPLIED       = FlagInfo.getBitmask(ID_FLAG_REPLIED);       // 4
    public static final int BITMASK_FORWARDED     = FlagInfo.getBitmask(ID_FLAG_FORWARDED);     // 8
    public static final int BITMASK_COPIED        = FlagInfo.getBitmask(ID_FLAG_COPIED);        // 16
    public static final int BITMASK_FLAGGED       = FlagInfo.getBitmask(ID_FLAG_FLAGGED);       // 32
    public static final int BITMASK_DRAFT         = FlagInfo.getBitmask(ID_FLAG_DRAFT);         // 64
    public static final int BITMASK_DELETED       = FlagInfo.getBitmask(ID_FLAG_DELETED);       // 128
    public static final int BITMASK_NOTIFIED      = FlagInfo.getBitmask(ID_FLAG_NOTIFIED);      // 256
    public static final int BITMASK_UNREAD        = FlagInfo.getBitmask(ID_FLAG_UNREAD);        // 512
    public static final int BITMASK_HIGH_PRIORITY = FlagInfo.getBitmask(ID_FLAG_HIGH_PRIORITY); // 1024
    public static final int BITMASK_LOW_PRIORITY  = FlagInfo.getBitmask(ID_FLAG_LOW_PRIORITY);  // 2048
    public static final int BITMASK_VERSIONED     = FlagInfo.getBitmask(ID_FLAG_VERSIONED);     // 4096
    public static final int BITMASK_INDEXING_DEFERRED = FlagInfo.getBitmask(ID_FLAG_INDEXING_DEFERRED); // 8192
    public static final int BITMASK_SUBSCRIBED    = FlagInfo.getBitmask(ID_FLAG_SUBSCRIBED);    // 524288
    public static final int BITMASK_EXCLUDE_FREEBUSY = FlagInfo.getBitmask(ID_FLAG_EXCLUDE_FREEBUSY); // 1048576
    public static final int BITMASK_CHECKED       = FlagInfo.getBitmask(ID_FLAG_CHECKED);       // 2097152
    public static final int BITMASK_NO_INHERIT    = FlagInfo.getBitmask(ID_FLAG_NO_INHERIT);    // 4194304
    public static final int BITMASK_INVITE        = FlagInfo.getBitmask(ID_FLAG_INVITE);        // 8388608
    public static final int BITMASK_SYNCFOLDER    = FlagInfo.getBitmask(ID_FLAG_SYNCFOLDER);    // 16777216
    public static final int BITMASK_SYNC          = FlagInfo.getBitmask(ID_FLAG_SYNC);          // 33554432
    public static final int BITMASK_NO_INFERIORS  = FlagInfo.getBitmask(ID_FLAG_NO_INFERIORS);  // 67108864
    public static final int BITMASK_GLOBAL        = FlagInfo.getBitmask(ID_FLAG_GLOBAL);        // 268435456
    public static final int BITMASK_IN_DUMPSTER   = FlagInfo.getBitmask(ID_FLAG_IN_DUMPSTER);   // 536870912
    public static final int BITMASK_UNCACHED      = FlagInfo.getBitmask(ID_FLAG_UNCACHED);


    static final String UNREAD_FLAG_ONLY = FlagInfo.getAbbreviation(ID_FLAG_UNREAD) + "";

    public static final int FLAG_SYSTEM   = FlagInfo.getSystemFlagBitmask();

    public static final int FLAGS_FOLDER  = FlagInfo.aggregateBitmasks(FLAG_FOLDER_ONLY);
    public static final int FLAGS_MESSAGE = FlagInfo.aggregateBitmasks(FLAG_MESSAGE_ONLY);
    public static final int FLAGS_GENERIC = FlagInfo.aggregateBitmasks(FLAG_GENERIC);

    /** Bitmask of all valid flags <b>except</b> {@link #BITMASK_UNREAD}. */
    public static final int FLAGS_ALL = (FLAGS_FOLDER | FLAGS_MESSAGE | FLAGS_GENERIC) & ~BITMASK_UNREAD;


    byte mAttributes;

    Flag(Mailbox mbox, UnderlyingData ud) throws ServiceException {
        super(mbox, ud);
        if (mData.type != TYPE_FLAG)
            throw new IllegalArgumentException();
    }

    @Override public byte getIndex() {
        return getIndex(mId);
    }

    public static byte getIndex(int flagId) {
        return (byte) (flagId > 0 || flagId < -MailItem.MAX_FLAG_COUNT ? -1 : -flagId - 1);
    }

    public char getAbbreviation() {
        return getAbbreviation(mId);
    }

    public static char getAbbreviation(int flagId) {
        return FlagInfo.getAbbreviation(flagId);
    }

    boolean isFolderOnly() {
        return (mAttributes & FLAG_FOLDER_ONLY) != 0;
    }

    @Override boolean canTag(MailItem item) {
        if ((mAttributes & FLAG_FOLDER_ONLY) != 0 && item instanceof Folder)
            return true;
        if (!item.isTaggable())
            return false;
        if ((mAttributes & FLAG_MESSAGE_ONLY) != 0 && !(item instanceof Message))
            return false;
        return true;
    }

    /** Throws an exception if the given <tt>flags</tt> bitmask contains bits
     *  for which a corresponding <code>Flag</code> object does not exist. */
    static void validateFlags(int flags) throws ServiceException {
        if ((flags & ~FLAGS_ALL) > 0)
            throw ServiceException.FAILURE("invalid value for flags: " + flags, null);
    }

    /** @return the "external" flag bitmask for the given flag string,
     *          which includes {@link Flag#BITMASK_UNREAD}. */
    public static int flagsToBitmask(String flags) {
        return FlagInfo.flagsToBitmask(flags);
    }

    public static String bitmaskToFlags(int bitmask) {
        return FlagInfo.bitmaskToFlags(bitmask);
    }
    
    public static List<Integer> bitmaskToFlagIds(int bitmask) {
        return FlagInfo.bitmaskToFlagIds(bitmask);
    }


    @Override boolean canAccess(short rightsNeeded) {
        return true;
    }

    @Override boolean canAccess(short rightsNeeded, Account authuser, boolean asAdmin) {
        return true;
    }


    @Override boolean isMutable()    { return false; }
    @Override boolean trackUnread()  { return false; }


    /** Returns the <code>Flag</code> with the given id, or <tt>null</tt> if
     *  there is no Flag with that id. */
    static Flag getFlag(Mailbox mbox, int flagId) throws ServiceException {
        return FlagInfo.instantiate(mbox, flagId);
    }

    /** Returns the <code>Flag</code> whose bitmask's single bit is in the
     *  given position, or <tt>null</tt> if there is no Flag with such a
     *  bitmask. */
    static Flag getFlag(Mailbox mbox, byte maskIndex) throws ServiceException {
        return FlagInfo.instantiate(mbox, -maskIndex - 1);
    }

    /** Returns the <code>Flag</code> with the given name, or <tt>null</tt> if
     *  there is no Flag with that name.  (The check is case-insenitive.) */
    static Flag getFlag(Mailbox mbox, String name) throws ServiceException {
        return FlagInfo.instantiate(mbox, name);
    }

    /** Returns all the possible <code>Flag</code> objects for the given
     *  <code>Mailbox</code>. */
    static List<Flag> getAllFlags(Mailbox mbox) throws ServiceException {
        return FlagInfo.instantiateAll(mbox);
    }

    @Override void decodeMetadata(Metadata meta)     { }
    @Override Metadata encodeMetadata(Metadata meta) { return meta; }
    
    
    public static void main(String[] args) {
        List<Integer> flagIds = bitmaskToFlagIds(FLAGS_MESSAGE);
        int bitmask = 0;
        for (int flagId : flagIds)
            bitmask |= 1 << getIndex(flagId);
        assert bitmask == FLAGS_MESSAGE;
    }
}
