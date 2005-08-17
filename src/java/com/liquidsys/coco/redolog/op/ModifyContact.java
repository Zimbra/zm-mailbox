/*
 * Created on 2004. 12. 13.
 */
package com.liquidsys.coco.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.liquidsys.coco.mailbox.Mailbox;

/**
 * @author jhahm
 */
public class ModifyContact extends RedoableOp {

    private int mId;
    private boolean mReplace;
    private Map mAttrs;

    public ModifyContact() {
        mId = UNKNOWN_ID;
    }

    public ModifyContact(int mailboxId, int id, Map attrs, boolean replace) {
        setMailboxId(mailboxId);
        mId = id;
        mReplace = replace;
        mAttrs = attrs;
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.redolog.op.RedoableOp#getOperationCode()
     */
    public int getOpCode() {
        return OP_MODIFY_CONTACT;
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.redolog.op.RedoableOp#getPrintableData()
     */
    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=");
        sb.append(mId).append(", replace=").append(mReplace);
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
     * @see com.liquidsys.coco.redolog.op.RedoableOp#serializeData(java.io.DataOutput)
     */
    protected void serializeData(DataOutput out) throws IOException {
        out.writeInt(mId);
        out.writeBoolean(mReplace);
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
     * @see com.liquidsys.coco.redolog.op.RedoableOp#deserializeData(java.io.DataInput)
     */
    protected void deserializeData(DataInput in) throws IOException {
        mId = in.readInt();
        mReplace = in.readBoolean();
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
        mailbox.modifyContact(getOperationContext(), mId, mAttrs, mReplace);
    }
}
