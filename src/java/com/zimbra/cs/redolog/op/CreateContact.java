/*
 * Created on 2004. 12. 13.
 */
package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * @author jhahm
 */
public class CreateContact extends RedoableOp {

    private int mId;
    private int mFolderId;
    private Map mAttrs;
    private String mTags;

    public CreateContact() {
        mId = UNKNOWN_ID;
        mFolderId = UNKNOWN_ID;
    }

    public CreateContact(int mailboxId, int folderId, Map attrs, String tags) {
        setMailboxId(mailboxId);
        mId = UNKNOWN_ID;
        mFolderId = folderId;
        mAttrs = attrs;
        mTags = tags != null ? tags : "";
    }

    public void setContactId(int id) {
    	mId = id;
    }

    public int getContactId() {
    	return mId;
    }

    /* (non-Javadoc)
	 * @see com.zimbra.cs.redolog.op.RedoableOp#getOperationCode()
	 */
	public int getOpCode() {
		return OP_CREATE_CONTACT;
	}

	/* (non-Javadoc)
	 * @see com.zimbra.cs.redolog.op.RedoableOp#getPrintableData()
	 */
	protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("folder=").append(mFolderId);
        sb.append(", tags=\"").append(mTags).append("\"");
        if (mAttrs != null && mAttrs.size() > 0) {
            sb.append(", attrs={");
            for (Iterator it = mAttrs.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry entry = (Map.Entry) it.next();
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();
                sb.append("\n    ").append(key).append(": ").append(value);
            }
            sb.append("\n}");
        }
		return sb.toString();
	}

	/* (non-Javadoc)
	 * @see com.zimbra.cs.redolog.op.RedoableOp#serializeData(java.io.DataOutput)
	 */
	protected void serializeData(DataOutput out) throws IOException {
        out.writeInt(mId);
        out.writeInt(mFolderId);
        writeUTF8(out, mTags);
        int numAttrs = mAttrs != null ? mAttrs.size() : 0;
        out.writeShort((short) numAttrs);
        for (Iterator it = mAttrs.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            writeUTF8(out, (String) entry.getKey());
            String value = (String) entry.getValue();
            writeUTF8(out, value != null ? value : "");
        }
	}

	/* (non-Javadoc)
	 * @see com.zimbra.cs.redolog.op.RedoableOp#deserializeData(java.io.DataInput)
	 */
	protected void deserializeData(DataInput in) throws IOException {
        mId = in.readInt();
        mFolderId = in.readInt();
        mTags = readUTF8(in);
        int numAttrs = in.readShort();
        if (numAttrs > 0) {
            mAttrs = new HashMap(numAttrs);
            for (int i = 0; i < numAttrs; i++) {
                String key = readUTF8(in);
                String value = readUTF8(in);
                mAttrs.put(key, value);
            }
        }
	}

    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mailbox = Mailbox.getMailboxById(mboxId);
        try {
            mailbox.createContact(getOperationContext(), mAttrs, mFolderId, mTags);
        } catch (MailServiceException e) {
            String code = e.getCode();
            if (code.equals(MailServiceException.ALREADY_EXISTS)) {
                if (mLog.isInfoEnabled())
                    mLog.info("Contact " + mId + " already exists in mailbox " + mboxId);
            } else
                throw e;
        }
    }
}
