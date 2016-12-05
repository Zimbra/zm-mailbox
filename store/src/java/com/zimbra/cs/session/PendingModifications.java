/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

/*
 * Created on Nov 28, 2004
 */
package com.zimbra.cs.session;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.mailbox.MailboxStore;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.util.TypedIdList;


/**
 * @param <T> - MailItem (local mailbox) | ZBaseItem (remote mailbox)
*/
public abstract class PendingModifications<T> {
    public static abstract class Change {
        public static final int NONE             = 0x00000000;
        public static final int UNREAD           = 0x00000001;
        public static final int TAGS             = 0x00000002;
        public static final int FLAGS            = 0x00000004;
        public static final int CONFIG           = 0x00000008;
        public static final int SIZE             = 0x00000010;
        public static final int DATE             = 0x00000020;
        public static final int SUBJECT          = 0x00000040;
        public static final int IMAP_UID         = 0x00000080;
        public static final int FOLDER           = 0x00000100;
        public static final int PARENT           = 0x00000200;
        public static final int CHILDREN         = 0x00000400;
        public static final int SENDERS          = 0x00000800;
        public static final int NAME             = 0x00001000;
        public static final int COLOR            = 0x00002000;
        public static final int POSITION         = 0x00004000;
        public static final int QUERY            = 0x00008000;
        public static final int CONTENT          = 0x00010000;
        public static final int INVITE           = 0x00020000;
        public static final int URL              = 0x00040000;
        public static final int METADATA         = 0x00080000;
        public static final int VIEW             = 0x00100000;
        public static final int ACL              = 0x00200000;
        public static final int CONFLICT         = 0x00400000;
        public static final int LOCK             = 0x00800000;
        public static final int SHAREDREM        = 0x01000000;
        public static final int RETENTION_POLICY = 0x02000000;
        public static final int DISABLE_ACTIVESYNC = 0x04000000;
        public static final int INTERNAL_ONLY    = 0x10000000;
        public static final int ALL_FIELDS       = ~0;

        public Object what;
        public int    why;
        public Object preModifyObj;

        Change(Object thing, int reason, Object preModifyObj) {
            what = thing; // MailItem.Type for deletions
            why = reason; // not applicable for deletions
            this.preModifyObj = preModifyObj;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            toStringInit(sb);

            if (why == 0) sb.append(" **NONE**");
            if ((why & UNREAD) != 0)    sb.append(" UNREAD");
            if ((why & TAGS) != 0)      sb.append(" TAGS");
            if ((why & FLAGS) != 0)     sb.append(" FLAGS");
            if ((why & CONFIG) != 0)    sb.append(" CONFIG");
            if ((why & SIZE) != 0)      sb.append(" SIZE");
            if ((why & DATE) != 0)      sb.append(" DATE");
            if ((why & SUBJECT) != 0)   sb.append(" SUBJECT");
            if ((why & IMAP_UID) != 0)  sb.append(" IMAP_UID");
            if ((why & FOLDER) != 0)    sb.append(" FOLDER");
            if ((why & PARENT) != 0)    sb.append(" PARENT");
            if ((why & CHILDREN) != 0)  sb.append(" CHILDREN");
            if ((why & SENDERS) != 0)   sb.append(" SENDERS");
            if ((why & NAME) != 0)      sb.append(" NAME");
            if ((why & COLOR) != 0)     sb.append(" COLOR");
            if ((why & POSITION) != 0)  sb.append(" POSITION");
            if ((why & QUERY) != 0)     sb.append(" QUERY");
            if ((why & CONTENT) != 0)   sb.append(" CONTENT");
            if ((why & INVITE) != 0)    sb.append(" INVITE");
            if ((why & URL) != 0)       sb.append(" URL");
            if ((why & METADATA) != 0)  sb.append(" METADATA");
            if ((why & VIEW) != 0)      sb.append(" VIEW");
            if ((why & ACL) != 0)       sb.append(" ACL");
            if ((why & CONFLICT) != 0)  sb.append(" CONFLICT");
            if ((why & LOCK) != 0)      sb.append(" LOCK");
            if ((why & SHAREDREM) != 0) sb.append(" SHAREDREM");
            if ((why & RETENTION_POLICY) != 0) sb.append(" RETENTION_POLICY");
            if ((why & DISABLE_ACTIVESYNC) != 0) sb.append(" DISABLE_ACTIVESYNC");
            if ((why & INTERNAL_ONLY) != 0)    sb.append(" **INTERNAL**");

            return sb.toString();
        }

        protected abstract void toStringInit(StringBuilder sb);
    }

