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
package com.zimbra.cs.session;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.zimbra.common.mailbox.BaseItemInfo;
import com.zimbra.common.mailbox.MailboxStore;
import com.zimbra.common.mailbox.ZimbraMailItem;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.io.SecureObjectInputStream;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;

public final class PendingLocalModifications extends PendingModifications<MailItem> {

    public PendingLocalModifications() {
    }

    @Override
    PendingModifications<MailItem> add(PendingModifications<MailItem> other) {
        changedTypes.addAll(other.changedTypes);
        addChangedParentFolderIds(other.getChangedParentFolders());

        if (other.deleted != null) {
            for (Map.Entry<PendingModifications.ModificationKey, PendingModifications.Change> entry : other.deleted
                    .entrySet()) {
                delete(entry.getKey(), entry.getValue());
            }
        }

        if (other.created != null) {
            for (BaseItemInfo item : other.created.values()) {
                recordCreated(item);
            }
        }

        if (other.modified != null) {
            for (PendingModifications.Change chg : other.modified.values()) {
                if (chg.what instanceof ZimbraMailItem) {
                    recordModified((ZimbraMailItem) chg.what, chg.why, (ZimbraMailItem) chg.preModifyObj);
                } else if (chg.what instanceof Mailbox) {
                    recordModified((Mailbox) chg.what, chg.why);
                }
            }
        }

        return this;
    }

    @Override
    protected void delete(PendingModifications.ModificationKey key, Type type, Object itemSnapshot) {
        delete(key, new Change(type, Change.NONE, itemSnapshot));
    }

    @Override
    public void recordCreated(BaseItemInfo item) {
        if (created == null) {
            created = new LinkedHashMap<PendingModifications.ModificationKey, BaseItemInfo>();
        }
        changedTypes.add(MailItem.Type.fromCommon(item.getMailItemType()));
        try {
            addChangedParentFolderId(item.getFolderIdInMailbox());
        } catch (ServiceException e) {
            ZimbraLog.mailbox.warn("error getting folder ID for modified item");
        }
        created.put(new ModificationKey(item), item);
    }

    @Override
    public void recordDeleted(ZimbraMailItem itemSnapshot) {
        if (null != itemSnapshot) {
            MailItem.Type type = MailItem.Type.fromCommon(itemSnapshot.getMailItemType());
            changedTypes.add(type);
            try {
                addChangedParentFolderId(itemSnapshot.getFolderIdInMailbox());
            } catch (ServiceException e) {
                ZimbraLog.mailbox.warn("error getting folder ID for modified item");
            }
            delete(new ModificationKey(itemSnapshot), type, itemSnapshot);
        }
        else {
        	ZimbraLog.mailbox.warn("PendingLocalModifications.recordDeleted itemSnapshot is NULL");
        }
    }

    @Override
    public void recordModified(PendingModifications.ModificationKey mkey, PendingModifications.Change chg) {
        recordModified(mkey, chg.what, chg.why, chg.preModifyObj, false);
    }

    @Override
    public void recordModified(MailboxStore mbox, int reason) {
        // Not recording preModify state of the mailbox for now
        if (mbox instanceof Mailbox) {
            Mailbox mb = (Mailbox) mbox;
            recordModified(new PendingModifications.ModificationKey(mb.getAccountId(), 0), mbox, reason, null, false);
        }
    }

    @Override
    public void recordModified(BaseItemInfo item, int reason) {
        MailItem.Type type = MailItem.Type.fromCommon(item.getMailItemType());
        changedTypes.add(type);
        try {
            addChangedParentFolderId(item.getFolderIdInMailbox());
        } catch (ServiceException e) {
            ZimbraLog.mailbox.warn("error getting folder ID for modified item");
        }
        if (type == MailItem.Type.FOLDER) {
            try {
                addChangedFolderId(item.getIdInMailbox());
            } catch (ServiceException e) {
                ZimbraLog.mailbox.warn("error getting ID for modified item");
            }
        }
        recordModified(new ModificationKey(item), item, reason, null, true);
    }

