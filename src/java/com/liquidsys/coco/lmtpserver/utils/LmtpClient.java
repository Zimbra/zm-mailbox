package com.liquidsys.coco.lmtpserver.utils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.liquidsys.coco.lmtpserver.LmtpInputStream;
import com.liquidsys.coco.lmtpserver.LmtpProtocolException;
import com.liquidsys.coco.localconfig.LC;

public class LmtpClient {

	private String mHost;
	private int mPort;
	private Socket mConnection;
	private String mGreetname;
	private LmtpInputStream mIn;
	private BufferedOutputStream mOut;
	private boolean mNewConnection;
	private String mResponse;
	private boolean mWarnOnRejectedRecipients = true;
	
	public LmtpClient(String host, int port) throws IOException {
		mHost = host;
		mPort = port;
		mGreetname = LC.liquid_server_hostname.value();

		mConnection = new Socket(host, port);
		mOut = new BufferedOutputStream(mConnection.getOutputStream());
		mIn = new LmtpInputStream(mConnection.getInputStream());
		
		mNewConnection = true;
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
	
    public boolean sendMessage(byte[] msg, List /*<String>*/ recipients, String sender, String logLabel)
        throws IOException, LmtpProtocolException 
    {
        long start = System.currentTimeMillis();
		if (mNewConnection) {
			mNewConnection = false;

			// swallow the greeting
			if (!replyOk()) {
				throw new LmtpProtocolException(mResponse);
			}
			
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

        List /* String */ acceptedRecipients = new ArrayList();
		for (Iterator iter = recipients.iterator(); iter.hasNext();) {
			String recipient = (String) iter.next();
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
		BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(msg), "iso-8859-1")); 
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
        for (Iterator iter = acceptedRecipients.iterator(); iter.hasNext();) {
            String recipient = (String)iter.next();
			if (replyOk()) {
                long elapsed = System.currentTimeMillis() - start;
                if (!mQuiet) {
                    info("Delivery OK msg=" + logLabel + " rcpt=" + recipient + " elapsed=" + elapsed + "ms");
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
	
    private void warn(String s)  { System.err.println("[warn] "  + s); System.err.flush(); }
	private void error(String s) { System.err.println("[error] " + s); System.err.flush(); }
	private void info(String s)  { System.out.println("[info] "  + s); System.out.flush(); }
	private void trace(String s) { System.out.println("[trace] " + s); System.out.flush(); }
}
