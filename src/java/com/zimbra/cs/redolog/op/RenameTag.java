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
public class RenameTag extends RedoableOp {

	private int mTagId;
	private String mName;

	public RenameTag() {
		mTagId = UNKNOWN_ID;
	}

	public RenameTag(int mailboxId, int tagId, String name) {
		setMailboxId(mailboxId);
		mTagId = tagId;
		mName = name != null ? name : "";
	}

	public int getOpCode() {
		return OP_RENAME_TAG;
	}

	protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=");
        sb.append(mTagId).append(", name=").append(mName);
        return sb.toString();
	}

	protected void serializeData(DataOutput out) throws IOException {
		out.writeInt(mTagId);
		writeUTF8(out, mName);
	}

	protected void deserializeData(DataInput in) throws IOException {
		mTagId = in.readInt();
		mName = readUTF8(in);
	}

	public void redo() throws Exception {
		int mboxId = getMailboxId();
		Mailbox mbox = Mailbox.getMailboxById(mboxId);
		mbox.renameTag(getOperationContext(), mTagId, mName);
	}
}
