/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class DismissCalendarItemAlarm extends RedoableOp {

    private int mId;
    private long mDismissedAt;

    public DismissCalendarItemAlarm() {
        mId = UNKNOWN_ID;
    }

    public DismissCalendarItemAlarm(int mailboxId, int id, long dismissedAt) {
        setMailboxId(mailboxId);
        mId = id;
        mDismissedAt = dismissedAt;
    }

    public int getOpCode() {
        return OP_DISMISS_CALENDAR_ITEM_ALARM;
    }

    protected String getPrintableData() {
        StringBuilder sb = new StringBuilder("id=");
        sb.append(mId).append(", dismissedAt=").append(mDismissedAt);
        return sb.toString();
    }

    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mId);
        out.writeLong(mDismissedAt);
    }

    protected void deserializeData(RedoLogInput in) throws IOException {
        mId = in.readInt();
        mDismissedAt = in.readLong();
    }

    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mailbox = MailboxManager.getInstance().getMailboxById(mboxId);
        mailbox.dismissCalendarItemAlarm(getOperationContext(), mId, mDismissedAt);
    }
}
