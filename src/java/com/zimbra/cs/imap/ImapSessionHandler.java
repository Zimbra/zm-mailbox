/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.imap;

import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;

interface ImapSessionHandler {
    void dropConnection(boolean sendBanner);
    DateFormat getTimeFormat();
    DateFormat getDateFormat();
    DateFormat getZimbraFormat();
    void sendNotifications(boolean notifyExpunges, boolean flush) throws IOException;
    void dumpState(Writer w);
}
