/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
/*
 * Created on Sep 19, 2005
 */
package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;

/**
 * @author dkarp
 */
public class ColorItem extends RedoableOp {

    private int mId;
    private byte mType;
    private byte mColor;

    public ColorItem() {
        mId = UNKNOWN_ID;
    }

    public ColorItem(int mailboxId, int id, byte type, byte color) {
        setMailboxId(mailboxId);
        mId = id;
        mType = type;
        mColor = color;
    }

    public int getOpCode() {
        return OP_COLOR_ITEM;
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=");
        sb.append(mId).append(", color=").append(mColor);
        return sb.toString();
    }

    protected void serializeData(DataOutput out) throws IOException {
        out.writeInt(mId);
        out.writeByte(mType);
        out.writeByte(mColor);
    }

    protected void deserializeData(DataInput in) throws IOException {
        mId = in.readInt();
        mType = in.readByte();
        mColor = in.readByte();
    }

    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mailbox = Mailbox.getMailboxById(mboxId);
        mailbox.setColor(getOperationContext(), mId, mType, mColor);
    }
}
