package com.zimbra.cs.imap;

import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;

interface ImapSessionHandler {
    void dropConnection();
    DateFormat getDateFormat();
    DateFormat getZimbraFormat();
    void sendNotifications(boolean notifyExpunges, boolean flush) throws IOException;
    void dumpState(Writer w);
}
