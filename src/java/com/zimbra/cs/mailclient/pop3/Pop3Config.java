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
package com.zimbra.cs.mailclient.pop3;

import com.zimbra.cs.mailclient.MailConfig;

/**
 * POP3 client configuration.
 */
public class Pop3Config extends MailConfig {
    public static final String PROTOCOL = "pop3";
    
    public static final int DEFAULT_PORT = 110;
    public static final int DEFAULT_SSL_PORT = 995;

    public String getProtocol() { return PROTOCOL; }

    public int getPort() {
        if (port != -1) return port;
        return sslEnabled ? DEFAULT_SSL_PORT : DEFAULT_PORT;
    }
}
