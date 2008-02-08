package com.zimbra.cs.mailclient.imap;

import com.zimbra.cs.mailclient.MailConfig;

import java.io.File;

/**
 * IMAP client configuration settings.
 */
public class ImapConfig extends MailConfig {
    private int mMaxLiteralMemSize = DEFAULT_MAX_LITERAL_MEM_SIZE;
    private File mLiteralDataDir = new File(System.getProperty("java.io.tmpdir"));
    
    public static final int DEFAULT_PORT = 143;
    public static final int DEFAULT_SSL_PORT = 993;
    public static final int DEFAULT_MAX_LITERAL_MEM_SIZE = 8 * 1024 * 1024;

    public ImapConfig() {
        setPort(DEFAULT_PORT);
    }

    public ImapConfig(String host, boolean sslEnabled) {
        this(host, sslEnabled ? DEFAULT_PORT : DEFAULT_SSL_PORT, sslEnabled);
    }

    public ImapConfig(String host, int port, boolean sslEnabled) {
        setHost(host);
        setPort(port);
        setSslEnabled(sslEnabled);
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
