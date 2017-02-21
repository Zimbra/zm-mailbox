/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

/**
 * @since 2004. 12. 13.
 */
public class RenameItemPath extends RedoableOp {

    protected int mId;
    protected MailItem.Type type;
    protected String mPath;
    protected int mParentIds[];
    protected String mParentUuids[];

    public RenameItemPath() {
        super(MailboxOperation.RenameItemPath);
        mId = UNKNOWN_ID;
        type = MailItem.Type.UNKNOWN;
    }

    public RenameItemPath(int mailboxId, int id, MailItem.Type type, String path) {
        this();
        setMailboxId(mailboxId);
        mId = id;
        this.type = type;
        mPath = path != null ? path : "";
    }

    public int[] getParentIds() {
        return mParentIds;
    }

    public String[] getParentUuids() {
        return mParentUuids;
    }

    public void setParentIdsAndUuids(int parentIds[], String parentUuids[]) {
        mParentIds = parentIds;
        mParentUuids = parentUuids;
    }

    @Override
    protected String getPrintableData() {
        StringBuilder sb = new StringBuilder("id=");
        sb.append(mId).append(", type=").append(type).append(", path=").append(mPath);
        if (mParentIds != null) {
            sb.append(", destParentIdsAndUuids=[");
            for (int i = 0; i < mParentIds.length; i++) {
                sb.append(mParentIds[i]).append(" (").append(mParentUuids[i]).append(")");
                if (i < mParentIds.length - 1) {
                    sb.append(", ");
                }
            }
            sb.append("]");
        }
        return sb.toString();
    }

    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mId);
        out.writeUTF(mPath);
        if (mParentIds != null) {
            out.writeInt(mParentIds.length);
            for (int i = 0; i < mParentIds.length; i++) {
                out.writeInt(mParentIds[i]);
                if (getVersion().atLeast(1, 37)) {
                    out.writeUTF(mParentUuids[i]);
                }
            }
        } else {
            out.writeInt(0);
        }
        out.writeByte(type.toByte());
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        mId = in.readInt();
        mPath = in.readUTF();
        int numParentIds = in.readInt();
        if (numParentIds > 0) {
            mParentIds = new int[numParentIds];
            mParentUuids = new String[numParentIds];
            for (int i = 0; i < numParentIds; i++) {
                mParentIds[i] = in.readInt();
                if (getVersion().atLeast(1, 37)) {
                    mParentUuids[i] = in.readUTF();
                }
            }
        }
        type = MailItem.Type.of(in.readByte());
    }

    @Override
    public void redo() throws Exception {
        Mailbox mailbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        mailbox.rename(getOperationContext(), mId, type, mPath);
    }
}
