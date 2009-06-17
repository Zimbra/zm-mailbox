/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
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
    private static final long serialVersionUID = -4186818816051395390L;

    private String mAccountId;
    private long mExpectedId;
    private long mFoundId;

    public MailboxIdConflictException(String accountId, long expectedId, long foundId, RedoableOp op) {
        super("Mailbox ID for account " + accountId + " changed unexpectedly to " + foundId +
              "; expected " + expectedId, op);
        mAccountId = accountId;
        mExpectedId = expectedId;
        mFoundId = foundId;
    }

    public String getAccountId() { return mAccountId; }
    public long getExpectedId() { return mExpectedId; }
    public long getFoundId() { return mFoundId; }
}
