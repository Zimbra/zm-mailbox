/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.imap;

/**
 * IMAP base exception.
 *
 * @author ysasaki
 */
abstract class ImapException extends Exception {
    private static final long serialVersionUID = -7723826215470186860L;

    ImapException() {
    }

    ImapException(String message) {
        super(message);
    }

    ImapException(String message, Throwable cause) {
        super(message, cause);
    }
}
