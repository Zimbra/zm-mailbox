/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
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
 * Created on 2004. 7. 21.
 */
package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;


/**
 * @author jhahm
 */
public class ColorTag extends RedoableOp {

	private int mTagId;
	private byte mColor;

	public ColorTag() {
		mTagId = UNKNOWN_ID;
		mColor = 0;
	}

	public ColorTag(int mailboxId, int tagId, byte color) {
		setMailboxId(mailboxId);
		mTagId = tagId;
		mColor = color;
	}

	public int getOpCode() {
		return OP_COLOR_TAG;
	}

	protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("tag=");
        sb.append(mTagId).append(", color=").append(mColor);
        return sb.toString();
	}

	protected void serializeData(DataOutput out) throws IOException {
		out.writeInt(mTagId);
		out.writeByte(mColor);
	}

	protected void deserializeData(DataInput in) throws IOException {
		mTagId = in.readInt();
		mColor = in.readByte();
	}

	public void redo() throws Exception {
		int mboxId = getMailboxId();
		Mailbox mbox = Mailbox.getMailboxById(mboxId);
        mbox.colorTag(getOperationContext(), mTagId, mColor);
	}
}
