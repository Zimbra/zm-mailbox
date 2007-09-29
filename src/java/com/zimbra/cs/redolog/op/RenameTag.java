/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on 2004. 7. 21.
 */
package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

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

	protected void serializeData(RedoLogOutput out) throws IOException {
		out.writeInt(mTagId);
		out.writeUTF(mName);
	}

	protected void deserializeData(RedoLogInput in) throws IOException {
		mTagId = in.readInt();
		mName = in.readUTF();
	}

	public void redo() throws Exception {
		int mboxId = getMailboxId();
		Mailbox mbox = MailboxManager.getInstance().getMailboxById(mboxId);
		mbox.renameTag(getOperationContext(), mTagId, mName);
	}
}
