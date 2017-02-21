/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013, 2014, 2016 Synacor, Inc.
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

import com.google.common.base.Charsets;
import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class MailboxInfoTest extends TestCase {
    
    public void testStatusResponse() throws Exception {
        MailboxInfo info = parseResponse(" \"INBOX\" (MESSAGES 1 RECENT 2 UIDNEXT 3 UIDVALIDITY 4 UNSEEN 5)");
        assertEquals("INBOX", info.getName());
        assertEquals(1, info.getExists());
        assertEquals(2, info.getRecent());
        assertEquals(3, info.getUidNext());
        assertEquals(4, info.getUidValidity());
        assertEquals(5, info.getUnseen());
    }

    // AOL's IMAP server has been known to include "helpful" comments in the STATUS attribute list.
    // AOL is probably (incorrectly) copying their code from the SELECT/EXAMINE response;
    // those responses require a comment after the data -- don't ask why.
    public void testTolerateGarbageInStatusResponse() throws Exception {
        MailboxInfo info = parseResponse(" \"INBOX\" ( UIDNEXT 29370321 predicted next UID  UIDVALIDITY 1 UID validity status)");
        assertEquals("INBOX", info.getName());
        assertEquals(29370321, info.getUidNext());
        assertEquals(1, info.getUidValidity());
    }

    private static MailboxInfo parseResponse(String response) throws IOException {
        ImapInputStream is = new ImapInputStream(new ByteArrayInputStream(response.getBytes(Charsets.US_ASCII)), new ImapConfig());
        return MailboxInfo.readStatus(is);
    }
}
