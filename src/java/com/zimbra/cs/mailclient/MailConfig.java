package com.zimbra.cs.mailclient;

/**
 * Mail client configuration settings.
 */
public class MailConfig {
    private String mHost;
    private int mPort;
    private boolean mSslEnabled;

    public boolean isSslEnabled() {
        return mSslEnabled;
    }

    public void setSslEnabled(boolean enabled) {
        mSslEnabled = enabled;
    }

    public String getHost() {
        return mHost;
    }

    public void setHost(String host) {
        this.mHost = host;
    }

    public int getPort() {
        return mPort;
    }

    public void setPort(int port) {
        mPort = port;
    }
}
