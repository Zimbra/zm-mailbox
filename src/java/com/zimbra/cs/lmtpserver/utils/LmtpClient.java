/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

package com.zimbra.cs.lmtpserver.utils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.lmtpserver.LmtpProtocolException;
import com.zimbra.cs.tcpserver.TcpServerInputStream;

public class LmtpClient {

    public static enum Protocol { LMTP, SMTP };

    private Protocol mProtocol;
	private Socket mConnection;
	private String mGreetname;
	private TcpServerInputStream mIn;
	private BufferedOutputStream mOut;
	private boolean mNewConnection;
	private String mResponse;
	private boolean mWarnOnRejectedRecipients = true;

	public LmtpClient(String host, int port, Protocol proto) throws IOException {
	    if (proto != null)
            mProtocol = proto;
        else
            mProtocol = Protocol.LMTP;
		mGreetname = LC.zimbra_server_hostname.value();

		mConnection = new Socket(host, port);
		mOut = new BufferedOutputStream(mConnection.getOutputStream());
		mIn = new TcpServerInputStream(mConnection.getInputStream());
		
		mNewConnection = true;
	}

    public LmtpClient(String host, int port) throws IOException {
        this(host, port, Protocol.LMTP);
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
	
	private static final byte[] lineSeparator = { '\r', '\n' };
	
	private void sendLine(String line, boolean flush) throws IOException {
		if (mTrace) {
			trace("CLI: " + line);
		}
		mOut.write(line.getBytes("iso-8859-1"));
		mOut.write(lineSeparator);
		if (flush) mOut.flush();
	}

	private void sendLine(String line) throws IOException {
		sendLine(line, true);
	}
	
	private boolean replyOk() throws LmtpProtocolException, IOException {
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
    throws IOException, LmtpProtocolException {
        return sendMessage(new ByteArrayInputStream(msg), new String[] { recipient }, sender, logLabel);
    }

    public boolean sendMessage(InputStream msgStream, String recipient, String sender, String logLabel)
    throws IOException, LmtpProtocolException {
        return sendMessage(msgStream, new String[] { recipient }, sender, logLabel);
    }
    
    /**
     * Sends a MIME message.
     * @param msgStream the message body
     * @param recipients recipient email addresses
     * @param sender sender email address
     * @param logLabel context string used for logging status
     * @return <code>true</code> if the message was successfully delivered to all recipients
     */
    public boolean sendMessage(InputStream msgStream, String[] recipients, String sender, String logLabel)
        throws IOException, LmtpProtocolException 
    {
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
		} else {
			sendLine("RSET");
			if (!replyOk()) {
				throw new LmtpProtocolException(mResponse);
			}
		}
		
		sendLine("MAIL FROM:<" + sender + ">");
		if (!replyOk()) {
			throw new LmtpProtocolException(mResponse);
		}

        List<String> acceptedRecipients = new ArrayList<String>(recipients.length);
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
		BufferedReader br = new BufferedReader(new InputStreamReader(msgStream, "iso-8859-1")); 
		String line;
		while ((line = br.readLine()) != null) {
			if (line.length() > 0 && line.charAt(0) == '.') {
				if (line.length() > 1 && line.charAt(1) == '.') {
					// don't have to apply transparency
				} else {
					line = "." + line;
				}
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
    
    private void warn(String s)  { System.err.println("[warn] "  + s); System.err.flush(); }
	private void error(String s) { System.err.println("[error] " + s); System.err.flush(); }
	private void info(String s)  { System.out.println("[info] "  + s); System.out.flush(); }
	private void trace(String s) { System.out.println("[trace] " + s); System.out.flush(); }
}
