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

import com.zimbra.common.mailbox.MailboxStore;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.Mailbox;

public final class PendingLocalModifications extends PendingModifications<MailItem> {

    public PendingLocalModifications() {
    }

    public static final class Change extends PendingModifications.Change {

        Change(Object thing, int reason, Object preModifyObj) {
            super(thing, reason, preModifyObj);
        }

        @Override
        protected void toStringInit(StringBuilder sb) {
            if (what instanceof MailItem) {
                MailItem item = (MailItem) what;
                sb.append(item.getType()).append(' ').append(item.getId()).append(":");
            } else if (what instanceof Mailbox) {
                sb.append("mailbox:");
            }

        }
    }

    public static final class ModificationKey extends PendingModifications.ModificationKey {
        public ModificationKey(MailItem item) {
            super(item.getMailbox().getAccountId(), item.getId());
        }
    }

    @Override
    PendingModifications<MailItem> add(PendingModifications<MailItem> other) {
        changedTypes.addAll(other.changedTypes);

        if (other.deleted != null) {
            for (Map.Entry<PendingModifications.ModificationKey, PendingModifications.Change> entry : other.deleted
                    .entrySet()) {
                delete(entry.getKey(), entry.getValue());
            }
        }

        if (other.created != null) {
            for (MailItem item : other.created.values()) {
                recordCreated(item);
            }
        }

        if (other.modified != null) {
            for (PendingModifications.Change chg : other.modified.values()) {
                if (chg.what instanceof MailItem) {
                    recordModified((MailItem) chg.what, chg.why, (MailItem) chg.preModifyObj);
                } else if (chg.what instanceof Mailbox) {
                    recordModified((Mailbox) chg.what, chg.why);
                }
            }
        }

        return this;
    }

    @Override
    protected void delete(PendingModifications.ModificationKey key, Type type, MailItem itemSnapshot) {
        delete(key, new Change(type, Change.NONE, itemSnapshot));
    }

    @Override
    public void recordCreated(MailItem item) {
        if (created == null) {
            created = new LinkedHashMap<PendingModifications.ModificationKey, MailItem>();
        }
        changedTypes.add(item.getType());
        created.put(new ModificationKey(item), item);

    }

    @Override
    public void recordDeleted(MailItem itemSnapshot) {
        MailItem.Type type = itemSnapshot.getType();
        changedTypes.add(type);
        delete(new ModificationKey(itemSnapshot), type, itemSnapshot);
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
    public void recordModified(MailItem item, int reason) {
        changedTypes.add(item.getType());
        recordModified(new ModificationKey(item), item, reason, null, true);
    }

    @Override
    public void recordModified(MailItem item, int reason, MailItem preModifyItem) {
        changedTypes.add(item.getType());
        recordModified(new ModificationKey(item), item, reason, preModifyItem, false);
    }

    private void recordModified(PendingModifications.ModificationKey key, Object item, int reason, Object preModifyObj,
            boolean snapshotItem) {
        PendingModifications.Change chg = null;
        if (created != null && created.containsKey(key)) {
            if (item instanceof MailItem) {
                recordCreated((MailItem) item);
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

}
