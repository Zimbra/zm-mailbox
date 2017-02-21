/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
