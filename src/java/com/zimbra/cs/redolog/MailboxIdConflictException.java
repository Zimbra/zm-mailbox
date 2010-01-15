/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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

package com.zimbra.cs.redolog;

import com.zimbra.cs.redolog.op.RedoableOp;

public class MailboxIdConflictException extends RedoException {

    private String mAccountId;
    private int mExpectedId;
    private int mFoundId;

    public MailboxIdConflictException(String accountId, int expectedId, int foundId, RedoableOp op) {
        super("Mailbox ID for account " + accountId + " changed unexpectedly to " + foundId +
              "; expected " + expectedId, op);
        mAccountId = accountId;
        mExpectedId = expectedId;
        mFoundId = foundId;
    }

    public String getAccountId() { return mAccountId; }
    public int getExpectedId() { return mExpectedId; }
    public int getFoundId() { return mFoundId; }
}
