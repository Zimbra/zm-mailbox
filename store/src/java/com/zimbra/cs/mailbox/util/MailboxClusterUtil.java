package com.zimbra.cs.mailbox.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import com.google.common.io.Files;
import com.zimbra.common.util.ZimbraLog;

public class MailboxClusterUtil {

    private static String mailboxWorkerName;
    static {
        try {
            mailboxWorkerName = Files.asCharSource(new File("/var/run/pod-info/pod-name"), Charset.defaultCharset()).read();
            ZimbraLog.mailbox.info("mailbox worker name is %s", mailboxWorkerName);
        } catch (IOException e) {
            ZimbraLog.mailbox.warn("Unable to determine mailbox worker name! Defaulting to blank string");
            mailboxWorkerName = "";
        }
    }

    public static String getMailboxWorkerName() {
        return mailboxWorkerName;
    }

}
