/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class EnableSharedReminder extends RedoableOp {

    private int mountpointId;
    private boolean enabled;

    public EnableSharedReminder() {
        super(MailboxOperation.EnableSharedReminder);
        mountpointId = UNKNOWN_ID;
        enabled = false;
    }

    public EnableSharedReminder(int mailboxId, int mountpointId, boolean enabled) {
        this();
        setMailboxId(mailboxId);
        this.mountpointId = mountpointId;
        this.enabled = enabled;
    }

    @Override protected String getPrintableData() {
        StringBuilder sb = new StringBuilder("mountpoint=").append(mountpointId);
        sb.append(", reminderEnabled=").append(enabled);
        return sb.toString();
    }

    @Override protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mountpointId);
        out.writeBoolean(enabled);
    }

    @Override protected void deserializeData(RedoLogInput in) throws IOException {
        mountpointId = in.readInt();
        enabled = in.readBoolean();
    }

    
    @Override public void redo() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        mbox.enableSharedReminder(getOperationContext(), mountpointId, enabled);
    }
}
