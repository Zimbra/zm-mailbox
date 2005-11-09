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

import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;


/**
 * @author jhahm
 */
public class CreateSavedSearch extends RedoableOp {

	private int mSearchId;
	private String mName;
    private String mQuery;
    private String mTypes;
    private String mSort;
	private int mFolderId;

	public CreateSavedSearch() {
		mSearchId = UNKNOWN_ID;
	}

	public CreateSavedSearch(int mailboxId, int folderId, String name, String query, String types, String sort) {
		setMailboxId(mailboxId);
		mSearchId = UNKNOWN_ID;
		mName = name != null ? name : "";
        mQuery = query != null ? query : "";
        mTypes = types != null ? query : "";
        mSort = sort != null ? query : "";
		mFolderId = folderId;
	}

	public int getSearchId() {
		return mSearchId;
	}

	public void setSearchId(int searchId) {
		mSearchId = searchId;
	}

	public int getOpCode() {
		return OP_CREATE_SAVED_SEARCH;
	}

	protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=").append(mSearchId);
        sb.append(", name=").append(mName).append(", query=").append(mQuery);
        sb.append(", types=").append(mTypes).append(", sort=").append(mSort);
        return sb.toString();
	}

	protected void serializeData(DataOutput out) throws IOException {
		out.writeInt(mSearchId);
		writeUTF8(out, mName);
        writeUTF8(out, mQuery);
        writeUTF8(out, mTypes);
        writeUTF8(out, mSort);
		out.writeInt(mFolderId);
	}

	protected void deserializeData(DataInput in) throws IOException {
		mSearchId = in.readInt();
		mName = readUTF8(in);
        mQuery = readUTF8(in);
        mTypes = readUTF8(in);
        mSort = readUTF8(in);
		mFolderId = in.readInt();
	}

	public void redo() throws Exception {
		int mboxId = getMailboxId();
    	Mailbox mailbox = Mailbox.getMailboxById(mboxId);
        try {
            mailbox.createSearchFolder(getOperationContext(), mFolderId, mName, mQuery, mTypes, mSort);
        } catch (MailServiceException e) {
            String code = e.getCode();
            if (code.equals(MailServiceException.ALREADY_EXISTS)) {
                if (mLog.isInfoEnabled())
                    mLog.info("Search " + mSearchId + " already exists in mailbox " + mboxId);
            } else
                throw e;
        }
	}
}
