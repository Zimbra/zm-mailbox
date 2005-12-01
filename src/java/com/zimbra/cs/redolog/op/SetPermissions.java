package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MetadataList;

public class SetPermissions extends RedoableOp {

    private int mFolderId;
    private String mACL;

    public SetPermissions() {
        mFolderId = UNKNOWN_ID;
        mACL = "";
    }

    public SetPermissions(int mailboxId, int folderId, ACL acl) {
        setMailboxId(mailboxId);
        mFolderId = folderId;
        mACL = acl == null ? "" : acl.toString();
    }

    public int getOpCode() {
        return OP_SET_PERMISSIONS;
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=").append(mFolderId);
        sb.append(", acl=").append(mACL);
        return sb.toString();
    }

    protected void serializeData(DataOutput out) throws IOException {
        out.writeInt(mFolderId);
        writeUTF8(out, mACL);
    }

    protected void deserializeData(DataInput in) throws IOException {
        mFolderId = in.readInt();
        mACL = readUTF8(in);
    }

    
    public void redo() throws Exception {
        Mailbox mbox = Mailbox.getMailboxById(getMailboxId());
        ACL acl = (mACL.equals("") ? null : new ACL(new MetadataList(mACL)));
        mbox.setPermissions(getOperationContext(), mFolderId, acl);
    }
}
