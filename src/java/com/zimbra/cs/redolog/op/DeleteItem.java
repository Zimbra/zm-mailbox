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
package com.zimbra.cs.redolog.op;

import java.io.IOException;
import java.util.Arrays;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.TargetConstraint;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

/**
 * @since 2004. 7. 21.
 */
public class DeleteItem extends RedoableOp {

    private int[] mIds;
    private MailItem.Type type;
    private String mConstraint;

    public DeleteItem() {
        super(MailboxOperation.DeleteItem);
        type = MailItem.Type.UNKNOWN;
        mConstraint = null;
    }

    public DeleteItem(int mailboxId, int[] ids, MailItem.Type type, TargetConstraint tcon) {
        this();
        setMailboxId(mailboxId);
        mIds = ids;
        this.type = type;
        mConstraint = (tcon == null ? null : tcon.toString());
    }

    public DeleteItem(int mailboxId, MailItem.Type type, TargetConstraint tcon) {
        this(mailboxId, null, type, tcon);
    }

    public void setIds(int[] ids) {
        mIds = ids;
    }

    @Override
    protected String getPrintableData() {
        StringBuilder sb = new StringBuilder("ids=");
        sb.append(Arrays.toString(mIds)).append(", type=").append(type);
        if (mConstraint != null) {
            sb.append(", constraint=").append(mConstraint);
        }
        return sb.toString();
    }

    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(-1);
        out.writeByte(type.toByte());
        boolean hasConstraint = mConstraint != null;
        out.writeBoolean(hasConstraint);
        if (hasConstraint) {
            out.writeUTF(mConstraint);
        }
        out.writeInt(mIds.length);
        for (int id : mIds) {
            out.writeInt(id);
        }
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        int id = in.readInt();
        if (id > 0) {
            mIds = new int[] { id };
        }
        type = MailItem.Type.of(in.readByte());
        if (in.readBoolean()) {
            mConstraint = in.readUTF();
        }
        if (id <= 0) {
            mIds = new int[in.readInt()];
            for (int i = 0; i < mIds.length; i++) {
                mIds[i] = in.readInt();
            }
        }
    }

    @Override
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

            try {
                mbox.delete(getOperationContext(), mIds, type, tcon);
            } catch (MailServiceException.NoSuchItemException e) {
                if (mLog.isInfoEnabled()) {
                    mLog.info("Some of the items being deleted were already deleted from mailbox " + mboxId);
                }
            }
    }

    @Override
    public boolean isDeleteOp() {
        return true;
    }
}
