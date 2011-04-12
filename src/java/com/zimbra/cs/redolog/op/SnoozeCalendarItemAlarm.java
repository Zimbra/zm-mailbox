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

public class SnoozeCalendarItemAlarm extends RedoableOp {

    private int id;
    private long snoozeUntil;

    public SnoozeCalendarItemAlarm() {
        super(MailboxOperation.SnoozeCalendarItemAlarm);
        id = UNKNOWN_ID;
    }

    public SnoozeCalendarItemAlarm(int mailboxId, int id, long snoozeUntil) {
        this();
        setMailboxId(mailboxId);
        this.id = id;
        this.snoozeUntil = snoozeUntil;
    }

    @Override protected String getPrintableData() {
        StringBuilder sb = new StringBuilder("id=");
        sb.append(id).append(", snoozeUntil=").append(snoozeUntil);
        return sb.toString();
    }

    @Override protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(id);
        out.writeLong(snoozeUntil);
    }

    @Override protected void deserializeData(RedoLogInput in) throws IOException {
        id = in.readInt();
        snoozeUntil = in.readLong();
    }

    @Override public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mailbox = MailboxManager.getInstance().getMailboxById(mboxId);
        mailbox.snoozeCalendarItemAlarm(getOperationContext(), id, snoozeUntil);
    }
}
