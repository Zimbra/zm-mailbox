/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2019 Synacor, Inc.
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

package com.zimbra.cs.mailbox;

import com.zimbra.common.util.Pair;

public class MailboxNotificationInfo extends Pair<String, Long> {

    public MailboxNotificationInfo(Mailbox mbox) {
        this(mbox.getAccountId(), mbox.getSize());
    }

    public MailboxNotificationInfo(String accountId, Long mailboxSize) {
        super(accountId, mailboxSize);
    }

    public String getAccountId() {
        return getFirst();
    }

    public long getSize() {
        return getSecond();
    }
}
