/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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