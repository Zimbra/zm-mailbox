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

public final class CopyResult {
    private final long uidValidity;
    private final long[] fromUids;
    private final long[] toUids;

    public static CopyResult parse(ImapInputStream is) throws IOException {
        is.skipChar(' ');
        long uidValidity = is.readNZNumber();
        is.skipChar(' ');
        is.skipSpaces();
        String fromSet = is.readText(" ");
        is.skipChar(' ');
        is.skipSpaces();
        String toSet = is.readText(" ]");
        try {
            return new CopyResult(uidValidity, fromSet, toSet);
        } catch (IllegalArgumentException e) {
            throw new ParseException("Invalid COPYUID result");
        }
    }

    private CopyResult(long uidValidity, String fromUidSet, String toUidSet) {
        this.uidValidity = uidValidity;
        fromUids = ImapUtil.parseUidSet(fromUidSet);
        toUids = ImapUtil.parseUidSet(toUidSet);
        if (fromUids.length != toUids.length) {
            throw new IllegalArgumentException();
        }
    }
    
    public long getUidValidity() {
        return uidValidity;
    }

    public long[] getFromUids() {
        return fromUids;
    }

    public long[] getToUids() {
        return toUids;
    }
}