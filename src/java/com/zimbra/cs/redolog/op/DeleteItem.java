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

import java.io.IOException;
import java.util.Arrays;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.TargetConstraint;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;
import com.zimbra.cs.service.ServiceException;

/**
 * @author jhahm
 */
public class DeleteItem extends RedoableOp {

	private int[] mIds;
    private byte mType;
    private String mConstraint;

	public DeleteItem() {
        mType = MailItem.TYPE_UNKNOWN;
        mConstraint = null;
	}

	public DeleteItem(int mailboxId, int[] ids, byte type, TargetConstraint tcon) {
		setMailboxId(mailboxId);
		mIds = ids;
        mType = type;
        mConstraint = (tcon == null ? null : tcon.toString());
	}

	public int getOpCode() {
		return OP_DELETE_ITEM;
	}

	protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("ids=");
        sb.append(Arrays.toString(mIds)).append(", type=").append(mType);
        if (mConstraint != null)
            sb.append(", constraint=").append(mConstraint);
        return sb.toString();
	}

	protected void serializeData(RedoLogOutput out) throws IOException {
		out.writeInt(-1);
        out.writeByte(mType);
        boolean hasConstraint = mConstraint != null;
        out.writeBoolean(hasConstraint);
        if (hasConstraint)
            out.writeUTF(mConstraint);
        out.writeInt(mIds.length);
        for (int id : mIds)
            out.writeInt(id);
	}

	protected void deserializeData(RedoLogInput in) throws IOException {
		int id = in.readInt();
        if (id > 0)
            mIds = new int[] { id };
        mType = in.readByte();
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

        try {
    		mbox.delete(getOperationContext(), mIds, mType, tcon);
        } catch (MailServiceException.NoSuchItemException e) {
            if (mLog.isInfoEnabled())
                mLog.info("Item " + mIds + " was already deleted from mailbox " + mboxId);
        }
	}
}
