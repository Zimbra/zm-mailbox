/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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
 * Created on Jul 24, 2005
 */
package com.zimbra.cs.redolog.op;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class SetImapUid extends RedoableOp {

    private Map<Integer, Integer> mImapUids = new HashMap<Integer, Integer>();

    public SetImapUid() {
    }

    public SetImapUid(int mailboxId, List<Integer> msgIds) {
        setMailboxId(mailboxId);
        for (int id : msgIds)
            mImapUids.put(id, UNKNOWN_ID);
    }

    public int getImapUid(int msgId) {
        Integer imapUid = mImapUids.get(msgId);
        int uid = imapUid == null ? Mailbox.ID_AUTO_INCREMENT : imapUid;
        return uid == UNKNOWN_ID ? Mailbox.ID_AUTO_INCREMENT : uid;
    }

    public void setImapUid(int msgId, int imapId) {
        mImapUids.put(msgId, imapId);
    }

    @Override public int getOpCode() {
        return OP_SET_IMAP_UID;
    }

    @Override protected String getPrintableData() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, Integer> entry : mImapUids.entrySet())
            sb.append(sb.length() == 0 ? "" : ", ").append(entry.getKey()).append('=').append(entry.getValue());
        return sb.toString();
    }

    @Override protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mImapUids.size());
        for (Map.Entry<Integer, Integer> entry : mImapUids.entrySet()) {
            out.writeInt(entry.getKey());
            out.writeInt(entry.getValue());
        }
    }

    @Override protected void deserializeData(RedoLogInput in) throws IOException {
        int count = in.readInt();
        for (int i = 0; i < count; i++) {
            int msgId = in.readInt();
            mImapUids.put(msgId, in.readInt());
        }
    }

    @Override public void redo() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        mbox.resetImapUid(getOperationContext(), new ArrayList<Integer>(mImapUids.keySet()));
    }
}
