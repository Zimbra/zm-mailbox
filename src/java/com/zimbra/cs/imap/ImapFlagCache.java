/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.imap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;

public class ImapFlagCache implements Iterable<ImapFlagCache.ImapFlag> {
    static final class ImapFlag {
        final String  mName;
        final String  mImapName;
        final int     mId;
        final long    mBitmask;
        final boolean mPositive;
        final boolean mPermanent;
        final boolean mListed;
        final int     mModseq;

        static final boolean VISIBLE = true, HIDDEN = false;

        ImapFlag(String name, Tag ltag, boolean positive) {
            mId   = ltag.getId();    mBitmask   = ltag.getBitmask();
            mName = ltag.getName();  mImapName  = normalize(name, mId);
            mPositive = positive;    mPermanent = true;
            mListed = VISIBLE;       mModseq    = ltag.getSavedSequence();
        }

        ImapFlag(String name, short bitmask, boolean listed) {
            mId   = 0;         mBitmask   = bitmask;
            mName = name;      mImapName  = name;
            mPositive = true;  mPermanent = false;
            mListed = listed;  mModseq    = -1;
        }

        private String normalize(String name, int id) {
            String imapName = name.replaceAll("[ *(){%*\\]\\\\]+", "");
            if (name.startsWith("\\"))
                imapName = '\\' + imapName;
            if (!name.equals(""))
                return imapName;
            return ":FLAG" + Tag.getIndex(id);
        }

        boolean matches(ImapMessage i4msg) {
            long mask = (mId == 0 ? i4msg.sflags : (mId > 0 ? i4msg.tags : i4msg.flags));
            return (mask & mBitmask) != 0;
        }

        @Override
        public String toString()  { return mImapName; }
    }


    private final Mailbox mMailbox;
    private final Map<String, ImapFlag> mNames  = new LinkedHashMap<String, ImapFlag>();
    private final Map<Long, ImapFlag> mBitmasks = new HashMap<Long, ImapFlag>();

    ImapFlagCache()  { mMailbox = null; }

    ImapFlagCache(Mailbox mbox, OperationContext octxt) throws ServiceException {
        mMailbox = mbox;
        try {
            for (Tag ltag : mbox.getTagList(octxt)) {
                if (!(ltag instanceof Flag))
                    cache(new ImapFlag(ltag.getName(), ltag, true));
            }
        } catch (ServiceException e) {
            if (!e.getCode().equals(ServiceException.PERM_DENIED))
                throw e;
        }
    }

    static ImapFlagCache getSystemFlags(Mailbox mbox) {
        ImapFlagCache i4cache = new ImapFlagCache();

        i4cache.cache(new ImapFlag("\\Answered", mbox.mReplyFlag,    true));
        i4cache.cache(new ImapFlag("\\Deleted",  mbox.mDeletedFlag,  true));
        i4cache.cache(new ImapFlag("\\Draft",    mbox.mDraftFlag,    true));
        i4cache.cache(new ImapFlag("\\Flagged",  mbox.mFlaggedFlag,  true));
        i4cache.cache(new ImapFlag("\\Seen",     mbox.mUnreadFlag,   false));
        i4cache.cache(new ImapFlag("$Forwarded", mbox.mForwardFlag,  true));
        i4cache.cache(new ImapFlag("$MDNSent",   mbox.mNotifiedFlag, true));
        i4cache.cache(new ImapFlag("Forwarded",  mbox.mForwardFlag,  true));

        i4cache.cache(new ImapFlag("\\Recent",     ImapMessage.FLAG_RECENT,       ImapFlag.HIDDEN));
        i4cache.cache(new ImapFlag("$Junk",        ImapMessage.FLAG_SPAM,         ImapFlag.VISIBLE));
        i4cache.cache(new ImapFlag("$NotJunk",     ImapMessage.FLAG_NONSPAM,      ImapFlag.VISIBLE));
        i4cache.cache(new ImapFlag("Junk",         ImapMessage.FLAG_SPAM,         ImapFlag.VISIBLE));
        i4cache.cache(new ImapFlag("JunkRecorded", ImapMessage.FLAG_JUNKRECORDED, ImapFlag.VISIBLE));
        i4cache.cache(new ImapFlag("NonJunk",      ImapMessage.FLAG_NONSPAM,      ImapFlag.VISIBLE));
        i4cache.cache(new ImapFlag("NotJunk",      ImapMessage.FLAG_NONSPAM,      ImapFlag.VISIBLE));

        return i4cache;
    }


    ImapFlag getByName(String name) {
        return mNames.get(name.toUpperCase());
    }

    ImapFlag getByMask(long mask) {
        return mBitmasks.get(mask);
    }

    List<String> listNames(boolean permanentOnly) {
        if (mNames.isEmpty())
            return Collections.emptyList();

        List<String> names = new ArrayList<String>();
        for (Map.Entry<String, ImapFlag> entry : mNames.entrySet()) {
            ImapFlag i4flag = entry.getValue();
            if (i4flag.mListed && (!permanentOnly || i4flag.mPermanent))
                names.add(i4flag.mImapName);
        }
        return names;
    }

    int getMaximumModseq() {
        int modseq = 0;
        for (ImapFlag i4flag : mNames.values())
            modseq = Math.max(modseq, i4flag.mModseq);
        return modseq;
    }


    ImapFlag createTag(OperationContext octxt, String name, List<Tag> newTags) throws ServiceException {
        if (mMailbox == null)
            return null;

        ImapFlag i4flag = getByName(name);
        if (i4flag != null)
            return i4flag;

        if (name.startsWith("\\"))
            throw MailServiceException.INVALID_NAME(name);

        try {
            Tag ltag = mMailbox.createTag(octxt, name, MailItem.DEFAULT_COLOR);
            newTags.add(ltag);
            i4flag = getByName(name);
            if (i4flag == null)
                return cache(i4flag = new ImapFlag(name, ltag, true));
        } catch (ServiceException e) {
            if (!e.getCode().equals(ServiceException.PERM_DENIED) && !e.getCode().equals(MailServiceException.TOO_MANY_TAGS))
                throw e;
        }
        return i4flag;
    }


    ImapFlag cache(ImapFlag i4flag) {
        mNames.put(i4flag.mImapName.toUpperCase(), i4flag);
        Long bitmask = new Long(i4flag.mBitmask);
        if (!mBitmasks.containsKey(bitmask))
            mBitmasks.put(bitmask, i4flag);
        return i4flag;
    }

    void uncache(long bitmask) {
        ImapFlag i4flag = mBitmasks.remove(bitmask);
        if (i4flag != null)
            mNames.remove(i4flag.mImapName.toUpperCase());
    }

    void clear() {
        mNames.clear();
        mBitmasks.clear();
    }

    public Iterator<ImapFlag> iterator() {
        return mNames.values().iterator();
    }
}