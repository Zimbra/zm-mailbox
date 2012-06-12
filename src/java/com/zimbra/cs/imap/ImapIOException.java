/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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

public class ImapIOException extends ImapException {

    private static final long serialVersionUID = 5910832733809945145L;

    public ImapIOException() {
        super();
    }

    public ImapIOException(String message, Throwable cause) {
        super(message, cause);
    }

    public ImapIOException(String message) {
        super(message);
    }
}
