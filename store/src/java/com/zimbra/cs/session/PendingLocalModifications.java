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

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;

public final class PendingLocalModifications extends PendingModifications<MailItem> {

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

}
