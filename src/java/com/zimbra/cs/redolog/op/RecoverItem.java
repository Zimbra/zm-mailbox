/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogInput;

public class RecoverItem extends CopyItem {

    public RecoverItem() {
        super();
        mOperation = MailboxOperation.RecoverItem;
        setFromDumpster(true);
    }

    public RecoverItem(int mailboxId, MailItem.Type type, int folderId) {
        super(mailboxId, type, folderId);
        mOperation = MailboxOperation.RecoverItem;
        setFromDumpster(true);
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        super.deserializeData(in);
        setFromDumpster(true);  // shouldn't be necessary, but let's be absolutely sure
    }
}
