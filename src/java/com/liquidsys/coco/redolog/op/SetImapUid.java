/*
 * Created on Jul 24, 2005
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
 * @author dkarp
 */
public class SetImapUid extends RedoableOp {

    private Map mImapUids = new HashMap();

    public SetImapUid() {
    }

    public SetImapUid(int mailboxId, int[] msgIds) {
        setMailboxId(mailboxId);
        for (int i = 0; i < msgIds.length; i++)
            mImapUids.put(new Integer(msgIds[i]), new Integer(UNKNOWN_ID));
    }

    public int getImapUid(int msgId) {
        Integer imapUid = (Integer) mImapUids.get(new Integer(msgId));
        int uid = imapUid == null ? Mailbox.ID_AUTO_INCREMENT : imapUid.intValue();
        return uid == UNKNOWN_ID ? Mailbox.ID_AUTO_INCREMENT : uid;
    }

    public void setImapUid(int msgId, int imapId) {
        mImapUids.put(new Integer(msgId), new Integer(imapId));
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.redolog.Redoable#getOperationCode()
     */
    public int getOpCode() {
        return OP_SET_IMAP_UID;
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.redolog.Redoable#getRedoContent()
     */
    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer();
        for (Iterator it = mImapUids.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            sb.append(sb.length() == 0 ? "" : ", ").append(entry.getKey()).append('=').append(entry.getValue());
        }
        return sb.toString();
    }

    protected void serializeData(DataOutput out) throws IOException {
        out.writeInt(mImapUids.size());
        for (Iterator it = mImapUids.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            out.writeInt(((Integer) entry.getKey()).intValue());
            out.writeInt(((Integer) entry.getValue()).intValue());
        }
    }

    protected void deserializeData(DataInput in) throws IOException {
        int count = in.readInt();
        for (int i = 0; i < count; i++) {
            int msgId = in.readInt();
            mImapUids.put(new Integer(msgId), new Integer(in.readInt()));
        }
    }

    public void redo() throws Exception {
        Mailbox mbox = Mailbox.getMailboxById(getMailboxId());
        int i = 0, msgIds[] = new int[mImapUids.size()];
        for (Iterator it = mImapUids.keySet().iterator(); it.hasNext(); )
            msgIds[i++] = ((Integer) it.next()).intValue();
        mbox.resetImapUid(getOperationContext(), msgIds);
    }
}
