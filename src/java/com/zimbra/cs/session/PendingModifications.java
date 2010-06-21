/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
 * Created on Nov 28, 2004
 */
package com.zimbra.cs.session;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;

import com.zimbra.common.util.Pair;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;

public final class PendingModifications {
    public static final class Change {
        public static final int UNMODIFIED         = 0x00000000;
        public static final int MODIFIED_UNREAD    = 0x00000001;
        public static final int MODIFIED_TAGS      = 0x00000002;
        public static final int MODIFIED_FLAGS     = 0x00000004;
        public static final int MODIFIED_CONFIG    = 0x00000008;
        public static final int MODIFIED_SIZE      = 0x00000010;
        public static final int MODIFIED_DATE      = 0x00000020;
        public static final int MODIFIED_SUBJECT   = 0x00000040;
        public static final int MODIFIED_IMAP_UID  = 0x00000080;
        public static final int MODIFIED_FOLDER    = 0x00000100;
        public static final int MODIFIED_PARENT    = 0x00000200;
        public static final int MODIFIED_CHILDREN  = 0x00000400;
        public static final int MODIFIED_SENDERS   = 0x00000800;
        public static final int MODIFIED_NAME      = 0x00001000;
        public static final int MODIFIED_COLOR     = 0x00002000;
        public static final int MODIFIED_POSITION  = 0x00004000;
        public static final int MODIFIED_QUERY     = 0x00008000;
        public static final int MODIFIED_CONTENT   = 0x00010000;
        public static final int MODIFIED_INVITE    = 0x00020000;
        public static final int MODIFIED_URL       = 0x00040000;
        public static final int MODIFIED_METADATA  = 0x00080000;
        public static final int MODIFIED_VIEW      = 0x00100000;
        public static final int MODIFIED_ACL       = 0x00200000;
        public static final int MODIFIED_CONFLICT  = 0x00400000;
        public static final int INTERNAL_ONLY      = 0x10000000;
        public static final int ALL_FIELDS         = ~0;

        public Object what;
        public int    why;

        Change(Object thing, int reason)  { what = thing;  why = reason; }
    }

    public static final class ModificationKey extends Pair<String, Integer> {
        public ModificationKey(String accountId, Integer itemId) {
            super(accountId, itemId);
        }
        public ModificationKey(MailItem item) {
            super(item.getMailbox().getAccountId(), item.getId());
        }

        public String getAccountId()  { return getFirst(); }
        public Integer getItemId()    { return getSecond(); }
    }


    /** Set of all the MailItem types that are included in this structure
     * The mask is generated from the MailItem type using 
     * @link{MailItem#typeToBitmask} */
    public int changedTypes = 0;

    public LinkedHashMap<ModificationKey, MailItem> created;
    public HashMap<ModificationKey, Change> modified;
    public HashMap<ModificationKey, Object> deleted;

    public PendingModifications() { }

    public boolean hasNotifications() {
        return (deleted  != null && !deleted.isEmpty()) ||
               (created  != null && !created.isEmpty()) ||
               (modified != null && !modified.isEmpty());
    }

    public int getScaledNotificationCount() {
        int count = 0;
        if (deleted != null)   count += (deleted.size() + 3) / 4;
        if (created != null)   count += created.size();
        if (modified != null)  count += modified.size();
        return count;
    }

    public boolean overlapsWithAccount(String accountId) {
        accountId = accountId == null ? null : accountId.toLowerCase();
        if (deleted != null) {
            for (ModificationKey mkey : deleted.keySet())
                if (mkey.getAccountId().equals(accountId))
                    return true;
        }
        if (created != null) {
            for (ModificationKey mkey : created.keySet())
                if (mkey.getAccountId().equals(accountId))
                    return true;
        }
        if (modified != null) {
            for (ModificationKey mkey : modified.keySet())
                if (mkey.getAccountId().equals(accountId))
                    return true;
        }
        return false;
    }

    public void recordCreated(MailItem item) {
//        ZimbraLog.mailbox.debug("--> NOTIFY: created " + item.getId());
        if (created == null)
            created = new LinkedHashMap<ModificationKey, MailItem>();
        changedTypes |= MailItem.typeToBitmask(item.getType());
        created.put(new ModificationKey(item), item);
    }

    public void recordDeleted(String accountId, int id, byte type) {
        if (type != 0 && type != MailItem.TYPE_UNKNOWN)
            changedTypes |= MailItem.typeToBitmask(type);
        ModificationKey key = new ModificationKey(accountId, id);
        delete(key, key.getItemId());
    }

    public void recordDeleted(String accountId, Collection<Integer> ids, int typesMask) {
        changedTypes |= typesMask;
        for (Integer id : ids) {
            ModificationKey key = new ModificationKey(accountId, id);
            delete(key, id);
        }
    }

    public void recordDeleted(MailItem item) {
        changedTypes |= MailItem.typeToBitmask(item.getType());
        delete(new ModificationKey(item), item);
    }

    public void recordDeleted(Collection<ModificationKey> keys, int typesMask) {
        changedTypes |= typesMask;
        for (ModificationKey key : keys)
            delete(key, key.getItemId());
    }

    private void delete(ModificationKey key, Object value) {
//      ZimbraLog.mailbox.debug("--> NOTIFY: deleted " + key);
        if (created != null && created.remove(key) != null)
            return;
        if (modified != null)
            modified.remove(key);
        if (deleted == null)
            deleted = new HashMap<ModificationKey, Object>();
        deleted.put(key, value);
    }

    public void recordModified(Mailbox mbox, int reason) {
        recordModified(new ModificationKey(mbox.getAccountId(), 0), mbox, reason);
    }

    public void recordModified(MailItem item, int reason) {
        changedTypes |= MailItem.typeToBitmask(item.getType());
        recordModified(new ModificationKey(item), item, reason);
    }

    private void recordModified(ModificationKey key, Object item, int reason) {
//        ZimbraLog.mailbox.debug("--> NOTIFY: modified " + key + " (" + reason + ')');
        Change chg = null;
        if (created != null && created.containsKey(key)) {
            if (item instanceof MailItem)
                recordCreated((MailItem) item);
            return;
        } else if (deleted != null && deleted.containsKey(key)) {
            return;
        } else if (modified == null) {
            modified = new HashMap<ModificationKey, Change>();
        } else {
            chg = modified.get(key);
            if (chg != null) {
                chg.what = item;
                chg.why |= reason;
            }
        }
        if (chg == null)
            chg = new Change(item, reason);
        modified.put(key, chg);
    }

    PendingModifications add(PendingModifications other) {
        changedTypes |= other.changedTypes;

        if (other.deleted != null) {
            // note that deleted MailItems are just added as IDs for concision
            for (ModificationKey key : other.deleted.keySet())
                delete(key, key.getItemId());
        }

        if (other.created != null) {
            for (MailItem item : other.created.values())
                recordCreated(item);
        }

        if (other.modified != null) {
            for (Change chg : other.modified.values()) {
                if (chg.what instanceof MailItem)
                    recordModified((MailItem) chg.what, chg.why);
                else if (chg.what instanceof Mailbox)
                    recordModified((Mailbox) chg.what, chg.why);
            }
        }

        return this;
    }

    public void clear()  { 
        created = null;  
        deleted = null;  
        modified = null;
        changedTypes = 0;
    }
}
