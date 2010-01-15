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