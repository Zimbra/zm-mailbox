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

/**
 * Indicates that a parsing error occurred while reading a mail protocol
 * response.
 */
public class ParseException extends MailException {
    /**
     * Creates a new <tt>ParseException</tt> with a <tt>null</tt> detail
     * message.
     */
    public ParseException() {}

    /**
     * Creates a new <tt>ParseException</tt> with the specified detail message.
     *
     * @param msg the detail message, or <tt>null</tt> if none
     */
    public ParseException(String msg) {
        super(msg);
    }
}