    @Override
    public void recordModified(BaseItemInfo item, int reason, ZimbraMailItem preModifyItem) {
        MailItem.Type type = MailItem.Type.fromCommon(item.getMailItemType());
        changedTypes.add(type);
        try {
            addChangedParentFolderId(item.getFolderIdInMailbox());
        } catch (ServiceException e) {
            ZimbraLog.mailbox.warn("error getting folder ID for modified item");
        }
        if (type == MailItem.Type.FOLDER) {
            try {
                addChangedFolderId(item.getIdInMailbox());
            } catch (ServiceException e) {
                ZimbraLog.mailbox.warn("error getting ID for modified item");
            }
        }
        recordModified(new ModificationKey(item), item, reason, preModifyItem, false);
    }

    private void recordModified(PendingModifications.ModificationKey key, Object item, int reason, Object preModifyObj,
            boolean snapshotItem) {
        PendingModifications.Change chg = null;
        if (created != null && created.containsKey(key)) {
            if (item instanceof ZimbraMailItem) {
                recordCreated((ZimbraMailItem) item);
            }
            return;
        } else if (deleted != null && deleted.containsKey(key)) {
            return;
        } else if (modified == null) {
            modified = new HashMap<PendingModifications.ModificationKey, PendingModifications.Change>();
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

    private static Map<PendingModifications.ModificationKey, PendingModifications.Change> getOriginal(Mailbox mbox,
            Map<String, ChangeMeta> map) throws ServiceException {
        if (map == null) {
            return null;
        }
        Map<PendingModifications.ModificationKey, PendingModifications.Change> ret = new LinkedHashMap<PendingModifications.ModificationKey, PendingModifications.Change>();
        Iterator<Entry<String, ChangeMeta>> iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, ChangeMeta> entry = iter.next();
            ModificationKeyMeta keyMeta = ModificationKeyMeta.fromString(entry.getKey());
            PendingModifications.ModificationKey key = new PendingModifications.ModificationKey(
                    keyMeta.accountId, keyMeta.itemId);
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
                    folder.setParentId(folder.getFolderId());
                }
            } else if (changeMeta.whatType == ChangeMeta.ObjectType.MAILITEMTYPE) {
                what = MailItem.Type.of(changeMeta.metaWhat);
            } else if (changeMeta.whatType == ChangeMeta.ObjectType.MAILBOX) {
                mbox.refreshMailbox(null);
                what = mbox;
            } else {
                ZimbraLog.session.warn("Unexpected mailbox change type received: %s", changeMeta.whatType);
                continue;
            }

            if (changeMeta.preModifyObjType == ChangeMeta.ObjectType.MAILITEM) {
                Metadata meta = new Metadata(changeMeta.metaPreModifyObj);
                MailItem.UnderlyingData ud = new MailItem.UnderlyingData();
                ud.deserialize(meta);
                preModifyObj = MailItem.constructItem(mbox, ud, true);
                if (preModifyObj instanceof Folder) {
                    Folder folder = ((Folder) preModifyObj);
                    folder.setParentId(folder.getFolderId());
                }
            } else if (changeMeta.preModifyObjType == ChangeMeta.ObjectType.MAILITEMTYPE) {
                preModifyObj = MailItem.Type.of(changeMeta.metaPreModifyObj);
            } else if (changeMeta.preModifyObjType == ChangeMeta.ObjectType.MAILBOX) {
                what = mbox;
            } else if (changeMeta.preModifyObjType != null ){
                ZimbraLog.session.warn("Unexpected mailbox change type received: %s", changeMeta.preModifyObjType);
                continue;
            }
            PendingModifications.Change change = new Change(what, changeMeta.metaWhy, preModifyObj);
            ret.put(key, change);
        }
        return ret;
    }

    private Map<String, String> encodeCreatedMap() throws ServiceException {
        if (created == null) {
            return null;
        } else {
        	ConcurrentHashMap<ModificationKey, BaseItemInfo> concurrentCreated = new ConcurrentHashMap<PendingModifications.ModificationKey, BaseItemInfo>(created);
        	Map<String, String> createdMeta = new ConcurrentHashMap<>();
            for (Iterator<Map.Entry<ModificationKey, BaseItemInfo>> iter = concurrentCreated.entrySet().iterator();iter.hasNext();) {
            	Map.Entry<ModificationKey, BaseItemInfo> entry = iter.next();
                ModificationKey key = entry.getKey();
                BaseItemInfo itemInfo = entry.getValue();
                if (itemInfo instanceof MailItem) {
                    MailItem item = (MailItem) itemInfo;
                    Metadata md = item.getUnderlyingData().serialize();
                    createdMeta.put(new ModificationKeyMeta(key.getAccountId(), key.getItemId()).toString(), md.toString());
                }
            }
            return createdMeta;
        }
    }

    private static Map<String, ChangeMeta> encodeMap(Map<ModificationKey, Change> map) throws ServiceException {
        if (map == null) {
            return null;
        }
        ConcurrentHashMap<ModificationKey, Change> concurrentMap = new ConcurrentHashMap<ModificationKey, Change>(map);
        Map<String, ChangeMeta> metaMap = new ConcurrentHashMap<>();
        for (Iterator<Map.Entry<ModificationKey, Change>> iter = concurrentMap.entrySet().iterator();iter.hasNext();) {
        	Map.Entry<ModificationKey, Change> entry = iter.next();
            ModificationKey key = entry.getKey();
            Change change = entry.getValue();
            ChangeMeta.ObjectType type = null;
            ChangeMeta.ObjectType preModType = null;
            String str = null;
            String preModStr = null;
            if (change.what instanceof Mailbox) {
                type = ChangeMeta.ObjectType.MAILBOX;
            } else if (change.what instanceof MailItem) {
                type = ChangeMeta.ObjectType.MAILITEM;
                str = ((MailItem) change.what).getUnderlyingData().serialize().toString();
                if (change.preModifyObj != null) {
                    preModStr = ((MailItem) change.preModifyObj).getUnderlyingData().serialize().toString();
                }
            } else if (change.what instanceof MailItem.Type) {
                type = ChangeMeta.ObjectType.MAILITEMTYPE;
                str = ((MailItem.Type) change.what).toString();
                if (change.preModifyObj instanceof MailItem.Type) {
                    preModStr = ((MailItem.Type) change.preModifyObj).toString();
                }
            }
            ChangeMeta changeMeta = new ChangeMeta(type, str, change.why, preModType, preModStr);
            metaMap.put(new ModificationKeyMeta(key.getAccountId(), key.getItemId()).toString(), changeMeta);
        }
        return metaMap;
    }

    public static class PendingModificationSnapshot {
        private Set<Type> changedTypes;
        private Set<Integer> changedFolders;
        private Set<Integer> changedParentFolders;
        private Map<String, String> created;
        private Map<String, ChangeMeta> modified;
        private Map<String, ChangeMeta> deleted;
    }

    public PendingModificationSnapshot toSnapshot() throws ServiceException {
        PendingModificationSnapshot snapshot = new PendingModificationSnapshot();
        snapshot.changedTypes = new HashSet<>(changedTypes);
        snapshot.changedFolders = new HashSet<>(getChangedFolders());
        snapshot.changedParentFolders = new HashSet<>(getChangedParentFolders());
        snapshot.modified = encodeMap(modified);
        snapshot.deleted = encodeMap(deleted);
        snapshot.created = encodeCreatedMap();
        return snapshot;
    }

    public static PendingLocalModifications fromSnapshot(Mailbox mbox, PendingModificationSnapshot snapshot) throws ServiceException {
        PendingLocalModifications pms = new PendingLocalModifications();
        pms.changedTypes = snapshot.changedTypes;
        pms.addChangedParentFolderIds(snapshot.changedParentFolders);
        for (Integer id: snapshot.changedFolders) {
            pms.addChangedFolderId(id);
        }
        if (snapshot.created != null) {
            pms.created = new LinkedHashMap<PendingModifications.ModificationKey, BaseItemInfo>();
            Iterator<Entry<String, String>> iter = snapshot.created.entrySet().iterator();
            while (iter.hasNext()) {
                Entry<String, String> entry = iter.next();
                Metadata meta = new Metadata(entry.getValue());
                MailItem.UnderlyingData ud = new MailItem.UnderlyingData();
                ud.deserialize(meta);
                MailItem item = MailItem.constructItem(mbox, ud, true);
                if (item instanceof Folder) {
                    Folder folder = ((Folder) item);
                    folder.setParentId(folder.getFolderId());
                }
                ModificationKeyMeta keyMeta = ModificationKeyMeta.fromString(entry.getKey());
                PendingModifications.ModificationKey key = new PendingModifications.ModificationKey(
                        keyMeta.accountId, keyMeta.itemId);
                pms.created.put(key, item);
            }
        }
        pms.modified = getOriginal(mbox, snapshot.modified);
        pms.deleted = getOriginal(mbox, snapshot.deleted);
        return pms;
    }

    @SuppressWarnings("unchecked")
    public static PendingLocalModifications deserialize(Mailbox mbox, byte[] data)
            throws IOException, ClassNotFoundException, ServiceException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        PendingLocalModifications pms = new PendingLocalModifications();
        try (ObjectInputStream ois = new SecureObjectInputStream(bis, Type.class.getName())) {
            pms.changedTypes = (Set<Type>) ois.readObject();
            pms.addChangedParentFolderIds((Set<Integer>) ois.readObject());

            LinkedHashMap<ModificationKeyMeta, String> metaCreated = (LinkedHashMap<ModificationKeyMeta, String>) ois
                    .readObject();
            if (metaCreated != null) {
                pms.created = new LinkedHashMap<PendingModifications.ModificationKey, BaseItemInfo>();
                Iterator<Entry<ModificationKeyMeta, String>> iter = metaCreated.entrySet().iterator();
                while (iter.hasNext()) {
                    Entry<ModificationKeyMeta, String> entry = iter.next();
                    Metadata meta = new Metadata(entry.getValue());
                    MailItem.UnderlyingData ud = new MailItem.UnderlyingData();
                    ud.deserialize(meta);
                    MailItem item = MailItem.constructItem(mbox, ud, true);
                    if (item instanceof Folder) {
                        Folder folder = ((Folder) item);
                        folder.setParent(mbox.getFolderById(null, folder.getFolderId()));

                    }
                    PendingModifications.ModificationKey key = new PendingModifications.ModificationKey(
                            entry.getKey().accountId, entry.getKey().itemId);
                    pms.created.put(key, item);
                }
            }

            Map<ModificationKeyMeta, ChangeMeta> metaModified = (Map<ModificationKeyMeta, ChangeMeta>) ois.readObject();
            Map<String, ChangeMeta> metaModifiedStringKeys = new HashMap<>();
            for (Map.Entry<ModificationKeyMeta, ChangeMeta> entry: metaModified.entrySet()) {
                metaModifiedStringKeys.put(entry.getKey().toString(), entry.getValue());
            }
            pms.modified = getOriginal(mbox, metaModifiedStringKeys);

            Map<ModificationKeyMeta, ChangeMeta> metaDeleted = (Map<ModificationKeyMeta, ChangeMeta>) ois.readObject();
            Map<String, ChangeMeta> metaDeletedStringKeys = new HashMap<>();
            for (Map.Entry<ModificationKeyMeta, ChangeMeta> entry: metaDeleted.entrySet()) {
                metaDeletedStringKeys.put(entry.getKey().toString(), entry.getValue());
            }
            pms.deleted = getOriginal(mbox, metaDeletedStringKeys);
        }
        return pms;
    }

    @Override
    boolean trackingFolderIds() {
        return true;
    }
}