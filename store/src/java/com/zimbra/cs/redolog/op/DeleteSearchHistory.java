package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class DeleteSearchHistory extends RedoableOp {

    protected DeleteSearchHistory() {
        super(MailboxOperation.DeleteSearchHistory);
    }

    public DeleteSearchHistory(int mailboxId) {
        this();
        setMailboxId(mailboxId);
    }

    @Override
    public void redo() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        mbox.deleteSearchHistory(getOperationContext());
    }

    @Override
    protected String getPrintableData() {
        // no printable data
        return null;
    }

    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        // no members to serialize
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        // no members to deserialize
    }

}
