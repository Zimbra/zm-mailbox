/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.imap;

/**
 * Exception thrown when session notification encounters a message which cannot be properly renumbered
 * This typically occurs when mailbox.item_id_checkpoint is inconsistent due to earlier manual DB modification
 * See bug 46549 and bug 77780 for more details on the sequence of events which results in this bad state
 */
public class ImapRenumberException extends RuntimeException {

    private static final long serialVersionUID = 6406289034846208672L;

    public ImapRenumberException() {
        super();
    }

    public ImapRenumberException(String message, Throwable cause) {
        super(message, cause);
    }

    public ImapRenumberException(String message) {
        super(message);
    }

}
