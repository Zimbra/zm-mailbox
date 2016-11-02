package com.zimbra.cs.imap;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.commons.io.output.ByteArrayOutputStream;

import com.google.common.base.Charsets;

class MockImapHandler extends ImapHandler {

    MockImapHandler() {
        super(new ImapConfig(false));
        output = new ByteArrayOutputStream();
    }

    @Override
    String getRemoteIp() {
        return "127.0.0.1";
    }

    @Override
    void sendLine(String line, boolean flush) throws IOException {
        output.write(line.getBytes(Charsets.UTF_8));
    }

    @Override
    void dropConnection(boolean sendBanner) {
    }

    @Override
    void close() {
    }

    @Override
    void enableInactivityTimer() throws IOException {
    }

    @Override
    void completeAuthentication() throws IOException {
    }

    @Override
    boolean doSTARTTLS(String tag) throws IOException {
        return false;
    }

    @Override
    InetSocketAddress getLocalAddress() {
        return new InetSocketAddress("localhost", 0);
    }
}