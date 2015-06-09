/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Nov 28, 2004
 */
package com.zimbra.cs.session;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.util.TypedIdList;

public final class PendingModifications {
    public static final class Change {
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
            ToStringHelper tsh = Objects.toStringHelper(this)
                    .add("preModifyObj", preModifyObj)
                    .add("what", what)
                    .add("why", why);
                return tsh.toString();
        }
    }

    public static final class ModificationKey extends Pair<String, Integer> {
        public ModificationKey(String accountId, Integer itemId) {
            super(accountId, itemId);
        }

        public ModificationKey(MailItem item) {
            super(item.getAccountId(), item.getId());
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
    public Map<ModificationKey, Change> modified;
    public Map<ModificationKey, Change> deleted;

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

    public void recordCreated(MailItem item) {
        if (created == null) {
            created = new LinkedHashMap<ModificationKey, MailItem>();
        }
        changedTypes.add(item.getType());
        created.put(new ModificationKey(item), item);
    }

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

    public void recordDeleted(MailItem itemSnapshot) {
        MailItem.Type type = itemSnapshot.getType();
        changedTypes.add(type);
        delete(new ModificationKey(itemSnapshot), type, itemSnapshot);
    }

    public void recordDeleted(Map<ModificationKey, Change> deletes) {
        if (deletes != null && !deletes.isEmpty()) {
            for (Map.Entry<ModificationKey, Change> entry : deletes.entrySet()) {
                changedTypes.add((MailItem.Type) entry.getValue().what);
                delete(entry.getKey(), entry.getValue());
            }
        }
    }

    private void delete(ModificationKey key, MailItem.Type type, MailItem itemSnapshot) {
        delete(key, new Change(type, Change.NONE, itemSnapshot));
    }

    private void delete(ModificationKey key, Change chg) {
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

    public void recordModified(ModificationKey mkey, Change chg) {
        recordModified(mkey, chg.what, chg.why, chg.preModifyObj, false);
    }

    public void recordModified(Mailbox mbox, int reason) {
        // Not recording preModify state of the mailbox for now
        recordModified(new ModificationKey(mbox.getAccountId(), 0), mbox, reason, null, false);
    }

    public void recordModified(MailItem item, int reason) {
        changedTypes.add(item.getType());
        recordModified(new ModificationKey(item), item, reason, null, true);
    }

    public void recordModified(MailItem item, int reason, MailItem preModifyItem) {
        changedTypes.add(item.getType());
        recordModified(new ModificationKey(item), item, reason, preModifyItem, false);
    }

    private void recordModified(ModificationKey key, Object item, int reason,
            Object preModifyObj, boolean snapshotItem) {
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
                if (chg.preModifyObj == null) {
                    chg.preModifyObj = preModifyObj == null && snapshotItem ? snapshotItemIgnoreEx(item) : preModifyObj;
                }
            }
        }
        if (chg == null) {
            chg = new Change(item, reason,
                    preModifyObj == null && snapshotItem ? snapshotItemIgnoreEx(item) : preModifyObj);
        }
        modified.put(key, chg);
    }

    private static Object snapshotItemIgnoreEx(Object item) {
        if (item instanceof MailItem) {
            try {
                return ((MailItem) item).snapshotItem();
            } catch (ServiceException e) {
                ZimbraLog.mailbox.warn("Error in taking item snapshot", e);
            }
        }
        return null;
    }

    PendingModifications add(PendingModifications other) {
        changedTypes.addAll(other.changedTypes);

        if (other.deleted != null) {
            for (Map.Entry<ModificationKey, Change> entry : other.deleted.entrySet()) {
                delete(entry.getKey(), entry.getValue());
            }
        }

        if (other.created != null) {
            for (MailItem item : other.created.values()) {
                recordCreated(item);
            }
        }

        if (other.modified != null) {
            for (Change chg : other.modified.values()) {
                if (chg.what instanceof MailItem) {
                    recordModified((MailItem) chg.what, chg.why, (MailItem) chg.preModifyObj);
                } else if (chg.what instanceof Mailbox) {
                    recordModified((Mailbox) chg.what, chg.why);
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

    public static final class ModificationKeyMeta implements Serializable {

        String accountId;
        Integer itemId;

        public ModificationKeyMeta(String accountId, int itemId) {
            this.accountId = accountId;
            this.itemId = itemId;
        }
    }

    public static final class ChangeMeta implements Serializable {
        public static enum ObjectType {
            MAILBOX, MAILITEM, MAILITEMTYPE
        }
        public ObjectType whatType;
        public String metaWhat;
        public int    metaWhy;
        public ObjectType preModifyObjType;
        public String metaPreModifyObj;

        @JsonCreator
        public ChangeMeta(
                @JsonProperty("whatType") ObjectType type,
                @JsonProperty("metaWhat") String thing,
                @JsonProperty("metaWhy") int reason,
                @JsonProperty("preModifyObjType") ObjectType preModifyObjType,
                @JsonProperty("metaPreModifyObj") String preModifyObj)
        {
            whatType = type;
            metaWhat = thing; // MailItem.Type for deletions
            metaWhy = reason; // not applicable for deletions
            this.preModifyObjType = preModifyObjType;
            metaPreModifyObj = preModifyObj;
        }

    }

    static Map<ModificationKey, Change> getOriginal(Mailbox mbox, Map<ModificationKeyMeta, ChangeMeta> map) throws ServiceException {
        if (map == null) {
            return null;
        }
        Map<ModificationKey, Change> ret = new LinkedHashMap<ModificationKey, Change>();
        Iterator<Entry<ModificationKeyMeta, ChangeMeta>> iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<ModificationKeyMeta, ChangeMeta> entry = iter.next();
            ModificationKey key = new ModificationKey(entry.getKey().accountId, entry.getKey().itemId);
            ChangeMeta changeMeta = entry.getValue();
            Object what = null;
            Object preModifyObj = null;
            if (changeMeta.whatType == ChangeMeta.ObjectType.MAILITEM) {
                Metadata meta = new Metadata(changeMeta.metaWhat);
                MailItem.UnderlyingData ud = new MailItem.UnderlyingData();
                ud.deserialize(meta);
                what = MailItem.constructItem(mbox, ud, true);
                if (what instanceof Folder) {
                    Folder folder = ((Folder) what);
                    folder.setParent(mbox.getFolderById(null, folder.getFolderId()));
                }
            } else if (changeMeta.whatType == ChangeMeta.ObjectType.MAILITEMTYPE) {
                what = MailItem.Type.of(changeMeta.metaWhat);
            } else if (changeMeta.whatType == ChangeMeta.ObjectType.MAILBOX) {
                mbox.refreshMailbox(null);
                what = mbox;
            } else {
                ZimbraLog.session.warn("Unexpected mailbox change type received : " + changeMeta.whatType);
                continue;
            }

            if (changeMeta.preModifyObjType == ChangeMeta.ObjectType.MAILITEM) {
                Metadata meta = new Metadata(changeMeta.metaPreModifyObj);
                MailItem.UnderlyingData ud = new MailItem.UnderlyingData();
                ud.deserialize(meta);
                preModifyObj = MailItem.constructItem(mbox, ud, true);
                if (preModifyObj instanceof Folder) {
                    Folder folder = ((Folder) preModifyObj);
                    folder.setParent(mbox.getFolderById(null, folder.getFolderId()));
                }
            } else if (changeMeta.preModifyObjType == ChangeMeta.ObjectType.MAILITEMTYPE) {
                preModifyObj = MailItem.Type.of(changeMeta.metaPreModifyObj);
            } else if (changeMeta.whatType == ChangeMeta.ObjectType.MAILBOX) {
                what = mbox;
            } else {
                ZimbraLog.session.warn("Unexpected mailbox change type received : " + changeMeta.whatType);
                continue;
            }
            Change change = new Change(what, changeMeta.metaWhy, preModifyObj);
            ret.put(key, change);
        }
        return ret;
    }

    @Override
    public String toString() {
        ToStringHelper tsh = Objects.toStringHelper(this)
            .add("changedTypes", changedTypes)
            .add("created", created)
            .add("deleted", deleted)
            .add("modified", modified);
        return tsh.toString();
    }
}
