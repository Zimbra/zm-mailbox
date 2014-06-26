/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.imap;

class ImapParseException extends ImapException {
    private static final long serialVersionUID = 4675342317380797673L;

    String mTag, mCode;
    boolean mNO;

    ImapParseException() {
    }

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
