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
import com.zimbra.cs.mailclient.util.Config;

import java.util.Properties;
import java.io.File;
import java.io.IOException;

/**
 * Represents POP3 mail client configuration.
 */
public class Pop3Config extends MailConfig {
    /** POP3 configuration protocol name */
    public static final String PROTOCOL = "pop3";

    /** Default port for POP3 plain text connection */
    public static final int DEFAULT_PORT = 110;

    /** Default port for POP3 SSL connection */
    public static final int DEFAULT_SSL_PORT = 995;

    /**
     * Loads POP3 configuration properties from the specified file.
     *
     * @param file the configuration properties file
     * @return the <tt>Pop3Config</tt> for the properties
     * @throws IOException if an I/O error occurred
     */
    public static Pop3Config load(File file) throws IOException {
        Properties props = Config.loadProperties(file);
        Pop3Config config = new Pop3Config();
        config.applyProperties(props);
        return config;
    }
    
    /**
     * Creates a new <tt>Pop3Config</tt>.
     */
    public Pop3Config() {}

    /**
     * Creates a new <tt>Pop3Config</tt> for the specified server host.
     *  
     * @param host the server host name
     */
    public Pop3Config(String host) {
        super(host);
    }

    /**
     * Returns the POP3 protocol name (value of {@link #PROTOCOL}).
     *
     * @return the POP3 protocol name
     */
    @Override
    public String getProtocol() {
        return PROTOCOL;
    }

    /**
     * Returns the POP3 server port number. If not set, the default is
     * {@link #DEFAULT_PORT} for a plain text connection and
     * {@link #DEFAULT_SSL_PORT} for an SSL connection.
     *
     * @return the POP3 server port number
     */
    @Override
    public int getPort() {
        int port = super.getPort();
        if (port != -1) return port;
        return isSslEnabled() ? DEFAULT_SSL_PORT : DEFAULT_PORT;
    }
}
