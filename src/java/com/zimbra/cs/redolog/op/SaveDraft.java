/*
 * Created on Jun 14, 2005
 */
package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mime.ParsedMessage;

/**
 * @author dkarp
 */
public class SaveDraft extends CreateMessage {

    private int mImapId;           // new IMAP id for this message

    public SaveDraft() {
    }

    public SaveDraft(int mailboxId, int draftId, String digest, int msgSize) {
        super(mailboxId, ":API:", false, digest, msgSize, -1, 0, null);
        setMessageId(draftId);
    }

    public int getImapId() {
        return mImapId;
    }

    public void setImapId(int imapId) {
        mImapId = imapId;
    }

    protected String getPrintableData() {
        return super.getPrintableData() + ",imap=" + mImapId;
    }

    public int getOpCode() {
        return OP_SAVE_DRAFT;
    }

    protected void serializeData(DataOutput out) throws IOException {
        out.writeInt(mImapId);
        super.serializeData(out);
    }

    protected void deserializeData(DataInput in) throws IOException {
        mImapId = in.readInt();
        super.deserializeData(in);
    }

    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mbox = Mailbox.getMailboxById(mboxId);

        ParsedMessage pm = new ParsedMessage(getMessageBody(), getTimestamp(), mbox.attachmentsIndexingEnabled());
        mbox.saveDraft(getOperationContext(), pm, getMessageId(), 0, null);
    }
}
