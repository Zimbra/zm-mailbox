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
package com.zimbra.cs.datasource;

import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.imap.ListData;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.io.IOException;

public final class ImapUtil {
    private static final String INBOX = "INBOX";
    private static final int INBOX_LEN = INBOX.length();

    // Used for sorting ListData lexicographically in reverse order. This
    // ensures that inferior mailboxes will be processed before their
    // parents, which avoids problems when deleting folders. Also, ignore
    // case of INBOX part when comparing INBOX or its inferiors.
    private static final Comparator<ListData> COMPARATOR =
        new Comparator<ListData>() {
            public int compare(ListData ld1, ListData ld2) {
                String name1 = getNormalizedName(ld1);
                String name2 = getNormalizedName(ld2);
                return name2.compareTo(name1);
            }
        };

    public static List<ListData> listFolders(ImapConnection ic, String name)
        throws IOException {
        return sortFolders(ic.list("", name));
    }

    public static List<ListData> sortFolders(List<ListData> folders) {
        // Keep INBOX and inferiors separate so we can return them first
        ListData inbox = null;
        List<ListData> inboxInferiors = new ArrayList<ListData>();
        List<ListData> otherFolders = new ArrayList<ListData>();
        for (ListData ld : folders) {
            String name = ld.getMailbox();
            if (name.equalsIgnoreCase(INBOX)) {
                if (inbox == null) {
                    inbox = ld; // Ignore duplicate INBOX (fixes bug 26483)
                }
            } else if (isInboxInferior(ld)) {
                inboxInferiors.add(ld);
            } else {
                otherFolders.add(ld);
            }
        }
        List<ListData> sorted = new ArrayList<ListData>(folders.size());
        if (inbox == null) {
            // If INBOX missing from LIST response, then see if we can
            // determine a reasonable default (bug 30844).
            inbox = getDefaultInbox(sorted);
        }
        if (inbox != null) {
            sorted.add(inbox);
        }
        Collections.sort(inboxInferiors, COMPARATOR);
        sorted.addAll(inboxInferiors);
        Collections.sort(otherFolders, COMPARATOR);
        sorted.addAll(otherFolders);
        return sorted;
    }

    private static ListData getDefaultInbox(List<ListData> sorted) {
        for (ListData ld : sorted) {
            if (isInboxInferior(ld)) {
                return new ListData(INBOX, ld.getDelimiter());
            }
        }
        return null;
    }
    
    private static String getNormalizedName(ListData ld) {
        String name = ld.getMailbox();
        if (name.equalsIgnoreCase(INBOX)) {
            return INBOX;
        } else if (isInboxInferior(ld)) {
            return INBOX + name.substring(INBOX_LEN);
        } else {
            return name;
        }
    }
    
    /*
     * Returns true if specified ListData refers to am inferior of INBOX
     * (i.e. "INBOX/Foo").
     */
    private static boolean isInboxInferior(ListData ld) {
        String name = ld.getMailbox();
        return name.length() > INBOX_LEN &&
               name.substring(0, INBOX_LEN).equalsIgnoreCase(INBOX) &&
               name.charAt(INBOX_LEN) == ld.getDelimiter();
    }
}
