/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
/*
 * Created on Nov 12, 2005
 */
package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class SetConfig extends RedoableOp {

    private String mSection;
    private String mConfig;

    public SetConfig() {
        super(MailboxOperation.SetConfig);
        mSection = "";
        mConfig = "";
    }

    public SetConfig(int mailboxId, String section, Metadata config) {
        this();
        setMailboxId(mailboxId);
        mSection = section == null ? "" : section;
        mConfig = config == null ? "" : config.toString();
    }

    @Override protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("section=").append(mSection);
        sb.append(", config=").append(mConfig.equals("") ? "null" : mConfig);
        return sb.toString();
    }

    @Override protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeUTF(mSection);
        out.writeUTF(mConfig);
    }

    @Override protected void deserializeData(RedoLogInput in) throws IOException {
        mSection = in.readUTF();
        mConfig = in.readUTF();
    }

    @Override public void redo() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        mbox.setConfig(getOperationContext(), mSection, mConfig.equals("") ? null : new Metadata(mConfig));
    }
}
