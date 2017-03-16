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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.zimbra.client.ZBaseItem;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.mailbox.BaseItemInfo;
import com.zimbra.common.mailbox.MailboxStore;
import com.zimbra.common.mailbox.ZimbraMailItem;
import com.zimbra.common.mailbox.ZimbraTag;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.soap.mail.type.CreateItemNotification;
import com.zimbra.soap.mail.type.DeleteItemNotification;
import com.zimbra.soap.mail.type.ModifyNotification;
import com.zimbra.soap.mail.type.ModifyNotification.ModifyItemNotification;
import com.zimbra.soap.mail.type.ModifyNotification.ModifyTagNotification;
import com.zimbra.soap.mail.type.ModifyNotification.RenameFolderNotification;
import com.zimbra.soap.mail.type.PendingFolderModifications;

public final class PendingRemoteModifications extends PendingModifications<ZBaseItem> {

    public PendingRemoteModifications() {
    }

    @Override
    PendingModifications<ZBaseItem> add(PendingModifications<ZBaseItem> other) {
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
                } else if (chg.what instanceof ZMailbox) {
                    recordModified((ZMailbox) chg.what, chg.why);
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
        changedTypes.add(getItemType(item));
        /* assumption - don't care about tracking folder IDs for PendingRemoteModifications */
        created.put(new ModificationKey(item), item);

    }

    @Override
    public void recordDeleted(ZimbraMailItem itemSnapshot) {
        MailItem.Type type = getItemType(itemSnapshot);
        try {
            recordDeleted(type, itemSnapshot.getAccountId(), itemSnapshot.getIdInMailbox());
        } catch (ServiceException e) {
            ZimbraLog.imap.warn("unable to record deleted message", e);
        }
    }

    public void recordDeleted(MailItem.Type type, String accountId, int itemId) {
        changedTypes.add(type);
        /* assumption - don't care about tracking folder IDs for PendingRemoteModifications */
        delete(new ModificationKey(accountId, itemId), type, null);
    }
    @Override
    public void recordModified(PendingModifications.ModificationKey mkey, PendingModifications.Change chg) {
        recordModified(mkey, chg.what, chg.why, chg.preModifyObj, false);
    }

    @Override
    public void recordModified(MailboxStore mbox, int reason) {
        // Not recording preModify state of the mailbox for now
        String actId = null;
        try {
            actId = mbox.getAccountId();
        } catch (ServiceException e) {
            ZimbraLog.mailbox.warn("error retrieving account id in mailboxstore", e);
        }
        recordModified(new PendingModifications.ModificationKey(actId, 0), mbox, reason, null, false);
    }

    @Override
    public void recordModified(BaseItemInfo item, int reason) {
        changedTypes.add(getItemType(item));
        /* assumption - don't care about tracking folder IDs for PendingRemoteModifications */
        recordModified(new ModificationKey(item), item, reason, null, true);
    }

    @Override
    public void recordModified(BaseItemInfo item, int reason, ZimbraMailItem preModifyItem) {
        changedTypes.add(getItemType(item));
        /* assumption - don't care about tracking folder IDs for PendingRemoteModifications */
        recordModified(new ModificationKey(item), item, reason, preModifyItem, false);
    }

    public void recordModified(ZimbraTag tag, String acctId, int reason) {
        ModificationKey key = new ModificationKey(acctId, tag.getTagId());
        recordModified(key, tag, reason, null, false);
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

    @Override
    boolean trackingFolderIds() {
        return false;
    }

    private static Object snapshotItemIgnoreEx(Object item) {
        // TODO - Do we need to be able to snapshot ZBaseItems?
        return null;
    }

    public static PendingRemoteModifications fromSOAP(PendingFolderModifications mods, Integer folderId, String acctId) {

        PendingRemoteModifications prms = new PendingRemoteModifications();
        for (CreateItemNotification createSpec: mods.getCreated()) {
            prms.recordCreated(ModificationItem.itemUpdate(createSpec.getMessageInfo(), folderId, acctId));
        }
        for (ModifyNotification modSpec: mods.getModified()) {
            int change = modSpec.getChangeBitmask();
            if (modSpec instanceof ModifyItemNotification) {
                ModifyItemNotification modifyItem = (ModifyItemNotification) modSpec;
                BaseItemInfo itemUpdate = ModificationItem.itemUpdate(modifyItem.getMessageInfo(), folderId, acctId);
                prms.recordModified(itemUpdate, change);
            } else if (modSpec instanceof ModifyTagNotification) {
                ModifyTagNotification modifyTag = (ModifyTagNotification) modSpec;
                int tagId = modifyTag.getId();
                String tagName = modifyTag.getName();
                ZimbraTag tagRename = ModificationItem.tagRename(tagId, tagName);
                prms.recordModified(tagRename, acctId, change);
            } else if (modSpec instanceof RenameFolderNotification) {
                RenameFolderNotification renameFolder = (RenameFolderNotification) modSpec;
                int renamedFolderId = renameFolder.getFolderId();
                String newPath = renameFolder.getPath();
                ModificationItem folderRename = ModificationItem.folderRename(renamedFolderId, newPath, acctId);
                prms.recordModified(folderRename, change);
            }
        }
        for (DeleteItemNotification delSpec: mods.getDeleted()) {
          int id = delSpec.getId();
          MailItem.Type type = MailItem.Type.of(delSpec.getType());
          prms.recordDeleted(type, acctId, id);
        }
        return prms;
    }
}
