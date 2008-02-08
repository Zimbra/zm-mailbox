package com.zimbra.cs.mailclient.pop3;

import com.zimbra.cs.mailclient.MailClient;
import com.zimbra.cs.mailclient.MailException;

import javax.security.auth.login.LoginException;
import java.io.IOException;

public class Pop3Client extends MailClient {
    public static final int DEFAULT_PORT = 110;
    public static final int DEFAULT_SSL_PORT = 995;

    private static final String STATUS_OK = "+OK";
    private static final String STATUS_ERR = "-ERR";
    
    public Pop3Client() {}

    public Pop3Client(String host, int port) {
        super(host, port);
    }
    
    public void connect() throws IOException {
        if (mPort == -1) {
            mPort = mSslEnabled ? DEFAULT_SSL_PORT : DEFAULT_PORT;
        }
        super.connect();
    }
    
    protected void processGreeting() throws IOException {
        processLine(readLine());
        if (!STATUS_OK.equals(mStatus)) {
            throw new MailException("Expected greeting, but got: " + mResponse);
        }
    }

    public void login() throws IOException {
        checkCredentials();
        sendCommand("USER", mAuthenticationId);
        sendCommand("PASS", mPassword);
    }

    public void logout() throws IOException {
        sendCommand("QUIT");
    }
    
    protected void sendAuthenticate(boolean ir) throws LoginException, IOException {
        StringBuffer sb = new StringBuffer(mMechanism);
        if (ir) {
            byte[] response = mAuthenticator.getInitialResponse();
            sb.append(' ').append(encodeBase64(response));
        }
        try {
            sendCommand("AUTH", sb.toString());
        } catch (MailException e) {
            if (STATUS_ERR.equals(mStatus)) {
                throw new LoginException(getResponse());
            }
            throw e;
        }
    }

    protected void sendStartTLS() throws IOException {
        sendCommand("STLS");
    }

    public void selectFolder(String folder) {
        if (!"INBOX".equals(folder)) {
            throw new IllegalArgumentException("Can only select INBOX folder");
        }
    }
    
    public String getProtocol() {
        return "pop3";
    }
    
    public void sendCommand(String cmd, String args) throws IOException {
        mStatus = null;
        mResponse = null;
        String line = cmd;
        if (args != null) line += " " + args;
        writeLine(line);
        while (mStatus == null) {
            processLine(readLine());
        }
        if (!STATUS_OK.equals(mStatus)) {
            throw new MailException(cmd + " failed: " + mResponse);
        }
    }

    private void processLine(String line) throws IOException {
        if (line.startsWith("+ ")) {
            processContinuation(line);
            return;
        }
        int i = line.indexOf(' ');
        if (i == -1) {
            mStatus = line;
            mResponse = "";
        } else {
            mStatus = line.substring(0, i);
            mResponse = line.substring(i).trim();
        }
    }
}
