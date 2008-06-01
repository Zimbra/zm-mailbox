/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008 Zimbra, Inc.
 *
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailclient;

import java.io.IOException;

/**
 * Indicates that a mail client error has occurred.
 */
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
