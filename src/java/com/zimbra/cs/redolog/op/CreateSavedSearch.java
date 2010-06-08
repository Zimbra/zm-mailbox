/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on 2004. 7. 21.
 */
package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class CreateSavedSearch extends RedoableOp {

    private int mSearchId;
    private String mName;
    private String mQuery;
    private String mTypes;
    private String mSort;
    private int mFolderId;
    private int mFlags;
    private long mColor;

    public CreateSavedSearch() {
        mSearchId = UNKNOWN_ID;
    }

    public CreateSavedSearch(long mailboxId, int folderId, String name,
        String query, String types, String sort, int flags, MailItem.Color color) {
        setMailboxId(mailboxId);
        mSearchId = UNKNOWN_ID;
        mName = name != null ? name : "";
        mQuery = query != null ? query : "";
        mTypes = types != null ? query : "";
        mSort = sort != null ? query : "";
        mFolderId = folderId;
        mFlags = flags;
        mColor = color.getValue();
    }

    public int getSearchId() {
        return mSearchId;
    }

    public void setSearchId(int searchId) {
        mSearchId = searchId;
    }

    @Override public int getOpCode() {
        return OP_CREATE_SAVED_SEARCH;
    }

    @Override protected String getPrintableData() {
        StringBuilder sb = new StringBuilder("id=").append(mSearchId);
        sb.append(", name=").append(mName).append(", query=").append(mQuery);
        sb.append(", types=").append(mTypes).append(", sort=").append(mSort);
        sb.append(", flags=").append(mFlags);
        sb.append(", color=").append(mColor);
        return sb.toString();
    }

    @Override protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mSearchId);
        out.writeUTF(mName);
        out.writeUTF(mQuery);
        out.writeUTF(mTypes);
        out.writeUTF(mSort);
        out.writeInt(mFolderId);
        out.writeInt(mFlags);
        // mColor from byte to long in Version 1.27
        out.writeLong(mColor);
    }

    @Override protected void deserializeData(RedoLogInput in) throws IOException {
        mSearchId = in.readInt();
        mName = in.readUTF();
        mQuery = in.readUTF();
        mTypes = in.readUTF();
        mSort = in.readUTF();
        mFolderId = in.readInt();
        mFlags = getVersion().atLeast(1, 28) ? in.readInt() : 0;
        if (getVersion().atLeast(1, 27))
            mColor = in.readLong();
        else
            mColor = in.readByte();
    }

    @Override public void redo() throws Exception {
        long mboxId = getMailboxId();
        Mailbox mailbox = MailboxManager.getInstance().getMailboxById(mboxId);

        try {
            mailbox.createSearchFolder(getOperationContext(), mFolderId, mName,
                mQuery, mTypes, mSort, mFlags, MailItem.Color.fromMetadata(mColor));
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
