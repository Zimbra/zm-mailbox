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
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
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

import java.io.IOException;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

/**
 * @author jhahm
 */
public class RenameTag extends RenameItem {

	public RenameTag() {
		super();
        mType = MailItem.TYPE_TAG;
        mFolderId = Mailbox.ID_FOLDER_TAGS;
	}

	public RenameTag(int mailboxId, int tagId, String name) {
        super(mailboxId, tagId, MailItem.TYPE_TAG, name, Mailbox.ID_FOLDER_TAGS);
	}

	public int getOpCode() {
		return OP_RENAME_TAG;
	}

	protected void serializeData(RedoLogOutput out) throws IOException {
		out.writeInt(mId);
		out.writeUTF(mName);
	}

	protected void deserializeData(RedoLogInput in) throws IOException {
		mId = in.readInt();
		mName = in.readUTF();
	}
}
