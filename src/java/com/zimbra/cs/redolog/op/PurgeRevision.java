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

import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class PurgeRevision extends RedoableOp {

    protected int mId;
    protected int mRev;
    protected boolean mIncludeOlderRevisions;

    public PurgeRevision() { }

    public PurgeRevision(long mailboxId, int id, int rev, boolean includeOlderRevisions) {
        setMailboxId(mailboxId);
        mId = id;
        mRev = rev;
        mIncludeOlderRevisions = includeOlderRevisions;
    }

    @Override public int getOpCode() {
        return OP_PURGE_REVISION;
    }

    @Override protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=").append(mId);
        sb.append(", rev=").append(mRev);
        sb.append(", includeOlderRevisions=").append(mIncludeOlderRevisions);
        return sb.toString();
    }

    @Override protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mId);
        out.writeInt(mRev);
        out.writeBoolean(mIncludeOlderRevisions);
    }

    @Override protected void deserializeData(RedoLogInput in) throws IOException {
        mId = in.readInt();
        mRev = in.readInt();
        mIncludeOlderRevisions = in.readBoolean();
    }

    @Override public void redo() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        mbox.purgeRevision(getOperationContext(), mId, mRev, mIncludeOlderRevisions);
    }
}
