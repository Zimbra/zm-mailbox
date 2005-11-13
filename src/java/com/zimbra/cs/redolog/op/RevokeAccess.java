/*
 * Created on Nov 12, 2005
 */
package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;

/**
 * @author dkarp
 */
public class RevokeAccess extends RedoableOp {

    private int mFolderId;
    private String mGrantee;

    public RevokeAccess() {
        mFolderId = UNKNOWN_ID;
        mGrantee = "";
    }

    public RevokeAccess(int mailboxId, int folderId, String grantee) {
        setMailboxId(mailboxId);
        mFolderId = folderId;
        mGrantee = grantee == null ? "" : grantee;
    }

    public int getOpCode() {
        return OP_REVOKE_ACCESS;
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=").append(mFolderId);
        sb.append(", grantee=").append(mGrantee);
        return sb.toString();
    }

    protected void serializeData(DataOutput out) throws IOException {
        out.writeInt(mFolderId);
        writeUTF8(out, mGrantee);
    }

    protected void deserializeData(DataInput in) throws IOException {
        mFolderId = in.readInt();
        mGrantee = readUTF8(in);
    }

    public void redo() throws ServiceException {
        Mailbox mbox = Mailbox.getMailboxById(getMailboxId());
        mbox.revokeAccess(getOperationContext(), mFolderId, mGrantee);
    }
}
