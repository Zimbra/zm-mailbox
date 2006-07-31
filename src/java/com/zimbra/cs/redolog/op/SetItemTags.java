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
 * Created on 2004. 9. 13.
 */
package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.TargetConstraint;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;

/**
 * @author jhahm
 */
public class SetItemTags extends RedoableOp {

	private int[] mIds;
	private byte mType;
	private int mFlags;
    private long mTags;
    private String mConstraint;

	public SetItemTags() {
		mType = MailItem.TYPE_UNKNOWN;
        mConstraint = null;
	}

	public SetItemTags(int mailboxId, int[] itemIds, byte itemType, int flags, long tags, TargetConstraint tcon) {
		setMailboxId(mailboxId);
		mIds = itemIds;
		mType = itemType;
		mFlags = flags;
        mTags = tags;
        mConstraint = (tcon == null ? null : tcon.toString());
	}

	public int getOpCode() {
		return OP_SET_ITEM_TAGS;
	}

	protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("ids=");
        sb.append(Arrays.toString(mIds)).append(", type=").append(mType);
        sb.append(", flags=[").append(mFlags);
        sb.append("], tags=[").append(mTags).append("]");
        if (mConstraint != null)
            sb.append(", constraint=").append(mConstraint);
        return sb.toString();
	}

	protected void serializeData(DataOutput out) throws IOException {
		out.writeInt(-1);
		out.writeByte(mType);
        out.writeInt(mFlags);
        out.writeLong(mTags);
        boolean hasConstraint = mConstraint != null;
        out.writeBoolean(hasConstraint);
        if (hasConstraint)
            out.writeUTF(mConstraint);
        out.writeInt(mIds == null ? 0 : mIds.length);
        if (mIds != null)
            for (int i = 0; i < mIds.length; i++)
                out.writeInt(mIds[i]);
	}

	protected void deserializeData(DataInput in) throws IOException {
        int id = in.readInt();
        if (id > 0)
            mIds = new int[] { id };
		mType = in.readByte();
        mFlags = in.readInt();
        mTags = in.readLong();
        if (in.readBoolean())
            mConstraint = in.readUTF();
        if (id <= 0) {
            mIds = new int[in.readInt()];
            for (int i = 0; i < mIds.length; i++)
                mIds[i] = in.readInt();
        }
	}

	public void redo() throws Exception {
		int mboxId = getMailboxId();
		Mailbox mbox = Mailbox.getMailboxById(mboxId);

        TargetConstraint tcon = null;
        if (mConstraint != null)
            try {
                tcon = TargetConstraint.parseConstraint(mbox, mConstraint);
            } catch (ServiceException e) {
                mLog.warn(e);
            }

		mbox.setTags(getOperationContext(), mIds, mType, mFlags, mTags, tcon);
	}
}
