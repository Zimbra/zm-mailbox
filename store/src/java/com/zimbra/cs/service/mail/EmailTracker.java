/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2025 Synacor, Inc.
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
package com.zimbra.cs.service.mail;

import com.zimbra.common.util.ZimbraLog;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class EmailTracker {
    private static final String SENT_EMAILS_FILE = "/tmp/sent_emails.txt";
    private static final Set<String> sentEmails = new HashSet<>();

    static {
        loadSentEmails();
    }

    /**
     * Loads the list of previously sent email addresses from the file specified by the constant
     * `SENT_EMAILS_FILE`. If the file does not exist, it is created. The method reads each email
     * address from the file and adds it to the `sentEmails` set to keep track of emails that have
     * already been sent.
     *
     * @throws IOException If an error occurs while reading the file or creating the file.
     */
    private static void loadSentEmails() {
        File file = new File(SENT_EMAILS_FILE);
        try {
        if (!file.exists()) {
            file.createNewFile();
        }
        BufferedReader reader = new BufferedReader(new FileReader(SENT_EMAILS_FILE));
            String email;
            while ((email = reader.readLine()) != null) {
                sentEmails.add(email);
            }
        } catch (IOException e) {
            ZimbraLog.store.error("Failed to load sent emails: " + e.getMessage());
        }
    }

    /**
     * Checks if an email has already been sent by verifying its presence in the `sentEmails` set.
     *
     * @param email The email address to be checked.
     * @return `true` if the email has already been sent (i.e., exists in the `sentEmails` set),
     *         otherwise `false`.
     */
    public static boolean isEmailSent(String email) {
        return sentEmails.contains(email);
    }

    /**
     * Marks the specified email address as sent by adding it to the `sentEmails` set and
     * appending it to the `SENT_EMAILS_FILE`. The method ensures that an email is only marked
     * once to avoid duplicates.
     *
     * @param email The email address to be marked as sent.
     *
     * @throws IOException If an error occurs while writing to the file.
     */
    public static synchronized void markEmailAsSent(String email) {
        if (sentEmails.add(email)) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(SENT_EMAILS_FILE, true))) {
                writer.write(email);
                writer.newLine();
            } catch (IOException e) {
                ZimbraLog.store.error("Failed to mark email as sent: " + e.getMessage());
            }
        }
    }

    public static String getSentEmailsFile() {
        return SENT_EMAILS_FILE;
    }
}
