/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
    private String mUuid;
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

    public String getUuid() {
        return mUuid;
    }

    public void setIdAndUuid(int id, String uuid) {
        mId = id;
        mUuid = uuid;
    }

    @Override
    protected String getPrintableData() {
        StringBuilder sb = new StringBuilder("id=").append(mId);
        sb.append(", uuid=").append(mUuid);
        sb.append(", name=").append(mName).append(", folder=").append(mFolderId);
        sb.append(", owner=").append(mOwnerId).append(", remote=").append(mRemoteId);
        return sb.toString();
    }

    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mId);
        if (getVersion().atLeast(1, 37)) {
            out.writeUTF(mUuid);
        }
        out.writeUTF(mName);
        out.writeUTF(mOwnerId);
        out.writeInt(mRemoteId);
        out.writeInt(mFolderId);
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        mId = in.readInt();
        if (getVersion().atLeast(1, 37)) {
            mUuid = in.readUTF();
        }
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
