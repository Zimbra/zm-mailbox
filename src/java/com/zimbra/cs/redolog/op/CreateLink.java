/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class CreateLink extends RedoableOp {

    private int mId;
    private int mFolderId;
    private String mName;
    private String mOwnerId;
    private int mRemoteId;

    public CreateLink() {
        super(MailboxOperation.CreateMountpoint);
        mId = UNKNOWN_ID;
    }

    public CreateLink(int mailboxId, int folderId, String name, String ownerId, int remoteId) {
        this();
        setMailboxId(mailboxId);
        mId = UNKNOWN_ID;
        mFolderId = folderId;
        mName = name != null ? name : "";
        mOwnerId = ownerId;
        mRemoteId = remoteId;
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        mId = id;
    }

    @Override
    protected String getPrintableData() {
        StringBuilder sb = new StringBuilder("id=").append(mId);
        sb.append(", name=").append(mName).append(", folder=").append(mFolderId);
        sb.append(", owner=").append(mOwnerId).append(", remote=").append(mRemoteId);
        return sb.toString();
    }

    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mId);
        out.writeUTF(mName);
        out.writeUTF(mOwnerId);
        out.writeInt(mRemoteId);
        out.writeInt(mFolderId);
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        mId = in.readInt();
        mName = in.readUTF();
        mOwnerId = in.readUTF();
        mRemoteId = in.readInt();
        mFolderId = in.readInt();
    }

    @Override
    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mailbox = MailboxManager.getInstance().getMailboxById(mboxId);

        try {
            mailbox.createLink(getOperationContext(), mFolderId, mName, mOwnerId, mRemoteId);
        } catch (MailServiceException e) {
            if (e.getCode() == MailServiceException.ALREADY_EXISTS) {
                if (mLog.isInfoEnabled()) {
                    mLog.info("Link " + mId + " already exists in mailbox " + mboxId);
                }
            } else {
                throw e;
            }
        }
    }
}
