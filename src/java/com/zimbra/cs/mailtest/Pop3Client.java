package com.zimbra.cs.mailtest;

import java.io.IOException;

public class Pop3Client extends MailClient {
    private String mStatus;
    private String mMessage;
    
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
    
    protected boolean processGreeting() throws IOException {
        processLine(readLine());
        return STATUS_OK.equals(mStatus);
    }

    protected boolean sendLogin() throws IOException {
        return sendCommand("USER " + mAuthenticationId) &&
               sendCommand("PASS " + mPassword);
    }

    protected boolean sendLogout() throws IOException {
        return sendCommand("QUIT");
    }
    
    protected boolean sendAuthenticate(boolean ir) throws IOException {
        StringBuffer sb = new StringBuffer("AUTH ");
        sb.append(mMechanism);
        if (ir) {
            byte[] response = mAuthenticator.getInitialResponse();
            sb.append(' ').append(encodeBase64(response));
        }
        return sendCommand(sb.toString());
    }

    protected boolean sendStartTLS() throws IOException {
        return sendCommand("STLS");
    }
    
    public String getProtocol() {
        return "pop3";
    }
    
    public boolean sendCommand(String command) throws IOException {
        mStatus = null;
        mMessage = null;
        writeLine(command);
        while (mStatus == null) {
            processLine(readLine());
        }
        return STATUS_OK.equals(mStatus);
    }

    public String getMessage() {
        return mMessage;
    }
    
    private void processLine(String line) throws IOException {
        if (line.startsWith("+ ")) {
            processContinuation(line);
            return;
        }
        int i = line.indexOf(' ');
        if (i == -1) {
            mStatus = line;
            mMessage = "";
        } else {
            mStatus = line.substring(0, i);
            mMessage = line.substring(i).trim();
        }
    }
}
