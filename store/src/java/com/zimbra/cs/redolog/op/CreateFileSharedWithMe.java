/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2022 Synacor, Inc.
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

import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class CreateFileSharedWithMe extends RedoableOp {

    private int mId;
    private String mUuid;
    private int mFolderId;
    private String mName;
    private String mOwnerId;
    private int mRemoteId;
    private String mRemoteUuid;
    private String mOwnerName;
    private String mContentType;
    private String mRights;

    public CreateFileSharedWithMe() {
        super(MailboxOperation.CreateMountpoint);
        mId = UNKNOWN_ID;
    }

    public CreateFileSharedWithMe(int mailboxId, int folderId, String name, String ownerId, int remoteId,
            String remoteUuid, String fileOwnerName, String contetnType, String rights) {
        this();
        setMailboxId(mailboxId);
        mId = UNKNOWN_ID;
        mFolderId = folderId;
        mName = name != null ? name : "";
        mOwnerId = ownerId;
        mRemoteId = remoteId;
        mRemoteUuid = remoteUuid;
        mOwnerName = fileOwnerName;
        mContentType = contetnType;
        mRights = rights;
    }

    public CreateFileSharedWithMe(int mailboxId, String remoteUuid) {
        this();
        setMailboxId(mailboxId);
        mRemoteUuid = remoteUuid;
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
        sb.append(", owner=").append(mOwnerId).append(", remoteId=").append(mRemoteId).append(", remoteUuid=")
                .append(mRemoteUuid);
        return sb.toString();
    }

    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mId);
        out.writeUTF(mUuid);
        out.writeUTF(mName);
        out.writeUTF(mOwnerId);
        out.writeInt(mRemoteId);
        out.writeUTF(mRemoteUuid);
        out.writeInt(mFolderId);
        out.writeUTF(mOwnerName);
        out.writeUTF(mContentType);
        out.writeUTF(mRights);
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        mId = in.readInt();
        mUuid = in.readUTF();
        mName = in.readUTF();
        mOwnerId = in.readUTF();
        mOwnerName = in.readUTF();
        mRemoteId = in.readInt();
        mRemoteUuid = in.readUTF();
        mFolderId = in.readInt();
        mContentType = in.readUTF();
        mRights = in.readUTF();
    }

    @Override
    public void redo() throws Exception {
        // TODO Auto-generated method stub
    }
}
