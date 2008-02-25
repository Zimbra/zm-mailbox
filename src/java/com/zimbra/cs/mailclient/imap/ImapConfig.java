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

import java.io.File;

/**
 * IMAP client configuration settings.
 */
public class ImapConfig extends MailConfig {
    private int mMaxLiteralMemSize = DEFAULT_MAX_LITERAL_MEM_SIZE;
    private File mLiteralDataDir = new File(System.getProperty("java.io.tmpdir"));
    
    public static final String PROTOCOL = "imap";

    public static final int DEFAULT_PORT = 143;
    public static final int DEFAULT_SSL_PORT = 993;
    public static final int DEFAULT_MAX_LITERAL_MEM_SIZE = 8 * 1024 * 1024;
    
    public ImapConfig() {}

    public ImapConfig(String host, boolean sslEnabled) {
        mHost = host;
        isSSLEnabled = sslEnabled;
    }

    public String getProtocol() { return PROTOCOL; }

    public int getPort() {
        if (mPort != -1) return mPort;
        return isSSLEnabled ? DEFAULT_SSL_PORT : DEFAULT_PORT;
    }
    
    public void setMaxLiteralMemSize(int size) {
        mMaxLiteralMemSize = size;
    }

    public int getMaxLiteralMemSize() {
        return mMaxLiteralMemSize;
    }

    public void setLiteralDataDir(File dir) {
        mLiteralDataDir = dir;
    }

    public File getLiteralDataDir() {
        return mLiteralDataDir;
    }
}
