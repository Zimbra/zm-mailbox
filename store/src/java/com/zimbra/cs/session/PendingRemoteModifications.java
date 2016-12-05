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

import java.util.LinkedHashMap;

import com.zimbra.client.ZBaseItem;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.mailbox.MailboxStore;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.Type;

public final class PendingRemoteModifications extends PendingModifications<ZBaseItem> {

    public PendingRemoteModifications() {
    }

    public static final class Change extends PendingModifications.Change {

        Change(Object thing, int reason, Object preModifyObj) {
            super(thing, reason, preModifyObj);
        }

        @Override
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

    }

    public static final class ModificationKey extends PendingModifications.ModificationKey {

        /*
         * TODO - Looks like we will be having to deal with ServiceExceptions in
         * numerous places in PendingRemoteModifications. See if there is a
         * cleaner way to handle them.
         */
        public ModificationKey(ZBaseItem item) {
            super("", 0);
            String actId = null;
            int idInMbox = 0;
            try {
                actId = item.getMailbox().getAccountId();
                idInMbox = item.getIdInMailbox();
            } catch (ServiceException e) {
                ZimbraLog.mailbox.warn("error retrieving account id or id in mailbox", e);
            }
            setAccountId(actId);
            setItemId(Integer.valueOf(idInMbox));
        }
    }

    public static MailItem.Type getItemType(ZBaseItem item) {
        return MailItem.Type.fromCommon(item.getMailItemType());
    }

    @Override
    protected void delete(PendingModifications.ModificationKey key, Type type, ZBaseItem itemSnapshot) {
        delete(key, new Change(type, Change.NONE, itemSnapshot));
    }

    @Override
    public void recordCreated(ZBaseItem item) {
        if (created == null) {
            created = new LinkedHashMap<PendingModifications.ModificationKey, ZBaseItem>();
        }
        changedTypes.add(getItemType(item));
        created.put(new ModificationKey(item), item);

    }

    @Override
    public void recordDeleted(ZBaseItem itemSnapshot) {
        MailItem.Type type = getItemType(itemSnapshot);
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
        String actId = null;
        try {
            actId = mbox.getAccountId();
        } catch (ServiceException e) {
            ZimbraLog.mailbox.warn("error retrieving account id in mailboxstore", e);
        }
        recordModified(new PendingModifications.ModificationKey(actId, 0), mbox, reason, null, false);
    }

    @Override
    public void recordModified(ZBaseItem item, int reason) {
        changedTypes.add(getItemType(item));
        recordModified(new ModificationKey(item), item, reason, null, true);
    }

    @Override
    public void recordModified(ZBaseItem item, int reason, ZBaseItem preModifyItem) {
        changedTypes.add(getItemType(item));
        recordModified(new ModificationKey(item), item, reason, preModifyItem, false);
    }

    private void recordModified(PendingModifications.ModificationKey key, Object item, int reason, Object preModifyObj,
            boolean snapshotItem) {
        // TODO - Implement
    }
}
