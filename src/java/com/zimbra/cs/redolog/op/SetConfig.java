/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
/*
 * Created on Nov 12, 2005
 */
package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

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

    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeUTF(mSection);
        out.writeUTF(mConfig);
    }

    protected void deserializeData(RedoLogInput in) throws IOException {
        mSection = in.readUTF();
        mConfig = in.readUTF();
    }

    public void redo() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        mbox.setConfig(getOperationContext(), mSection, mConfig.equals("") ? null : new Metadata(mConfig));
    }
}
