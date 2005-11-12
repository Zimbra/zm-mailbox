/*
 * Created on Nov 12, 2005
 */
package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;

/**
 * @author dkarp
 */
public class GrantAccess extends RedoableOp {

    private int mFolderId;
    private String mGrantee;
    private byte mGranteeType;
    private short mRights;
    private boolean mInherit;

    public GrantAccess() {
        mFolderId = UNKNOWN_ID;
        mGrantee = "";
    }

    public GrantAccess(int mailboxId, int folderId, String grantee, byte granteeType, short rights, boolean inherit) {
        setMailboxId(mailboxId);
        mFolderId = folderId;
        mGrantee = grantee == null ? "" : grantee;
        mGranteeType = granteeType;
        mRights = rights;
        mInherit = inherit;
    }

    public int getOpCode() {
        return OP_GRANT_ACCESS;
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=").append(mFolderId);
        sb.append(", grantee=").append(mGrantee);
        sb.append(", type=").append(mGranteeType);
        sb.append(", rights=").append(ACL.rightsToString(mRights));
        sb.append(", inherit=").append(mInherit);
        return sb.toString();
    }

    protected void serializeData(DataOutput out) throws IOException {
        out.writeInt(mFolderId);
        writeUTF8(out, mGrantee);
        out.writeByte(mGranteeType);
        out.writeShort(mRights);
        out.writeBoolean(mInherit);
    }

    protected void deserializeData(DataInput in) throws IOException {
        mFolderId = in.readInt();
        mGrantee = readUTF8(in);
        mGranteeType = in.readByte();
        mRights = in.readShort();
        mInherit = in.readBoolean();
    }

    public void redo() throws ServiceException {
        Mailbox mbox = Mailbox.getMailboxById(getMailboxId());
        mbox.grantAccess(getOperationContext(), mFolderId, mGrantee, mGranteeType, mRights, mInherit);
    }
}
