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
package com.zimbra.cs.mailclient.imap;

import com.zimbra.cs.mailclient.MailConfig;
import com.zimbra.cs.mailclient.util.Config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * IMAP protocol client configuration.
 */
public class ImapConfig extends MailConfig {
    private int maxLiteralMemSize = DEFAULT_MAX_LITERAL_MEM_SIZE;
    private int maxLiteralTraceSize = DEFAULT_MAX_LITERAL_TRACE_SIZE;
    private File literalDataDir = new File(System.getProperty("java.io.tmpdir"));

    /** IMAP configuration protocol name */
    public static final String PROTOCOL = "imap";

    /** Default port for IMAP plain text connection */
    public static final int DEFAULT_PORT = 143;

    /** Default port for IMAP SSL connection */
    public static final int DEFAULT_SSL_PORT = 993;

    /** Maximum literal size to cache in memory */
    public static final int DEFAULT_MAX_LITERAL_MEM_SIZE = 8 * 1024 * 1024;

    /** Maximum literal size to include in trace output if enabled */
    public static final int DEFAULT_MAX_LITERAL_TRACE_SIZE = 80;

    /**
     * Loads IMAP configuration properties from the specified file.
     *
     * @param file the configuration properties file
     * @return the <tt>ImapConfig</tt> for the properties
     * @throws IOException if an I/O error occurs
     */
    public static ImapConfig load(File file) throws IOException {
        Properties props = Config.loadProperties(file);
        ImapConfig config = new ImapConfig();
        config.applyProperties(props);
        return config;
    }
    
    /**
     * Creates a new <tt>ImapConfig</tt>.
     */
    public ImapConfig() {}

    /**
     * Creates a new <tt>ImapConfig</tt> for the specified server host.
     * 
     * @param host the IMAP server host name
     */
    public ImapConfig(String host) {
        super(host);
    }

    /**
     * Returns the IMAP protocol name (value of {@link #PROTOCOL}).
     *
     * @return the IMAP protocol name
     */
    @Override
    public String getProtocol() {
        return PROTOCOL;
    }

    /**
     * Returns the IMAP server port number. If not set, the default is
     * {@link #DEFAULT_PORT} for a plain text connection and
     * {@link #DEFAULT_SSL_PORT} for an SSL connection.
     * 
     * @return the IMAP server port number
     */
    @Override
    public int getPort() {
        int port = super.getPort();
        if (port != -1) return port;
        return isSslEnabled() ? DEFAULT_SSL_PORT : DEFAULT_PORT;
    }

    /**
     * Returns the maximum literal size to cache in memory. If a literal is
     * received that exceeds the specified number of bytes, then it will be
     * cached to disk. The default value is {@link #DEFAULT_MAX_LITERAL_MEM_SIZE}.
     * 
     * @return the maximum literal memory size in bytes
     * @see #getLiteralDataDir
     */
    public int getMaxLiteralMemSize() {
        return maxLiteralMemSize;
    }

    /**
     * Sets the maximum literal size to cache in memory.
     *
     * @param size the maximum literal memory size in bytes
     */
    public void setMaxLiteralMemSize(int size) {
        maxLiteralMemSize = size;
    }

    /**
     * Returns the maximum literal size to output to the protocol trace log.
     * If tracing is enabled and literal is received exceeding this size in
     * bytes, then it will be abbreviated in the trace log. The default
     * value is {@link #DEFAULT_MAX_LITERAL_TRACE_SIZE}
     * 
     * @return the maximum literal trace size in bytes
     */
    public int getMaxLiteralTraceSize() {
        return maxLiteralTraceSize;
    }

    /**
     * Sets the maximum literal size to output to the protocol trace log.
     *
     * @param size the maximum literal trace size in bytes
     */
    public void setMaxLiteralTraceSize(int size) {
        maxLiteralTraceSize = size;
    }

    /**
     * Returns the directory to use for temporary literal storage. If a literal
     * is received that exceeds {@link @getMaxLiteralMemSize} then it will be
     * temporarily stored in this directory. The default directory is the
     * value of the system property <tt>java.io.tmpdir</tt>.
     *
     * @return the literal data directory
     */
    public File getLiteralDataDir() {
        return literalDataDir;
    }

    /**
     * Sets the directory to use for temporary literal storage.
     *
     * @param dir the literal data directory
     */
    public void setLiteralDataDir(File dir) {
        literalDataDir = dir;
    }
}
