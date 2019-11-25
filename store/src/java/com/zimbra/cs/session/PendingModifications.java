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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zimbra.client.ZBaseItem;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.mailbox.BaseItemInfo;
import com.zimbra.common.mailbox.MailboxStore;
import com.zimbra.common.mailbox.ZimbraMailItem;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mailbox.util.TypedIdList;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.mail.type.DeleteItemNotification;
import com.zimbra.soap.mail.type.ModifyNotification.ModifyTagNotification;
import com.zimbra.soap.mail.type.PendingFolderModifications;


/**
 * @param <T> - MailItem (local mailbox) | ZBaseItem (remote mailbox)
*/
public abstract class PendingModifications<T extends ZimbraMailItem> {
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
        public static final int MODSEQ           = 0x20000000;
        public static final int POP3UID          = 0x40000000;
        public static final int ALL_FIELDS       = ~0;

        public Object what;
        public int    why;
        public Object preModifyObj;
        private int   folderId = -1;

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

        protected void toStringInit(StringBuilder sb) {
            if (what instanceof ZBaseItem) {
                ZBaseItem item = (ZBaseItem) what;
                int idInMbox = 0;
                try {
                    idInMbox = item.getIdInMailbox();
                } catch (ServiceException e) {
                }
                sb.append(getItemType(item)).append(' ').append(idInMbox).append(":");
            } else if (what instanceof ZMailbox) {
                sb.append("mailbox:");
            }
        }

        /** @return ID of folder ID or -1 if not known/appropriate */
        public int getFolderId() {
            if (preModifyObj instanceof MailItem) {
                return ((MailItem) preModifyObj).getFolderId();
            }
            else {
                return folderId;
            }
        }

