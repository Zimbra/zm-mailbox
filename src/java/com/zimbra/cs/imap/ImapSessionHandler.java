package com.zimbra.cs.imap;

import java.io.IOException;
import java.text.DateFormat;

interface ImapSessionHandler {
    void dropConnection(boolean connectionIsOpen);
    DateFormat getDateFormat();
    DateFormat getZimbraFormat();
    void sendNotifications(boolean notifyExpunges, boolean flush) throws IOException;
}
