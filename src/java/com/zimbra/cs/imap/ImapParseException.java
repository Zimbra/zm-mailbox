/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.imap;

class ImapParseException extends ImapException {
    private static final long serialVersionUID = 4675342317380797673L;

    String mTag, mCode;
    boolean mNO;

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
    }
}
