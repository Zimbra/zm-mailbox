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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.imap;

import java.util.ArrayList;
import java.util.HashMap;
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

public class ImapFlagCache {
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
            mId   = ltag.getId();    mBitmask   = ltag.getBitmask();
            mName = ltag.getName();  mImapName  = normalize(name, mId);
            mPositive = positive;    mPermanent = true;
            mListed = VISIBLE;
        }

        ImapFlag(String name, short bitmask, boolean listed) {
            mId   = 0;         mBitmask   = bitmask;
            mName = name;      mImapName  = name;
            mPositive = true;  mPermanent = false;
            mListed = listed;
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
        ImapFlag i4flag = mNames.get(name.toUpperCase());
        return (i4flag == null || i4flag.mListed == ImapFlag.HIDDEN ? null : i4flag);
    }

    ImapFlag getByMask(long mask) {
        return mBitmasks.get(mask);
    }

    List<String> listNames(boolean permanentOnly) {
        List<String> names = new ArrayList<String>();
        for (Map.Entry<String, ImapFlag> entry : mNames.entrySet()) {
            ImapFlag i4flag = entry.getValue();
            if (i4flag.mListed && (!permanentOnly || i4flag.mPermanent))
                names.add(i4flag.mImapName);
        }
        return names;
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
        return null;
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
}