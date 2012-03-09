package com.zimbra.cs.redolog.op;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class SetActiveSyncDisabled extends RedoableOp {
    
    private int folderId;
    private boolean disableActiveSync;

    public SetActiveSyncDisabled() {
        super(MailboxOperation.SetDisableActiveSync);
    }
    
    public SetActiveSyncDisabled(int mailboxId, int folderId, boolean disableActiveSync) {
        this();
        setMailboxId(mailboxId);
        this.folderId = folderId;
        this.disableActiveSync = disableActiveSync;
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        folderId = in.readInt();
        disableActiveSync = in.readBoolean();
    }

    @Override
    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=").append(folderId);
        sb.append(", disableActiveSync="+(disableActiveSync?"TRUE":"FALSE"));
        return sb.toString();
    }

    @Override
    public void redo() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        mbox.setActiveSyncDisabled(getOperationContext(), folderId, disableActiveSync);
    }

    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(folderId);
        out.writeBoolean(disableActiveSync);
    }
    
    /* Unit test methods */
    
    byte[] testSerialize() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializeData(new RedoLogOutput(out));
        return out.toByteArray();
    }
    
    void testDeserialize(byte[] data) throws IOException {
        deserializeData(new RedoLogInput(new ByteArrayInputStream(data)));
    }

}
