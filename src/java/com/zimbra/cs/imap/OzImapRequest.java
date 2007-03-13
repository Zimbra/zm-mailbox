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

import java.util.List;

/**
 * @author dkarp
 */
class OzImapRequest extends ImapRequest {
    OzImapRequest(OzImapConnectionHandler handler, ImapSession session) {
        super(handler, session);
    }

    public OzImapRequest(String tag, List<Object> currentRequestParts, OzImapConnectionHandler handler, ImapSession session) {
        super(handler, session);
        mTag = tag;
        mParts = currentRequestParts;
    }

    private byte[] getNextBuffer() throws ImapParseException {
        if ((mIndex + 1) >= mParts.size()) {
            throw new ImapParseException(mTag, "no next literal");
        }
        Object part = mParts.get(mIndex + 1);
        if (!(part instanceof byte[]))
            throw new ImapParseException(mTag, "in string next not literal");
        mIndex += 2;
        mOffset = 0;
        return (byte[]) part;
    }

    static String readTag(String line) throws ImapParseException  { return readContent(line, 0, null, TAG_CHARS); }

    byte[] readLiteral() throws ImapParseException {
        return getNextBuffer();
    }
}
