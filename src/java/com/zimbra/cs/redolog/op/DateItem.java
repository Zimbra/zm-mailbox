/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006 Zimbra, Inc.
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
/*
 * Created on Nov 12, 2005
 */
package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

/**
 * @author tfr
 */
public class DateItem extends RedoableOp {

    private int mId;
    private byte mType;
    private long mDate;

    public DateItem() { }

    public DateItem(int mailboxId, int itemId, byte type, long date) {
        setMailboxId(mailboxId);
        mId = itemId;
        mType = type;
        mDate = date;
    }

    public int getOpCode() {
        return OP_DATE_ITEM;
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=").append(mId);
        sb.append(", date=").append(mDate);
        return sb.toString();
    }

    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mId);
        out.writeByte(mType);
        out.writeLong(mDate);
    }

    protected void deserializeData(RedoLogInput in) throws IOException {
        mId = in.readInt();
        mType = in.readByte();
        mDate = in.readLong();
    }

    public void redo() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        mbox.setDate(getOperationContext(), mId, mType, mDate);
    }
}
