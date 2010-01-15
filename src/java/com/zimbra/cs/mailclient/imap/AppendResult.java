/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.mailclient.imap;

import com.zimbra.cs.mailclient.ParseException;

import java.io.IOException;

public final class AppendResult {
    private final long uidValidity;
    private final long[] uids;

    // UIDPLUS (RFC 2359):
    // resp_code_apnd ::= "APPENDUID" SPACE nz_number SPACE set
    public static AppendResult parse(ImapInputStream is) throws IOException {
        is.skipChar(' ');
        long uidValidity = is.readNZNumber();
        is.skipChar(' ');
        is.skipSpaces();
        String uidSet = is.readText(" ]");
        try {
            return new AppendResult(uidValidity, uidSet);
        } catch (IllegalArgumentException e) {
            throw new ParseException("Invalid APPENDUID result");
        }
    }

    private AppendResult(long uidValidity, String uidSet) {
        this.uidValidity = uidValidity;
        this.uids = ImapUtil.parseUidSet(uidSet);
    }

    public long getUidValidity() {
        return uidValidity;
    }

    public long[] getUids() {
        return uids;
    }

    public long getUid() {
        return uids[0];
    }
}