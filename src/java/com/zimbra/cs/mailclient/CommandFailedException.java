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
 * Indicates that a mail protocol command has failed.
 */
public class CommandFailedException extends MailException {
    private final String cmd;

    /**
     * Creates a new <tt>CommandFailedException</tt> for the specified
     * command and detail message.
     * 
     * @param cmd the name of the failed command
     * @param msg the detail message, or <tt>null</tt> if none
     */
    public CommandFailedException(String cmd, String msg) {
        super(cmd + " failed: " + msg);
        this.cmd = cmd;
    }

    /**
     * Returns the name of the command that failed.
     * 
     * @return the name of the failed command
     */
    public String getCommand() {
        return cmd;
    }
}
