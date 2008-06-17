/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008 Zimbra, Inc.
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
package com.zimbra.qa.unittest;

import junit.framework.TestCase;
import com.zimbra.cs.mailclient.imap.ListData;
import com.zimbra.cs.mailclient.imap.ImapInputStream;
import com.zimbra.cs.mailclient.util.Ascii;
import com.zimbra.cs.datasource.ImapUtil;

import java.util.List;
import java.util.ArrayList;
import java.io.ByteArrayInputStream;

public class TestImapUtil extends TestCase {
    private static ImapInputStream getImapInputStream(String[] folders) {
        StringBuilder sb = new StringBuilder();
        for (String folder : folders) {
            sb.append("* LIST () \"/\" \"").append(folder).append("\"\r\n");
        }
        return new ImapInputStream(
            new ByteArrayInputStream(Ascii.getBytes(sb.toString())), null);
    }

    private static final String[] FOLDERS = {
        "BOOBOO/BEAR", "INBOX", "Inbox", "Inbox/Foo", "Foobar", "INBOX/bar",
        "Foobar/Blah Blah/XXX", "Foobar/Blah Blah"
    };

    private static final String[] SORTED_FOLDERS = {
        "INBOX", "INBOX/bar", "Inbox/Foo", "Foobar/Blah Blah/XXX",
        "Foobar/Blah Blah", "Foobar", "BOOBOO/BEAR"
    };

    public void testSortFolders() throws Exception {
        List<ListData> folders = new ArrayList<ListData>();
        ImapInputStream is = getImapInputStream(FOLDERS);
        while (!is.isEOF()) {
            is.skipChar('*');
            is.skipChar(' ');
            is.readAtom();
            is.skipChar(' ');
            folders.add(ListData.read(is));
            is.skipCRLF();
        }
        folders = ImapUtil.sortFolders(folders);
        for (int i = 0; i < folders.size(); i++) {
            assertEquals(SORTED_FOLDERS[i], folders.get(i).getMailbox());
        }
    }
}
