package com.zimbra.cs.mailtest;

import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashSet;
import java.util.Set;

public class ImapClient extends MailClient {
    private String mTag;
    private String mStatus;
    private String mMessage;
    private Set<String> mCapability = new HashSet<String>();
    private boolean mReceivedBYE;

    private int mCount;

    public static final int DEFAULT_PORT = 143;
    public static final int DEFAULT_SSL_PORT = 993;

    private static final String TAG_PREFIX = "C";
    private static final String TAG_FORMAT = "C%02d";
    private static final String CAPABILITY = "CAPABILITY";
    private static final String BYE = "BYE";
    
    private static final String STATUS_OK = "OK";
    private static final String STATUS_NO = "NO";
    private static final String STATUS_BAD = "BAD";

    public ImapClient() {}

    public void connect() throws IOException {
        if (mPort == -1) {
            mPort = mSslEnabled ? DEFAULT_SSL_PORT : DEFAULT_PORT;
        }
        super.connect();
    }
    
    protected boolean processGreeting() throws IOException {
        processLine(recvLine());
        return STATUS_OK.equals(mStatus);
    }
    
    protected boolean sendLogin() throws IOException {
        return sendCommand("LOGIN " + mAuthenticationId + " " + mPassword);
    }

    public boolean sendLogout() throws IOException {
        return sendCommand("LOGOUT");
    }

    protected boolean sendAuthenticate() throws IOException {
        return sendCommand("AUTHENTICATE " + mMechanism);
    }

    protected boolean sendStartTLS() throws IOException {
        return sendCommand("STARTTLS");
    }

    public String getProtocol() {
        return "imap";
    }

    public boolean sendCommand(String command) throws IOException {
        mTag = createTag();
        mStatus = null;
        mMessage = null;
        sendLine(mTag + ' ' + command);
        try {
            while (mStatus == null) {
                processLine(recvLine());
            }
        } catch (EOFException e) {
            // As long as we received a BYE before the connection was closed,
            // fail the command but do not rethrow EOFException.
            if (mReceivedBYE) return false;
            throw e;
        }
        return STATUS_OK.equals(mStatus);
    }

    public String getMessage() {
        return mMessage;
    }
    
    private void processLine(String line) throws IOException {
        if (line.startsWith("+ ")) {
            processContinuation(line);
        } else if (line.startsWith("* ")) {
            processUntagged(line);
        } else if (line.startsWith(TAG_PREFIX)) {
            processTagged(line);
        } else {
            throw new IOException("Invalid server response line: " + line);
        }
    }

    private void processUntagged(String line) {
        line = line.substring(2).trim();
        int i = line.indexOf(' ');
        String s = i != -1 ? line.substring(0, i) : line;
        mMessage = i != -1 ? line.substring(i + 1) : null;
        if (STATUS_OK.equals(s) || STATUS_NO.equals(s) || STATUS_BAD.equals(s)) {
            mStatus = s;
        } else if (CAPABILITY.equals(s)) {
            mCapability.clear();
            if (mMessage != null) {
                mCapability.addAll(Arrays.asList(mMessage.split("\\s")));
            }
        } else if (BYE.equals(s)) {
            mReceivedBYE = true;
        }
    }

    private void processTagged(String line) throws IOException {
        String tag = getTag(line);
        if (!tag.equals(mTag)) {
            throw new IOException("Unexpected tagged reponse: " + line);
        }
        String s = line.substring(tag.length()).trim();
        int i = s.indexOf(' ');
        if (i == -1) {
            throw new IOException("Missing status in tagged response: " + line);
        }
        mStatus = s.substring(0, i);
        mMessage = s.substring(i).trim();
    }

    private static String getTag(String line) throws IOException {
        int i = line.indexOf(' ');
        if (i == -1) {
            throw new IOException("Untagged response: " + line);
        }
        return line.substring(0, i);
    }
    
    private boolean isMechanismSupported(String mechanism) {
        return mCapability.contains("AUTH=" + mechanism);
    }
    
    private String createTag() {
        Formatter fmt = new Formatter();
        fmt.format(TAG_FORMAT, mCount++);
        return fmt.toString();
    }
}
