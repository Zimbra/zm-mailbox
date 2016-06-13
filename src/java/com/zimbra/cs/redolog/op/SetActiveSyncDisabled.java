/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class SetActiveSyncDisabled extends RedoableOp {
    
    private int folderId;
    private boolean disableActiveSync;

    public SetActiveSyncDisabled() {
        super(MailboxOperation.SetDisableActiveSync);
    }
    
    public SetActiveSyncDisabled(int mailboxId, int folderId, boolean disableActiveSync) {
        this();
        setMailboxId(mailboxId);
        this.folderId = folderId;
        this.disableActiveSync = disableActiveSync;
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        folderId = in.readInt();
        disableActiveSync = in.readBoolean();
    }

    @Override
    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=").append(folderId);
        sb.append(", disableActiveSync="+(disableActiveSync?"TRUE":"FALSE"));
        return sb.toString();
    }

    @Override
    public void redo() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        mbox.setActiveSyncDisabled(getOperationContext(), folderId, disableActiveSync);
    }

    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(folderId);
        out.writeBoolean(disableActiveSync);
    }
    
    /* Unit test methods */
    
    byte[] testSerialize() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializeData(new RedoLogOutput(out));
        return out.toByteArray();
    }
    
    void testDeserialize(byte[] data) throws IOException {
        deserializeData(new RedoLogInput(new ByteArrayInputStream(data)));
    }

}