    public static class ModificationKey extends Pair<String, Integer> {
        public ModificationKey(String accountId, Integer itemId) {
            super(accountId, itemId);
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

        public void setAccountId(String accountId) {
            setFirst(accountId);
        }

        public void setItemId(Integer itemId) {
            setSecond(itemId);
        }
    }


    /**
     * Set of all the MailItem types that are included in this structure
     */
    public Set<MailItem.Type> changedTypes = EnumSet.noneOf(MailItem.Type.class);

    public LinkedHashMap<ModificationKey, T> created;
    public Map<ModificationKey, Change> modified;
    public Map<ModificationKey, Change> deleted;

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

    public boolean overlapsWithAccount(String acctId) {
        acctId = acctId == null ? null : acctId.toLowerCase();
        if (deleted != null) {
            for (ModificationKey mkey : deleted.keySet()) {
                if (mkey.getAccountId().equals(acctId))
                    return true;
            }
        }
        if (created != null) {
            for (ModificationKey mkey : created.keySet()) {
                if (mkey.getAccountId().equals(acctId))
                    return true;
            }
        }
        if (modified != null) {
            for (ModificationKey mkey : modified.keySet()) {
                if (mkey.getAccountId().equals(acctId)) {
                    return true;
                }
            }
        }
        return false;
    }

    public abstract void recordCreated(T item);

    public void recordDeleted(String acctId, int id, MailItem.Type type) {
        if (type != MailItem.Type.UNKNOWN) {
            changedTypes.add(type);
        }
        ModificationKey key = new ModificationKey(acctId, id);
        delete(key, type, null);
    }

    public void recordDeleted(String acctId, TypedIdList idlist) {
        changedTypes.addAll(idlist.types());
        for (Map.Entry<MailItem.Type, List<TypedIdList.ItemInfo>> entry : idlist) {
            MailItem.Type type = entry.getKey();
            for (TypedIdList.ItemInfo iinfo : entry.getValue()) {
                delete(new ModificationKey(acctId, iinfo.getId()), type, null);
            }
        }
    }

    public abstract void recordDeleted(T itemSnapshot);

    public void recordDeleted(Map<ModificationKey, Change> deletes) {
        if (deletes != null && !deletes.isEmpty()) {
            for (Map.Entry<ModificationKey, Change> entry : deletes.entrySet()) {
                changedTypes.add((MailItem.Type) entry.getValue().what);
                delete(entry.getKey(), entry.getValue());
            }
        }
    }

    protected abstract void delete(ModificationKey key, MailItem.Type type, T itemSnapshot);

    protected void delete(ModificationKey key, Change chg) {
        if (created != null && created.remove(key) != null)
            return;

        if (modified != null) {
            modified.remove(key);
        }
        if (deleted == null) {
            deleted = new HashMap<ModificationKey, Change>();
        }
        Change existingChg = deleted.get(key);
        if (existingChg == null) {
            deleted.put(key, chg);
        } else if (existingChg.preModifyObj == null) {
            existingChg.preModifyObj = chg.preModifyObj;
        }
    }

    public abstract void recordModified(ModificationKey mkey, Change chg);

    public abstract void recordModified(MailboxStore mbox, int reason);

    public abstract void recordModified(T item, int reason);

    public abstract void recordModified(T item, int reason, T preModifyItem);

    abstract PendingModifications<T> add(PendingModifications<T> other);

    public void clear()  {
        created = null;
        deleted = null;
        modified = null;
        changedTypes.clear();
    }

    public static final class ModificationKeyMeta implements Serializable {

        private static final long serialVersionUID = -5509441698584047140L;
        String accountId;
        Integer itemId;

        public ModificationKeyMeta(String accountId, int itemId) {
            this.accountId = accountId;
            this.itemId = itemId;
        }

    }

    public static final class ChangeMeta implements Serializable {

        private static final long serialVersionUID = 5910956366698510738L;

        public static enum ObjectType {
            MAILBOX, MAILITEM, MAILITEMTYPE
        }
        public ObjectType whatType;
        public String metaWhat;
        public int    metaWhy;
        public ObjectType preModifyObjType;
        public String metaPreModifyObj;

        public ChangeMeta(ObjectType type, String thing, int reason, ObjectType preModifyObjType, String preModifyObj) {
            whatType = type;
            metaWhat = thing; // MailItem.Type for deletions
            metaWhy = reason; // not applicable for deletions
            this.preModifyObjType = preModifyObjType;
            metaPreModifyObj = preModifyObj;
        }

    }

}
