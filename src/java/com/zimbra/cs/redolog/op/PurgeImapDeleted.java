/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.redolog.op;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class PurgeImapDeleted extends RedoableOp {

    public PurgeImapDeleted() {
        super(MailboxOperation.PurgeImapDeleted);
    }

    public PurgeImapDeleted(int mailboxId) {
        this();
        setMailboxId(mailboxId);
    }

    @Override
    protected String getPrintableData() {
        // no members to print
        return null;
    }

    @Override
    protected void serializeData(RedoLogOutput out) {
        // no members to serialize
    }

    @Override
    protected void deserializeData(RedoLogInput in) {
        // no members to deserialize
    }

    @Override
    public void redo() throws ServiceException {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        mbox.purgeImapDeleted(getOperationContext());
    }

    @Override
    public boolean isDeleteOp() {
        return true;
    }
}
