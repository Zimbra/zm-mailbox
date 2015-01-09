/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.redolog.op.RedoableOp;


/**
 * Helper class to enable unit tests to create transactions directly; i.e. providing visibility to test scope
 *
 */
public class MailboxTransactionProxy {
    public static void beginTransaction(Mailbox mbox, String caller, RedoableOp op) throws ServiceException {
        mbox.beginTransaction(caller, null, op);
    }

    public static void endTransaction(Mailbox mbox, boolean success) throws ServiceException {
        mbox.endTransaction(success);
    }
}
