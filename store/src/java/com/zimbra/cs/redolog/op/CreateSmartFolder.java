package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class CreateSmartFolder extends RedoableOp {

    private int mId;
    private String mName;

    public CreateSmartFolder() {
        super(MailboxOperation.CreateSmartFolder);
        mId = UNKNOWN_ID;
    }

    public CreateSmartFolder(int mailboxId, String name) {
        this();
        setMailboxId(mailboxId);
        mId = UNKNOWN_ID;
        mName = name != null ? name : "";
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        mId = id;
    }

    @Override protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=").append(mId);
        sb.append(", name=").append(mName);
        return sb.toString();
    }

    @Override protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mId);
        out.writeUTF(mName);
    }

    @Override protected void deserializeData(RedoLogInput in) throws IOException {
        mId = in.readInt();
        mName = in.readUTF();
    }

    @Override public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mboxId);

        try {
            mbox.createSmartFolder(getOperationContext(), mName);
        } catch (MailServiceException e) {
            String code = e.getCode();
            if (code.equals(MailServiceException.ALREADY_EXISTS)) {
                if (mLog.isInfoEnabled())
                    mLog.info("SmartFolder " + mId + " already exists in mailbox " + mboxId);
            } else {
                throw e;
            }
        }
    }
}

