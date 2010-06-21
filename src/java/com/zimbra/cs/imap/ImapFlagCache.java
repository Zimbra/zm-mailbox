/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.Tag;

public class ImapFlagCache implements Iterable<ImapFlagCache.ImapFlag>, java.io.Serializable {
    private static final long serialVersionUID = -8938341239505513246L;

    static final class ImapFlag implements java.io.Serializable {
        private static final long serialVersionUID = 5445749167572465447L;

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
            StringBuilder sb = new StringBuilder(name.length());
            for (int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);
                // strip all non-{@link ImapRequest#ATOM_CHARS} except for a leading '\'
                if (c > 0x20 && c < 0x7f && c != '(' && c != ')' && c != '{' && c != '%' && c != '*' && c != '"' && c != ']' && (i == 0 || c != '\\'))
                    sb.append(c);
            }
            // if we stripped chars, make sure to disambiguate the resulting keyword names
            if (sb.length() != name.length())
                sb.append(":FLAG").append(Tag.getIndex(id));
            return sb.toString();
        }

        boolean matches(ImapMessage i4msg) {
            long mask = (mId == 0 ? i4msg.sflags : (mId > 0 ? i4msg.tags : i4msg.flags));
            return (mask & mBitmask) != 0;
        }

        @Override public String toString()  { return mImapName; }
    }


    private final Map<String, ImapFlag> mNames;
    private transient Map<Long, ImapFlag> mBitmasks;

    ImapFlagCache() {
        mNames = new LinkedHashMap<String, ImapFlag>();
        mBitmasks = new HashMap<Long, ImapFlag>();
    }

    ImapFlagCache(Mailbox mbox, OperationContext octxt) throws ServiceException {
        this();
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

    static ImapFlagCache getSystemFlags(Mailbox mbox) throws ServiceException {
        ImapFlagCache i4cache = new ImapFlagCache();

        i4cache.cache(new ImapFlag("\\Answered", mbox.getFlagById(Flag.ID_FLAG_REPLIED),   true));
        i4cache.cache(new ImapFlag("\\Deleted",  mbox.getFlagById(Flag.ID_FLAG_DELETED),   true));
        i4cache.cache(new ImapFlag("\\Draft",    mbox.getFlagById(Flag.ID_FLAG_DRAFT),     true));
        i4cache.cache(new ImapFlag("\\Flagged",  mbox.getFlagById(Flag.ID_FLAG_FLAGGED),   true));
        i4cache.cache(new ImapFlag("\\Seen",     mbox.getFlagById(Flag.ID_FLAG_UNREAD),    false));
        i4cache.cache(new ImapFlag("$Forwarded", mbox.getFlagById(Flag.ID_FLAG_FORWARDED), true));
        i4cache.cache(new ImapFlag("$MDNSent",   mbox.getFlagById(Flag.ID_FLAG_NOTIFIED),  true));
        i4cache.cache(new ImapFlag("Forwarded",  mbox.getFlagById(Flag.ID_FLAG_FORWARDED), true));

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


    ImapFlag createTag(Mailbox mbox, OperationContext octxt, String name, List<Tag> newTags) throws ServiceException {
        if (mbox == null)
            return null;

        ImapFlag i4flag = getByName(name);
        if (i4flag != null)
            return i4flag;

        if (name.startsWith("\\"))
            throw MailServiceException.INVALID_NAME(name);

        try {
            Tag ltag = mbox.createTag(octxt, name, MailItem.DEFAULT_COLOR);
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

    private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
        // read in standard stuff
        s.defaultReadObject();
        // construct bitmask mapping
        mBitmasks = new HashMap<Long, ImapFlag>();
        for (ImapFlag i4flag : mNames.values())
            cache(i4flag);
    }
}