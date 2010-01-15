/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2010 Zimbra, Inc.
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
import java.util.Arrays;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailItem.TargetConstraint;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

/**
 * @author jhahm
 */
public class MoveItem extends RedoableOp {

	private int[] mIds;
    private byte mType;
	private int mDestId;
    private String mConstraint;
    private int mUIDNEXT = Mailbox.ID_AUTO_INCREMENT;

	public MoveItem() {
        mType = MailItem.TYPE_UNKNOWN;
		mDestId = 0;
        mConstraint = null;
	}

	public MoveItem(int mailboxId, int[] ids, byte type, int destId, TargetConstraint tcon) {
		setMailboxId(mailboxId);
		mIds = ids;
        mType = type;
		mDestId = destId;
        mConstraint = (tcon == null ? null : tcon.toString());
	}

    public void setUIDNEXT(int uidnext) {
        mUIDNEXT = (uidnext > 0 ? uidnext : Mailbox.ID_AUTO_INCREMENT);
    }

    public int getUIDNEXT() {
        return mUIDNEXT;
    }

	public int getOpCode() {
		return OP_MOVE_ITEM;
	}

	protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=");
        sb.append(Arrays.toString(mIds)).append(", type=").append(mType);
        sb.append(", dest=").append(mDestId);
        if (mConstraint != null)
            sb.append(", constraint=").append(mConstraint);
        if (mUIDNEXT != Mailbox.ID_AUTO_INCREMENT)
            sb.append(", uidnext=").append(mUIDNEXT);
        return sb.toString();
	}

	protected void serializeData(RedoLogOutput out) throws IOException {
		out.writeInt(-1);
        out.writeByte(mType);
		out.writeInt(mDestId);
        boolean hasConstraint = mConstraint != null;
        out.writeBoolean(hasConstraint);
        if (hasConstraint)
            out.writeUTF(mConstraint);
        out.writeInt(mIds == null ? 0 : mIds.length);
        if (mIds != null)
            for (int i = 0; i < mIds.length; i++)
                out.writeInt(mIds[i]);
        if (getVersion().atLeast(1, 16))
            out.writeInt(mUIDNEXT);
	}

	protected void deserializeData(RedoLogInput in) throws IOException {
        int id = in.readInt();
        if (id > 0)
            mIds = new int[] { id };
        mType = in.readByte();
		mDestId = in.readInt();
        if (in.readBoolean())
            mConstraint = in.readUTF();
        if (id <= 0) {
            mIds = new int[in.readInt()];
            for (int i = 0; i < mIds.length; i++)
                mIds[i] = in.readInt();
        }
        if (getVersion().atLeast(1, 16))
            mUIDNEXT = in.readInt();
	}

	public void redo() throws Exception {
		int mboxId = getMailboxId();
		Mailbox mbox = MailboxManager.getInstance().getMailboxById(mboxId);

        TargetConstraint tcon = null;
        if (mConstraint != null)
            try {
                tcon = TargetConstraint.parseConstraint(mbox, mConstraint);
            } catch (ServiceException e) {
                mLog.warn(e);
            }

        // No extra checking needed because Mailbox.move() is already idempotent.
        mbox.move(getOperationContext(), mIds, mType, mDestId, tcon);
	}
}
