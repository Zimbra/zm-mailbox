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
import java.util.Arrays;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class TestImapUtil extends TestCase {
    private static final String[] FOLDERS = {
        "BOOBOO/BEAR", "INBOX", "Inbox", "Inbox/Foo", "Foobar", "INBOX/bar",
        "Foobar/Blah Blah/XXX", "Foobar/Blah Blah"
    };

    private static final String[] FOLDERS_2 = {
        "Bulk Mail",
        "INBOX/Page-PSI-Wallet",
        "INBOX/ReviewBoard",
        "Trash",
        "INBOX/Page-PSI-Bill",
        "INBOX/QA",
        "INBOX/PSI-Wallet-OnCall",
        "INBOX/Devel-CPP",
        "INBOX/Manoj",
        "Drafts",
        "INBOX/PSI-Wallet",
        "INBOX/IT Support",
        "Sent",
        "INBOX/FitnessCenter",
        "INBOX/Hack",
        "INBOX/Bugzilla",
        "Junk E-mail",
        "Deleted Items",
        "Contacts",
        "Calendar",
        "INBOX/Devel-Random",
        "INBOX/PSI-Payments",
        "INBOX/D_PSI-Devel",
        "Sent Items",
        "INBOX/Page-PSI-Payments",
        "INBOX/BTN",
        "INBOX/PSI-Billing",
        "INBOX",
        "Outbox",
        "Journal",
        "Notes",
        "Tasks",
        "Sync Issues",
        "Sync Issues/Conflicts",
        "Sync Issues/Local Failures",
        "Sync Issues/Server Failures"
    };

    private static final String[] SORTED_FOLDERS = {
        "INBOX", "INBOX/bar", "Inbox/Foo", "Foobar/Blah Blah/XXX",
        "Foobar/Blah Blah", "Foobar", "BOOBOO/BEAR"
    };

    public void testSortFolders() throws Exception {
        List<String> folders = getNames(ImapUtil.sortFolders(parseFolders(FOLDERS)));
        assertEquals(Arrays.asList(SORTED_FOLDERS), folders);
    }

    public void testSortFolders2() throws Exception {
        List<String> folders = getNames(ImapUtil.sortFolders(parseFolders(FOLDERS_2)));
        assertEquals("INBOX", folders.get(0));
    }
    
    private List<ListData> parseFolders(String[] folders) throws IOException {
        List<ListData> lds = new ArrayList<ListData>();
        ImapInputStream is = getImapInputStream(folders);
        while (!is.isEOF()) {
            is.skipChar('*');
            is.skipChar(' ');
            is.readAtom();
            is.skipChar(' ');
            lds.add(ListData.read(is));
            is.skipCRLF();
        }
        return lds;
    }

    private List<String> getNames(List<ListData> lds) {
        List<String> names = new ArrayList<String>(lds.size());
        for (ListData ld : lds) {
            names.add(ld.getMailbox());
        }
        return names;
    }

    private static ImapInputStream getImapInputStream(String[] folders) {
        StringBuilder sb = new StringBuilder();
        for (String folder : folders) {
            sb.append("* LIST () \"/\" \"").append(folder).append("\"\r\n");
        }
        return new ImapInputStream(
            new ByteArrayInputStream(Ascii.getBytes(sb.toString())), null);
    }
}
