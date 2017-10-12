/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016, 2017 Synacor, Inc.
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

package com.zimbra.cs.imap;

public class ImapParseException extends ImapException {
    private static final long serialVersionUID = 4675342317380797673L;

    protected String mTag;
    protected String responseCode;
    protected boolean userServerResponseNO;  /* if true use "NO" as server response, else use "BAD" */

    protected ImapParseException() {
    }

    protected ImapParseException(String tag, String message) {
        super("parse error: " + message);
        mTag = tag;
    }

    protected ImapParseException(String tag, String message, boolean no, boolean parseError) {
        super((parseError ? "parse error: " : "") + message);
        mTag = tag;
        userServerResponseNO = no;
    }

    protected ImapParseException(String tag, String code, String message, boolean parseError) {
        super((parseError ? "parse error: " : "") + message);
        mTag = tag;
        responseCode = code;
        userServerResponseNO = code != null;
    }

    protected static class ImapMaximumSizeExceededException extends ImapParseException {
        private static final long serialVersionUID = -8080429172062016010L;
        public static final String sizeExceededFmt = "maximum %s size exceeded";
        protected ImapMaximumSizeExceededException(String tag, String code, String exceededType) {
            super(tag, code,
                    String.format(sizeExceededFmt, exceededType),
                    false /* don't prefix parse error: */);
        }
        protected ImapMaximumSizeExceededException(String tag, String exceededType) {
            super(tag,
                    String.format(sizeExceededFmt, exceededType),
                    false /* use BAD not NO */, false /* don't prefix parse error: */);
        }
    }
}
