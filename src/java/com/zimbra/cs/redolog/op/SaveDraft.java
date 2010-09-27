/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
 * Created on Jun 14, 2005
 */
package com.zimbra.cs.redolog.op;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.StoreManager;

public class SaveDraft extends CreateMessage {

    private int mImapId;           // new IMAP id for this message

    public SaveDraft()  { }

    public SaveDraft(long mailboxId, int draftId, String digest, int msgSize) {
        super(mailboxId, ":API:", false, digest, msgSize, -1, true, 0, null);
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

    @Override public int getOpCode() {
        return OP_SAVE_DRAFT;
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
            in = mData.getInputStream();
            if (mData.getLength() != mMsgSize)
                in = new GZIPInputStream(in);

            blob = sm.storeIncoming(in, null);
            ParsedMessage pm = new ParsedMessage(blob.getFile(), getTimestamp(), mbox.attachmentsIndexingEnabled());

            mbox.saveDraft(getOperationContext(), pm, getMessageId());
        } finally {
            ByteUtil.closeStream(in);
            sm.quietDelete(blob);
        }
    }
}
