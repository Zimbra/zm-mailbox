/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.mailclient;

import java.io.IOException;

/**
 * Indicates that a mail client error has occurred.
 */
@SuppressWarnings("serial")
public class MailException extends IOException {
    /**
     * Creates a new <tt>MailException</tt> with a <tt>null</tt> detail
     * message.
     */
    public MailException() {}

    /**
     * Creates a new <tt>MailException</tt> with the specified detail message.
     *
     * @param msg the detail message, or <tt>null</tt> if none
     */
    public MailException(String msg) {
        super(msg);
    }

    /**
     * Creates a new <tt>MailException</tt> with the specified detail message
     * and cause.
     * 
     * @param msg the detail message, or <tt>null</tt> if none
     * @param cause the cause, or <tt>null</tt> if unknown
     */
    public MailException(String msg, Throwable cause) {
        super(msg);
        initCause(cause);
    }
}
