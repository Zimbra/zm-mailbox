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
    private final String error;

    /**
     * Creates a new <tt>CommandFailedException</tt> for the specified
     * command and detail message.
     * 
     * @param cmd the name of the failed command
     * @param error the error message, or <tt>null</tt> if none
     */
    public CommandFailedException(String cmd, String error) {
        this.cmd = cmd;
        this.error = error;
    }

    /**
     * Returns the name of the command that failed.
     * 
     * @return the name of the failed command
     */
    public String getCommand() {
        return cmd;
    }

    /**
     * Returns the error that caused the failure
     *
     * @return the error message, or <tt>null</tt> if unknown
     */
    public String getError() {
        return error;
    }

    /**
     * Returns the exception detail message.
     * 
     * @return the exception detail message
     */
    @Override
    public String getMessage() {
        return error != null ? cmd + " failed: " + error : cmd + " failed";
    }
}
