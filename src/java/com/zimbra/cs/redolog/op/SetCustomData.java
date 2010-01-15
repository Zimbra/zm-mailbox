/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
import com.zimbra.cs.mailbox.MailItem.CustomMetadata;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class SetCustomData extends RedoableOp {

    private int mId;
    private byte mType;
    private CustomMetadata mExtendedData;

    public SetCustomData() { }

    public SetCustomData(long mailboxId, int id, byte type, CustomMetadata custom) {
        setMailboxId(mailboxId);
        mId = id;
        mType = type;
        mExtendedData = custom;
    }

    @Override public int getOpCode() {
        return OP_SET_CUSTOM_DATA;
    }

    @Override protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=");
        sb.append(mId).append(", data=").append(mExtendedData);
        return sb.toString();
    }

    @Override protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mId);
        out.writeByte(mType);
        out.writeUTF(mExtendedData.getSectionKey());
        out.writeUTF(mExtendedData.getSerializedValue());
    }

    @Override protected void deserializeData(RedoLogInput in) throws IOException {
        mId = in.readInt();
        mType = in.readByte();
        String extendedKey = in.readUTF();
        mExtendedData = new CustomMetadata(extendedKey, in.readUTF());
    }

    @Override public void redo() throws Exception {
        Mailbox mailbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        mailbox.setCustomData(getOperationContext(), mId, mType, mExtendedData);
    }
}