        public void setFolderId(int folderId) {
            this.folderId = folderId;
        }
    }

    public static class ModificationKey extends Pair<String, Integer> {
        public ModificationKey(String accountId, Integer itemId) {
            super(accountId, itemId);
        }

        public ModificationKey(BaseItemInfo item) {
            this("", 0);
            String acctId = null;
            int idInMbox = 0;
            try {
                acctId = item.getAccountId();
                idInMbox = item.getIdInMailbox();
            } catch (ServiceException e) {
                ZimbraLog.mailbox.warn("error retrieving account id or id in mailbox", e);
            }
            setAccountId(acctId);
            setItemId(Integer.valueOf(idInMbox));

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

    /**
     * Set of folders in which MailItems have changed
     */
    private final Set<Integer> changedParentFolders = Sets.newHashSet();

    /**
     * Set of folders that themselves have changed. We track this in addition to changedParentFolders
     * because folder interests should be triggered if either the parent folder OR the folder itself
     * has changed
     */
    private final Set<Integer> changedFolders = Sets.newHashSet();

    public LinkedHashMap<ModificationKey, BaseItemInfo> created;
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

    public abstract void recordCreated(BaseItemInfo item);

    public void recordDeleted(String acctId, int id, int parentFolderId, MailItem.Type type) {
        if (type != MailItem.Type.UNKNOWN) {
            changedTypes.add(type);
        }
        addChangedParentFolderId(parentFolderId);
        ModificationKey key = new ModificationKey(acctId, id);
        delete(key, type, null);
    }

    public void recordDeleted(String acctId, TypedIdList idlist) {
        changedTypes.addAll(idlist.types());
        for (Map.Entry<MailItem.Type, List<TypedIdList.ItemInfo>> entry : idlist) {
            MailItem.Type type = entry.getKey();
            for (TypedIdList.ItemInfo iinfo : entry.getValue()) {
                addChangedParentFolderId(iinfo.getFolderId());
                if (type == MailItem.Type.MESSAGE || type == MailItem.Type.FOLDER) {
                    delete(new ModificationKey(acctId, iinfo.getId()), type, iinfo.getFolderId());
                }
                else {
                    delete(new ModificationKey(acctId, iinfo.getId()), type, null);
                }
            }
        }
    }

    public abstract void recordDeleted(ZimbraMailItem itemSnapshot);

    public void recordDeleted(Map<ModificationKey, Change> deletes) {
        if (deletes != null && !deletes.isEmpty()) {
            for (Map.Entry<ModificationKey, Change> entry : deletes.entrySet()) {
                changedTypes.add((MailItem.Type) entry.getValue().what);
                addChangedParentFolderId(entry.getValue().getFolderId());
                delete(entry.getKey(), entry.getValue());
            }
        }
    }

    protected abstract void delete(ModificationKey key, MailItem.Type type, Object itemSnapshot);

    protected void delete(PendingModifications.ModificationKey key, Type type, int folderId) {
        Change chg = new Change(type, Change.NONE, null);
        chg.setFolderId(folderId);
        delete(key, chg);
    }

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

    public abstract void recordModified(BaseItemInfo item, int reason);

    public abstract void recordModified(BaseItemInfo item, int reason, ZimbraMailItem preModifyItem);

    abstract PendingModifications<T> add(PendingModifications<T> other);
    abstract boolean trackingFolderIds();

    public Set<Integer> getChangedParentFolders() {
        return Collections.unmodifiableSet(changedParentFolders);
    }

    public Set<Integer> getChangedFolders() {
        return Collections.unmodifiableSet(changedFolders);
    }

    public Set<Integer> getAllChangedFolders() {
        return Collections.unmodifiableSet(Sets.union(changedFolders, changedParentFolders));
    }

    void addChangedParentFolderId(int folderId) {
        if (!trackingFolderIds()) {
            return;
        }
        if (folderId == Mailbox.ID_AUTO_INCREMENT) {
            // Not expecting this
            ZimbraLog.misc.trace("ChangedFolderId unset (i.e. -1) %s", ZimbraLog.getStackTrace(15));
            return;
        }
        changedParentFolders.add(folderId);
    }

    void addChangedFolderId(int folderId) {
        if (!trackingFolderIds()) {
            return;
        }
        changedFolders.add(folderId);
    }

    void addChangedParentFolderIds(Set<Integer> folderIds) {
        if (trackingFolderIds()) {
            changedParentFolders.addAll(folderIds);
            changedParentFolders.remove(Mailbox.ID_AUTO_INCREMENT); /* just in case it is in the list of folderIds */
            if (ZimbraLog.misc.isTraceEnabled() && folderIds.contains(Mailbox.ID_AUTO_INCREMENT)) {
                ZimbraLog.misc.trace("ChangedFolderId -1 in '%s' %s", folderIds, ZimbraLog.getStackTrace(15));
            }
        }
    }

    public void clear()  {
        created = null;
        deleted = null;
        modified = null;
        changedTypes.clear();
        changedParentFolders.clear();
    }

    public static MailItem.Type getItemType(BaseItemInfo item) {
        return MailItem.Type.fromCommon(item.getMailItemType());
    }

    public static final class ModificationKeyMeta implements Serializable {

        private static final long serialVersionUID = -5509441698584047140L;
        String accountId;
        Integer itemId;

        public ModificationKeyMeta(String accountId, int itemId) {
            this.accountId = accountId;
            this.itemId = itemId;
        }

        private final void readObject(ObjectInputStream in) throws java.io.IOException {
            throw new IOException("Cannot be deserialized");
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

        private final void readObject(ObjectInputStream in) throws java.io.IOException {
            throw new IOException("Cannot be deserialized");
         }

    }

    @SuppressWarnings("rawtypes")
    public static Map<Integer, PendingFolderModifications> encodeIMAPFolderModifications(PendingModifications accountMods) throws ServiceException {
        return encodeIMAPFolderModifications(accountMods, null);
    }

    @SuppressWarnings("rawtypes")
    public static Map<Integer, PendingFolderModifications> encodeIMAPFolderModifications(PendingModifications accountMods, Set<Integer> folderInterests) throws ServiceException {
        HashMap<Integer, PendingFolderModifications> folderMap = Maps.newHashMap();
        if(accountMods!= null && accountMods.created != null) {
            for(Object mod : accountMods.created.values()) {
                if(mod instanceof BaseItemInfo) {
                    Integer folderId = ((BaseItemInfo)mod).getFolderIdInMailbox();
                    if(folderInterests != null && !folderInterests.contains(folderId)) {
                        continue;
                    }
                    JaxbUtil.getFolderMods(folderId, folderMap).addCreatedItem(JaxbUtil.getCreatedItemSOAP((BaseItemInfo)mod));
                }
            }
        }

        if(accountMods!= null && accountMods.modified != null) {
            //aggregate tag changes so they are sent to each folder we are interested in
            List<ModifyTagNotification> tagMods = new ArrayList<ModifyTagNotification>();
            List<DeleteItemNotification> tagDeletes = new ArrayList<DeleteItemNotification>();
            for(Object maybeTagChange : accountMods.modified.values()) {
                if(maybeTagChange instanceof Change) {
                    Object maybeTag = ((Change) maybeTagChange).what;
                    if(maybeTag != null && maybeTag instanceof Tag) {
                        Tag tag = (Tag) maybeTag;
                        tagMods.add(new ModifyTagNotification(tag.getIdInMailbox(), tag.getName(), ((Change) maybeTagChange).why));
                    }
                }
            }
            if(accountMods!= null && accountMods.deleted != null) {
                @SuppressWarnings("unchecked")
                Map<ModificationKey, Change> deletedMap = accountMods.deleted;
                for (Map.Entry<ModificationKey, Change> entry : deletedMap.entrySet()) {
                    ModificationKey key = entry.getKey();
                    Change mod = entry.getValue();
                    if(mod instanceof Change) {
                        Object what = mod.what;
                        if(what != null && what instanceof MailItem.Type) {
                            if(what == MailItem.Type.TAG) {
                                //aggregate tag deletions so they are sent to each folder we are interested in
                                tagDeletes.add(JaxbUtil.getDeletedItemSOAP(key.getItemId(), what.toString()));
                            } else {
                                Integer folderId;
                                if(what == MailItem.Type.FOLDER) {
                                    folderId = key.getItemId();
                                } else {
                                    folderId = mod.getFolderId();
                                }
                                if(folderInterests != null && !folderInterests.contains(folderId)) {
                                    continue;
                                }
                                JaxbUtil.getFolderMods(folderId, folderMap).addDeletedItem(JaxbUtil.getDeletedItemSOAP(key.getItemId(), what.toString()));
                            }
                        }
                    }
                }
            }
            for(Object mod : accountMods.modified.values()) {
                if(mod instanceof Change) {
                    Object what = ((Change) mod).what;
                    if(what != null && what instanceof BaseItemInfo) {
                        BaseItemInfo itemInfo = (BaseItemInfo)what;
                        Integer folderId = itemInfo.getFolderIdInMailbox();
                        if (itemInfo instanceof Folder) {
                            Integer itemId = itemInfo.getIdInMailbox();
                            if(folderInterests != null &&
                                    !folderInterests.contains(folderId) &&
                                    !folderInterests.contains(itemId)) {
                                continue;
                            }
                            if (!tagMods.isEmpty()) {
                                PendingFolderModifications folderMods = JaxbUtil.getFolderMods(itemId, folderMap);
                                for (ModifyTagNotification modTag: tagMods) {
                                    folderMods.addModifiedTag(modTag);
                                }
                            } else if(!tagDeletes.isEmpty()) {
                                PendingFolderModifications folderMods = JaxbUtil.getFolderMods(itemId, folderMap);
                                for(DeleteItemNotification tagDelete :tagDeletes) {
                                    folderMods.addDeletedItem(tagDelete);
                                }
                            }
                        } else if (!(itemInfo instanceof Tag)){
                            if(folderInterests != null && !folderInterests.contains(folderId)) {
                                continue;
                            }
                            JaxbUtil.getFolderMods(folderId, folderMap).addModifiedMsg(JaxbUtil.getModifiedItemSOAP(itemInfo, ((Change) mod).why));
                        }
                    }
                }
            }
        }

        return folderMap;
    }
}
