/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
 * Created on 2004. 11. 2.
 */
package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.MailboxIdConflictException;
import com.zimbra.cs.redolog.RedoException;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class CreateMailbox extends RedoableOp {

    private String mAccountId;

    public CreateMailbox() {
        super(MailboxOperation.CreateMailbox);
    }

    public CreateMailbox(String accountId) {
        this();
        mAccountId = accountId;
    }

    @Override protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("account=").append(mAccountId != null ? mAccountId : "");
        return sb.toString();
    }

    @Override protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeUTF(mAccountId);
    }

    @Override protected void deserializeData(RedoLogInput in) throws IOException {
        mAccountId = in.readUTF();
    }

    @Override public void redo() throws Exception {
        int opMboxId = getMailboxId();
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(mAccountId, false);

        if (mbox == null) {
            Account account = Provisioning.getInstance().get(AccountBy.id, mAccountId);
            if (account == null) { 
                throw new RedoException("Account " + mAccountId + " does not exist", this);
            }

            mbox = MailboxManager.getInstance().createMailbox(getOperationContext(), account);
            if (mbox == null) {
                //something went really wrong
                throw new RedoException("unable to create mailbox for accountId " + mAccountId, this);
            }
        }

        int mboxId = mbox.getId();
        if (opMboxId == mboxId) {
            mLog.info("Mailbox " + opMboxId + " for account " + mAccountId + " already exists");
            return;
        } else {
            throw new MailboxIdConflictException(mAccountId, opMboxId, mboxId, this);
        }
    }
}
