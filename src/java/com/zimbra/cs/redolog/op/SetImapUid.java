/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class SetImapUid extends RedoableOp {

    private Map<Integer, Integer> mImapUids = new HashMap<Integer, Integer>();

    public SetImapUid() {
        super(MailboxOperation.SetImapUid);
    }

    public SetImapUid(int mailboxId, List<Integer> msgIds) {
        this();
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
