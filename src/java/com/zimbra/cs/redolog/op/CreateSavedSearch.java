/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
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

import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

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
    private byte mColor;

    public CreateSavedSearch() {
        mSearchId = UNKNOWN_ID;
    }

    public CreateSavedSearch(int mailboxId, int folderId, String name, String query, String types, String sort, byte color) {
        setMailboxId(mailboxId);
        mSearchId = UNKNOWN_ID;
        mName = name != null ? name : "";
        mQuery = query != null ? query : "";
        mTypes = types != null ? query : "";
        mSort = sort != null ? query : "";
        mFolderId = folderId;
        mColor = color;
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
        StringBuilder sb = new StringBuilder("id=").append(mSearchId);
        sb.append(", name=").append(mName).append(", query=").append(mQuery);
        sb.append(", types=").append(mTypes).append(", sort=").append(mSort);
        sb.append(", color=").append(mColor);
        return sb.toString();
    }

    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mSearchId);
        out.writeUTF(mName);
        out.writeUTF(mQuery);
        out.writeUTF(mTypes);
        out.writeUTF(mSort);
        out.writeInt(mFolderId);
        out.writeByte(mColor);
    }

    protected void deserializeData(RedoLogInput in) throws IOException {
        mSearchId = in.readInt();
        mName = in.readUTF();
        mQuery = in.readUTF();
        mTypes = in.readUTF();
        mSort = in.readUTF();
        mFolderId = in.readInt();
        mColor = in.readByte();
    }

    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mailbox = MailboxManager.getInstance().getMailboxById(mboxId);
        try {
            mailbox.createSearchFolder(getOperationContext(), mFolderId, mName, mQuery, mTypes, mSort, mColor);
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
