/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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

package com.zimbra.cs.imap;

class ImapParseException extends Exception {
    private static final long serialVersionUID = 4675342317380797673L;

    String mTag, mCode;
    boolean mNO;

    ImapParseException() { }

    ImapParseException(String tag, String message) { 
        super("parse error: " + message);
        mTag = tag;
    }

    ImapParseException(String tag, String message, boolean no) { 
        super((no ? "" : "parse error: ") + message);
        mTag = tag;
        mNO = no;
    }

    ImapParseException(String tag, String code, String message, boolean parseError) {
        super((parseError ? "parse error: " : "") + message);
        mTag = tag;
        mCode = code;
        mNO = code != null;
    }
}
