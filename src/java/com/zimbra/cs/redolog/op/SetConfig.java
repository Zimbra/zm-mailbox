/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
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
