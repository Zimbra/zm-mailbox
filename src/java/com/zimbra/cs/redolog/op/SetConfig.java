/*
 * Created on Nov 12, 2005
 */
package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;

/**
 * @author dkarp
 */
public class SetConfig extends RedoableOp {

    private String mSection;
    private String mConfig;

    public SetConfig() {
        mSection = "";
        mConfig = "";
    }

    public SetConfig(int mailboxId, String section, Metadata config) {
        setMailboxId(mailboxId);
        mSection = section == null ? "" : section;
        mConfig = config == null ? "" : config.toString();
    }

    public int getOpCode() {
        return OP_SET_CONFIG;
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("section=").append(mSection);
        sb.append(", config=").append(mConfig.equals("") ? "null" : mConfig);
        return sb.toString();
    }

    protected void serializeData(DataOutput out) throws IOException {
        writeUTF8(out, mSection);
        writeUTF8(out, mConfig);
    }

    protected void deserializeData(DataInput in) throws IOException {
        mSection = readUTF8(in);
        mConfig = readUTF8(in);
    }

    public void redo() throws Exception {
        Mailbox mbox = Mailbox.getMailboxById(getMailboxId());
        mbox.setConfig(getOperationContext(), mSection, mConfig.equals("") ? null : new Metadata(mConfig));
    }
}
