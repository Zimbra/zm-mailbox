/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.common.lmtp;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import com.google.common.collect.Lists;
import com.zimbra.common.io.TcpServerInputStream;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.net.TrustManagers;
import com.zimbra.common.util.CharsetUtil;

public class LmtpClient {

    public static enum Protocol { LMTP, SMTP };
    private static String STARTTLS = "STARTTLS";

    private Protocol mProtocol;
    private Socket mConnection;
    private String mGreetname;
    private TcpServerInputStream mIn;
    private BufferedOutputStream mOut;
    private boolean mNewConnection;
    private String mResponse;
    private boolean mWarnOnRejectedRecipients = true;
    private boolean skipTLSCertValidation = false;
    
    public LmtpClient(String host, int port) throws IOException {
    	//By default don't skip TLS server cert validation
        this(host, port, Protocol.LMTP, false);
    }

    public LmtpClient(String host, int port, boolean skipTLSCertValidation) throws IOException {
        this(host, port, Protocol.LMTP, skipTLSCertValidation);
    }
    
    public LmtpClient(String host, int port, Protocol proto, boolean skipTLSCertValidation) throws IOException {
        if (proto != null)
            mProtocol = proto;
        else
            mProtocol = Protocol.LMTP;
        mGreetname = LC.zimbra_server_hostname.value();
        mConnection = new Socket(host, port);
        mOut = new BufferedOutputStream(mConnection.getOutputStream());
        mIn = new TcpServerInputStream(mConnection.getInputStream());
        mNewConnection = true;
        this.skipTLSCertValidation = skipTLSCertValidation;
    }

   

    public void warnOnRejectedRecipients(boolean yesno) {
        mWarnOnRejectedRecipients = yesno;
    }

    public void close() {
        try {
            sendLine("QUIT");
            mConnection.close();
        } catch (IOException ioe) {
            warn("IOException closing connection: " + ioe.getMessage());
        }
        mConnection = null;
    }

    // This method is used only for testing
    public void abruptClose() {
        try {
            mConnection.close();
        } catch (IOException ioe) {
            info("IOException closing connection: " + ioe.getMessage());
        }
        mConnection = null;
    }

    private static final byte[] lineSeparator = { '\r', '\n' };

    public void sendLine(String line, boolean flush) throws IOException {
        if (mTrace) {
            trace("CLI: " + line);
        }
        mOut.write(line.getBytes("iso-8859-1"));
        mOut.write(lineSeparator);
        if (flush) mOut.flush();
    }

    public void sendLine(String line) throws IOException {
        sendLine(line, true);
    }

    public boolean replyOk() throws LmtpProtocolException, IOException {
        boolean positiveReplyCode = false;
        StringBuffer sb = new StringBuffer();

        while (true) {
            String response = mIn.readLine();
            if (response == null) {
                break;
            }
            if (mTrace) {
                trace("SRV: " + response);
            }
            if (response.length() < 3) {
                throw new LmtpProtocolException("response too short: " + response);
            }
            if (response.length() > 3 && response.charAt(3) == '-') {
                sb.append(response);
            } else {
                sb.append(response);
                if (response.charAt(0) >= '1' && response.charAt(0) <= '3') {
                    positiveReplyCode = true;
                }
                break;
            }
        }

        mResponse = sb.toString();
        return positiveReplyCode;
    }

    public boolean sendMessage(byte[] msg, String recipient, String sender, String logLabel)
    throws IOException, LmtpProtocolException, LmtpClientException {
        return sendMessage(new ByteArrayInputStream(msg), new String[] { recipient }, sender, logLabel, (long) msg.length);
    }

    public boolean sendMessage(InputStream msgStream, String recipient, String sender, String logLabel)
    throws IOException, LmtpProtocolException, LmtpClientException {
        return sendMessage(msgStream, new String[] { recipient }, sender, logLabel, null);
    }

    public boolean sendMessage(InputStream msgStream, String recipient, String sender, String logLabel, long size)
    throws IOException, LmtpProtocolException, LmtpClientException {
        return sendMessage(msgStream, new String[] { recipient }, sender, logLabel, size);
    }

    public boolean sendMessage(InputStream msgStream, String[] recipients, String sender, String logLabel, Long size)
    throws IOException, LmtpProtocolException, LmtpClientException {
        return sendMessage(msgStream, Lists.newArrayList(recipients), sender, logLabel, size);
    }

