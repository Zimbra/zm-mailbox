/*
***** BEGIN LICENSE BLOCK *****
Version: ZPL 1.1

The contents of this file are subject to the Zimbra Public License
Version 1.1 ("License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.zimbra.com/license

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
the License for the specific language governing rights and limitations
under the License.

The Original Code is: Zimbra Collaboration Suite.

The Initial Developer of the Original Code is Zimbra, Inc.  Portions
created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
Reserved.

Contributor(s): 

***** END LICENSE BLOCK *****
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
public class ModifySavedSearch extends RedoableOp {

	private int mSearchId;
    private String mQuery;
    private String mTypes;
    private String mSort;

	public ModifySavedSearch() {
		mSearchId = UNKNOWN_ID;
	}

	public ModifySavedSearch(int mailboxId, int searchId, String query, String types, String sort) {
		setMailboxId(mailboxId);
		mSearchId = searchId;
        mQuery = query != null ? query : "";
        mTypes = types != null ? types : "";
        mSort = sort != null ? sort : "";
	}

	public int getOpCode() {
		return OP_MODIFY_SAVED_SEARCH;
	}

	protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=");
        sb.append(mSearchId).append(", query=").append(mQuery);
        sb.append(", types=").append(mTypes).append(", sort=").append(mSort);
        return sb.toString();
	}

	protected void serializeData(DataOutput out) throws IOException {
		out.writeInt(mSearchId);
        writeUTF8(out, mQuery);
        writeUTF8(out, mTypes);
        writeUTF8(out, mSort);
	}

	protected void deserializeData(DataInput in) throws IOException {
		mSearchId = in.readInt();
        mQuery = readUTF8(in);
        mTypes = readUTF8(in);
        mSort = readUTF8(in);
	}

	public void redo() throws Exception {
		int mboxId = getMailboxId();
		Mailbox mailbox = Mailbox.getMailboxById(mboxId);
    	mailbox.modifySearchFolder(getOperationContext(), mSearchId, mQuery, mTypes, mSort);
	}
}
