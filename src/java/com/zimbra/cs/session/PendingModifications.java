/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;

import com.zimbra.common.util.Pair;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxOperation;

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
        public static final int MODIFIED_LOCK      = 0x00800000;
        public static final int INTERNAL_ONLY      = 0x10000000;
        public static final int ALL_FIELDS         = ~0;

        public MailboxOperation op;
        
        public Object what;
        public int    why;

        Change(MailboxOperation op, Object thing, int reason) {
            this.op = op;  what = thing;  why = reason;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (what instanceof MailItem) {
                MailItem item = (MailItem) what;
                sb.append(item.getType()).append(' ').append(item.getId()).append(":");
            } else if (what instanceof Mailbox) {
                sb.append("mailbox:");
            }

            if (why == 0)                        sb.append(" **UNMODIFIED**");
            if ((why & MODIFIED_UNREAD) != 0)    sb.append(" UNREAD");
            if ((why & MODIFIED_TAGS) != 0)      sb.append(" TAGS");
            if ((why & MODIFIED_FLAGS) != 0)     sb.append(" FLAGS");
            if ((why & MODIFIED_CONFIG) != 0)    sb.append(" CONFIG");
            if ((why & MODIFIED_SIZE) != 0)      sb.append(" SIZE");
            if ((why & MODIFIED_DATE) != 0)      sb.append(" DATE");
            if ((why & MODIFIED_SUBJECT) != 0)   sb.append(" SUBJECT");
            if ((why & MODIFIED_IMAP_UID) != 0)  sb.append(" IMAP_UID");
            if ((why & MODIFIED_FOLDER) != 0)    sb.append(" FOLDER");
            if ((why & MODIFIED_PARENT) != 0)    sb.append(" PARENT");
            if ((why & MODIFIED_CHILDREN) != 0)  sb.append(" CHILDREN");
            if ((why & MODIFIED_SENDERS) != 0)   sb.append(" SENDERS");
            if ((why & MODIFIED_NAME) != 0)      sb.append(" NAME");
            if ((why & MODIFIED_COLOR) != 0)     sb.append(" COLOR");
            if ((why & MODIFIED_POSITION) != 0)  sb.append(" POSITION");
            if ((why & MODIFIED_QUERY) != 0)     sb.append(" QUERY");
            if ((why & MODIFIED_CONTENT) != 0)   sb.append(" CONTENT");
            if ((why & MODIFIED_INVITE) != 0)    sb.append(" INVITE");
            if ((why & MODIFIED_URL) != 0)       sb.append(" URL");
            if ((why & MODIFIED_METADATA) != 0)  sb.append(" METADATA");
            if ((why & MODIFIED_VIEW) != 0)      sb.append(" VIEW");
            if ((why & MODIFIED_ACL) != 0)       sb.append(" ACL");
            if ((why & MODIFIED_CONFLICT) != 0)  sb.append(" CONFLICT");
            if ((why & MODIFIED_LOCK) != 0)      sb.append(" LOCK");
            if ((why & INTERNAL_ONLY) != 0)      sb.append(" **INTERNAL**");

            return sb.toString();
        }
    }

    public static final class ModificationKey extends Pair<String, Integer> {
        public ModificationKey(String accountId, Integer itemId) {
            super(accountId, itemId);
        }

        public ModificationKey(MailItem item) {
            super(item.getMailbox().getAccountId(), item.getId());
        }

        public ModificationKey(ModificationKey mkey) {
            super(mkey.getAccountId(), mkey.getItemId());
        }

        public String getAccountId() {
            return getFirst();
        }

        public Integer getItemId() {
            return getSecond();
        }
    }


    /**
     * Set of all the MailItem types that are included in this structure
     */
    public Set<MailItem.Type> changedTypes = EnumSet.noneOf(MailItem.Type.class);

    public LinkedHashMap<ModificationKey, MailItem> created;
    public HashMap<ModificationKey, Change> modified;
    public HashMap<ModificationKey, Object> deleted;
    public HashMap<Integer, MailItem> preModifyItems;

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
            for (ModificationKey mkey : deleted.keySet()) {
                if (mkey.getAccountId().equals(accountId))
                    return true;
            }
        }
        if (created != null) {
            for (ModificationKey mkey : created.keySet()) {
                if (mkey.getAccountId().equals(accountId))
                    return true;
            }
        }
        if (modified != null) {
            for (ModificationKey mkey : modified.keySet()) {
                if (mkey.getAccountId().equals(accountId))
                    return true;
            }
        }
        return false;
    }

    public void recordCreated(MailItem item) {
        if (created == null) {
            created = new LinkedHashMap<ModificationKey, MailItem>();
        }
        changedTypes.add(item.getType());
        created.put(new ModificationKey(item), item);
    }

    public void recordDeleted(String accountId, int id, MailItem.Type type) {
        if (type != MailItem.Type.UNKNOWN) {
            changedTypes.add(type);
        }
        ModificationKey key = new ModificationKey(accountId, id);
        delete(key, key.getItemId());
    }

    public void recordDeleted(String accountId, Collection<Integer> ids, Set<MailItem.Type> types) {
        changedTypes.addAll(types);
        for (Integer id : ids) {
            ModificationKey key = new ModificationKey(accountId, id);
            delete(key, id);
        }
    }

    public void recordDeleted(MailItem item) {
        changedTypes.add(item.getType());
        delete(new ModificationKey(item), item);
    }

    public void recordDeleted(Collection<ModificationKey> keys, Set<MailItem.Type> types) {
        changedTypes.addAll(types);
        for (ModificationKey key : keys) {
            delete(key, key.getItemId());
        }
    }

    private void delete(ModificationKey key, Object value) {
        if (created != null && created.remove(key) != null)
            return;
        if (modified != null) {
            modified.remove(key);
        }
        if (deleted == null) {
            deleted = new HashMap<ModificationKey, Object>();
        }
        deleted.put(key, value);
    }

    public void recordModified(ModificationKey mkey, Change chg) {
        recordModified(mkey, chg.op, chg.what, chg.why);
    }

    public void recordModified(MailboxOperation op, Mailbox mbox, int reason) {
        recordModified(new ModificationKey(mbox.getAccountId(), 0), op, mbox, reason);
    }

    public void recordModified(MailboxOperation op, MailItem item, int reason) {
        changedTypes.add(item.getType());
        recordModified(new ModificationKey(item), op, item, reason);
    }

    private void recordModified(ModificationKey key, MailboxOperation op, Object item, int reason) {
        Change chg = null;
        if (created != null && created.containsKey(key)) {
            if (item instanceof MailItem) {
                recordCreated((MailItem) item);
            }
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
        if (chg == null) {
            chg = new Change(op, item, reason);
        }
        modified.put(key, chg);
    }

    PendingModifications add(PendingModifications other) {
        changedTypes.addAll(other.changedTypes);

        if (other.deleted != null) {
            // note that deleted MailItems are just added as IDs for concision
            for (ModificationKey key : other.deleted.keySet()) {
                delete(key, key.getItemId());
            }
        }

        if (other.created != null) {
            for (MailItem item : other.created.values())
                recordCreated(item);
        }

        if (other.modified != null) {
            for (Change chg : other.modified.values()) {
                if (chg.what instanceof MailItem) {
                    recordModified(chg.op, (MailItem) chg.what, chg.why);
                } else if (chg.what instanceof Mailbox) {
                    recordModified(chg.op, (Mailbox) chg.what, chg.why);
                }
            }
        }

        return this;
    }

    public void clear()  {
        created = null;
        deleted = null;
        modified = null;
        changedTypes.clear();
    }
}
