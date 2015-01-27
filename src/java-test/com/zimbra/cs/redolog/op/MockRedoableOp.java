/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
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

package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.mailbox.Folder.FolderOptions;
import com.zimbra.cs.redolog.RedoLogOutput;
import com.zimbra.cs.redolog.TransactionId;

/**
 * Mock redo op for testing. Exposes serverId and other internals for testing purposes
 *
 */
public class MockRedoableOp extends CreateFolder {

    public MockRedoableOp(int mailboxId, String name, int parentId,
            FolderOptions fopt) {
        super(mailboxId, name, parentId, fopt);
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public static String getLocalServerId() {
        return RedoableOp.LOCAL_SERVER_ID;
    }

    @Override
    public void setTransactionId(TransactionId txnId) {
        super.setTransactionId(txnId);
    }

    public void serializeOperation(RedoLogOutput out) throws IOException {
        super.serializeHeader(out);
        super.serializeData(out);
    }

    @Override
    public void setTimestamp(long currentTimeMillis) {
        super.setTimestamp(currentTimeMillis);
    }
}
