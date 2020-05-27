/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
 * Created on Jun 14, 2005
 */
package com.zimbra.cs.redolog.op;


import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.StoreManager;

public class SaveDraft extends CreateMessage {

    private int mImapId;           // new IMAP id for this message

    public SaveDraft()  {
        mOperation = MailboxOperation.SaveDraft;
    }

    public SaveDraft(int mailboxId, int draftId, String digest, int msgSize) {
        super(mailboxId, ":API:", false, digest, msgSize, -1, true, 0, null);
        mOperation = MailboxOperation.SaveDraft;
        setMessageId(draftId);
    }

    public int getImapId() {
        return mImapId;
    }

    public void setImapId(int imapId) {
        mImapId = imapId;
    }

    @Override protected String getPrintableData() {
        return super.getPrintableData() + ",imap=" + mImapId;
    }

    @Override protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mImapId);
        super.serializeData(out);
    }

    @Override protected void deserializeData(RedoLogInput in) throws IOException {
        mImapId = in.readInt();
        super.deserializeData(in);
    }

    @Override public void redo() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());

        StoreManager sm = StoreManager.getInstance();
        Blob blob = null;
        InputStream in = null;
        try {
            if (mMsgBodyType == MSGBODY_EXTERNAL) {
                blob = getExternalBlob();
            } else {
                in = mData.getInputStream();
                if (mData.getLength() != mMsgSize) {
                    in = new GZIPInputStream(in);
                }
                blob = sm.storeIncoming(in);
            }
            ParsedMessage pm = new ParsedMessage(blob.getFile(), getTimestamp(), mbox.attachmentsIndexingEnabled());

            mbox.saveDraft(getOperationContext(), pm, getMessageId());
        } finally {
            ByteUtil.closeStream(in);
            sm.quietDelete(blob);
        }
    }
}
