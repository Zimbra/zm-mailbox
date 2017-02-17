/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.redolog;

import com.zimbra.cs.redolog.op.RedoableOp;

public class MailboxIdConflictException extends RedoException {
    private static final long serialVersionUID = -4186818816051395390L;

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
