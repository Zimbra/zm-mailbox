/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class DeleteItemFromDumpster extends RedoableOp {

    private int[] mIds;

    public DeleteItemFromDumpster() {
    }

    public DeleteItemFromDumpster(long mailboxId, int[] ids) {
        setMailboxId(mailboxId);
        mIds = ids;
    }

    @Override public int getOpCode() {
        return OP_DELETE_ITEM_FROM_DUMPSTER;
    }

    @Override protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("ids=");
        sb.append(Arrays.toString(mIds));
        return sb.toString();
    }

    @Override protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mIds.length);
        for (int id : mIds)
            out.writeInt(id);
    }

    @Override protected void deserializeData(RedoLogInput in) throws IOException {
        mIds = new int[in.readInt()];
        for (int i = 0; i < mIds.length; i++)
            mIds[i] = in.readInt();
    }

    @Override public void redo() throws Exception {
        long mboxId = getMailboxId();
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mboxId);

        try {
            mbox.deleteFromDumpster(getOperationContext(), mIds);
        } catch (MailServiceException.NoSuchItemException e) {
            if (mLog.isInfoEnabled())
                mLog.info("Some of the items being deleted were already deleted from dumpster " + mboxId);
        }
    }

    @Override public boolean isDeleteOp() {
        return true;
    }
}
