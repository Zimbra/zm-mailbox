package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class PurgeSearchHistory extends RedoableOp {

    protected PurgeSearchHistory() {
        super(MailboxOperation.PurgeSearchHistory);
    }

    public PurgeSearchHistory(int mailboxId) {
        this();
        setMailboxId(mailboxId);
    }
    @Override
    public void redo() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        mbox.purgeSearchHistory(getOperationContext());
    }

    @Override
    protected String getPrintableData() {
        // no printable members
        return null;
    }

    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        // nothing to serialize
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        //nothing to deserialize
    }

}