    /**
     * Sends a MIME message.
     * @param msgStream the message body
     * @param recipients recipient email addresses
     * @param sender sender email address
     * @param logLabel context string used for logging status
     * @param size the size of the data or <tt>null</tt> if not specified
     * @return <code>true</code> if the message was successfully delivered to all recipients
     * @throws LmtpClientException 
     */
    public boolean sendMessage(InputStream msgStream, Iterable<String> recipients, String sender, String logLabel, Long size)
    throws IOException, LmtpProtocolException, LmtpClientException {
        long start = System.currentTimeMillis();
        if (mNewConnection) {
            mNewConnection = false;
            
            // swallow the greeting
            if (!replyOk()) {
                throw new LmtpProtocolException(mResponse);
            }

            if (Protocol.SMTP.equals(mProtocol))
                sendLine("EHLO " + mGreetname);
            else
                sendLine("LHLO " + mGreetname);
            if (!replyOk()) {
                throw new LmtpProtocolException(mResponse);
            }
            if (serverSupportsStartTls()) {
	            startTLS();
	            if (Protocol.SMTP.equals(mProtocol))
	                sendLine("EHLO " + mGreetname);
	            else
	                sendLine("LHLO " + mGreetname);
	            if (!replyOk()) {
	                throw new LmtpProtocolException(mResponse);
	            }
            }
          
        } else {
            sendLine("RSET");
            if (!replyOk()) {
                throw new LmtpProtocolException(mResponse);
            }
        }

        String sizeString = "";
        if (size != null) {
            sizeString = " SIZE=" + size;
        }
        sendLine("MAIL FROM:<" + sender + ">" + sizeString);
        if (!replyOk()) {
            throw new LmtpProtocolException(mResponse);
        }

        List<String> acceptedRecipients = Lists.newArrayList();
        for (String recipient : recipients) {
            sendLine("RCPT TO:<" + recipient + ">");
            if (replyOk()) {
                acceptedRecipients.add(recipient);
            } else {
                if (mWarnOnRejectedRecipients) {
                    warn("Recipient `" + recipient + "' rejected");
                }
            }
        }

        sendLine("DATA");
        if (!replyOk()) {
            throw new LmtpProtocolException(mResponse);
        }
        // Classic case of lazy programmer here.  We read 8bit data from the file.
        // But we want to treat it as String for a little while because we want to
        // apply transparency and BufferedReader.getLine() is handy.  This conversion
        // here has a reverse with getBytes(charset) elsewhere in sendLine().
        BufferedReader br = new BufferedReader(new InputStreamReader(msgStream, CharsetUtil.ISO_8859_1));
        String line;
        while ((line = br.readLine()) != null) {
            /**
             *  http://tools.ietf.org/html/rfc2821#section-4.5.2 Transparency:
             *      Before sending a line of mail text, the SMTP client checks the first character of the line.  If it
             *      is a period, one additional period is inserted at the beginning of the line.
             */
            if (line.length() > 0 && line.charAt(0) == '.') {
                line = "." + line;
            }
            sendLine(line, false);
        }
        sendLine("", false);
        sendLine(".");

        boolean allDelivered = true;
        for (String recipient : acceptedRecipients) {
            if (replyOk()) {
                long elapsed = System.currentTimeMillis() - start;
                if (!mQuiet) {
                    info(mProtocol + " delivery OK msg=" + logLabel + " rcpt=" + recipient + " elapsed=" + elapsed + "ms");
                }
            } else {
                allDelivered = false;
                error("Delivery failed msg=" + logLabel + " rcpt=" + recipient + " response=" + mResponse);
            }
        }
        return allDelivered;
    }

    private boolean mQuiet;
    private boolean mTrace;

    public void quiet(boolean onOff) {
        mQuiet = onOff;
    }

    public void trace(boolean onOff) {
        mTrace = onOff;
    }

    public String getResponse() {
        return (mResponse);
    }
    
    private boolean serverSupportsStartTls(){
    	return getResponse().contains(STARTTLS);
    }
    
    public void startTLS() throws IOException, LmtpProtocolException, LmtpClientException{
         sendLine(STARTTLS, true);
         if (!replyOk()) {
             throw new LmtpProtocolException(mResponse);
         }
         SSLSocket sock = newSSLSocket(mConnection);
         sock.startHandshake();
         mOut = new BufferedOutputStream(sock.getOutputStream());
         mIn = new TcpServerInputStream(sock.getInputStream());
    }

    private SSLSocket newSSLSocket(Socket sock) throws IOException, LmtpClientException {
        try {
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null,
                    new TrustManager[] { getTrustManager() },
                    new java.security.SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext
                    .getSocketFactory();
            return (SSLSocket) sslSocketFactory.createSocket(sock, sock
                    .getInetAddress().getHostName(), sock.getPort(), false);
        } catch (KeyManagementException e) {
            throw new LmtpClientException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new LmtpClientException(e);
        }
    }

    private TrustManager getTrustManager() {
        if (skipTLSCertValidation) {
            info("Server certificate validation will be skipped in TLS handshake.");
            return TrustManagers.dummyTrustManager();
        } else {
            return TrustManagers.customTrustManager();	        
        }
    }



    private void warn(String s)  { System.err.println("[warn] "  + s); System.err.flush(); }
    private void error(String s) { System.err.println("[error] " + s); System.err.flush(); }
    private void info(String s)  { System.out.println("[info] "  + s); System.out.flush(); }
    private void trace(String s) { System.out.println("[trace] " + s); System.out.flush(); }
}
