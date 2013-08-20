/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.lmtpserver;

/**
 * Handling of this type of exception should be to simply drop the connection. 
 */
public class UnrecoverableLmtpException extends Exception {

    public UnrecoverableLmtpException(String message) {
        super(message);
    }

    public UnrecoverableLmtpException(String message, Throwable cause) {
        super(message, cause);
    }
}
