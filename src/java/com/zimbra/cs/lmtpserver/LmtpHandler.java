/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.lmtpserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.Socket;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.tcpserver.ProtocolHandler;
import com.zimbra.cs.util.Config;

public class LmtpHandler extends ProtocolHandler {

    // Connection specific data
	private LmtpServer mServer;
	private LmtpInputStream mInputStream;
	private LmtpWriter mWriter;
	private String mRemoteAddress;
	private String mLhloArg;
	
	// Message specific data
	private LmtpEnvelope mEnvelope;
	private String mCurrentCommandLine;

	LmtpHandler(LmtpServer server) {
		super(server);
		mServer = server;
    }

	protected void notifyIdleConnection() {
		sendResponse("421 " + mServer.getConfigName() + " Timeout exceeded");
	}

	protected boolean setupConnection(Socket connection) throws IOException {
		reset();
		mRemoteAddress = connection.getInetAddress().getHostAddress();
		INFO("connected");

		InputStream is = connection.getInputStream();
		mInputStream = new LmtpInputStream(is);
		
		OutputStream os = connection.getOutputStream();
		mWriter = new LmtpWriter(os);

        boolean allow = Config.userServicesEnabled();
        if (allow) {
            sendResponse(mServer.get220Greeting());
        } else {
            sendResponse("421 4.3.2 User services disabled");
            dropConnection();
        }
        return allow;
	}

	protected boolean authenticate() throws IOException {
		// LMTP doesn't need auth.
		return true;
	}

	protected synchronized void dropConnection() {
		try {
			if (mInputStream != null) {
				mInputStream.close();
				mInputStream = null;
			}
			if (mWriter != null) {
				mWriter.close();
				mWriter = null;
			}
		} catch (IOException e) {
			INFO("exception while closing connection", e);
		}
	}

	protected boolean processCommand() throws IOException {
		mCurrentCommandLine = mInputStream.readLine();
		String cmd = mCurrentCommandLine;
		String arg = null;

		if (cmd == null) {
			INFO("disconnected without quit");
			dropConnection();
			return false;
		}

        if (!Config.userServicesEnabled()) {
            sendResponse(mServer.get421Error());
            dropConnection();
            return false;
        }

		setIdle(false);

		int space = cmd.indexOf(" ");
		if (space > 0) {
			arg = cmd.substring(space + 1); 
			cmd = cmd.substring(0, space);
		}
		
		if (mLog.isDebugEnabled()) {
			DEBUG("command=" + cmd + " arg=" + arg);
		}
				
		if (cmd.length() < 4) {
			doSyntaxError("command too short");
			return true;
		}
		
		int ch = cmd.charAt(0);
		
		// Breaking out of this switch causes a syntax error to be returned
		// So if you process a command then return immediately (even if the
		// command handler reported a syntax error or failed otherwise)
		
		switch (ch) {
		
		case 'l':
		case 'L':
			if ("LHLO".equalsIgnoreCase(cmd)) {
				doLHLO(arg);
				return true;
			}
			break;

		case 'm':
		case 'M':
			if ("MAIL".equalsIgnoreCase(cmd)) {
				final int fromColonLength = 5;
				if (arg.length() < fromColonLength) {
					break; // not enough room to carry "from:"
				}
				String fromColon = arg.substring(0, fromColonLength);
				if (! "FROM:".equalsIgnoreCase(fromColon)) {
					break; // there was no "from:"
				}
				arg = arg.substring(fromColonLength);
				doMAIL(arg);
				return true;
			}
			break;
			
		case 'r':
		case 'R':
			if ("RSET".equalsIgnoreCase(cmd)) {
				doRSET(arg);
				return true;
			}
			if ("RCPT".equalsIgnoreCase(cmd)) {
				final int toColonLength = 3;
				if (arg.length() < toColonLength) {
					break; // not enough room to carry "to:"
				}
				String toColon = arg.substring(0, toColonLength);
				if (! "TO:".equalsIgnoreCase(toColon)) {
					break; // there was no "to:"
				}
				arg = arg.substring(toColonLength);
				doRCPT(arg);
				return true;
			}
			break;
			
		case 'd':
		case 'D':
			if ("DATA".equalsIgnoreCase(cmd)) {
				doDATA(arg);
				return true;
			}
			break;
			
		case 'n':
		case 'N':
			if ("NOOP".equalsIgnoreCase(cmd)) {
				doNOOP();
				return true;
			}
			break;

		case 'q':
		case 'Q':
			if ("QUIT".equalsIgnoreCase(cmd)) {
				doQUIT();
				return false;
			}
			break;
			
		case 'v':
		case 'V':
			if ("VRFY".equalsIgnoreCase(cmd)) {
				doVRFY(arg);
				return true;
			}
			break;
			
		default:
			break;
		}
		doSyntaxError("unknown command");
		return true;
    }

	private void sendResponse(String response) {
		String cl = mCurrentCommandLine != null ? mCurrentCommandLine : "<none>";
		char firstCh = response.charAt(0);
		if (mLog.isDebugEnabled()) {
			DEBUG(response + " (" + cl + ")");
		} else {
			if (firstCh != '2' && firstCh != '3') {	// Log only error cases
				INFO(response + " (" + cl + ")");
			}
		}
		mWriter.println(response);
		mWriter.flush();
	}
	
    private void doSyntaxError(String why) {
	    sendResponse("500 5.5.2 Syntax error");
    }

    private void doNOOP() {
    	sendResponse("250 2.0.0 OK");
    }
    
    private void doQUIT() {
    	sendResponse(mServer.get221Goodbye());
    	INFO("quit from client");
    	dropConnection();
    }

    private void doRSET(String arg) {
    	if (arg != null) {
    		doSyntaxError("paramater supplied to rset");
    		return;
    	}
    	reset();
    	sendResponse("250 2.0.0 OK");
    }

    private void doVRFY(String arg) {
    	if (arg == null || arg.length() == 0) {
    		doSyntaxError("no parameter to vrfy");
    		return;
    	}
    	// RFC 2821, Section 7.3: If a site disables these commands for security
		// reasons, the SMTP server MUST return a 252 response, rather than a
		// code that could be confused with successful or unsuccessful
		// verification.
    	// RFC 1892: X.3.3 System not capable of selected features
    	sendResponse("252 2.3.3 Use RCPT to deliver messages");
    }

    private void doLHLO(String arg) {
    	if (arg == null || arg.length() == 0) {
    		doSyntaxError("no parameter to lhlo");
    		return;
    	}
    	mWriter.println("250-" + mServer.getConfigName());
    	mWriter.println("250-8BITMIME");
    	mWriter.println("250-ENHANCEDSTATUSCODES");
   		if (mServer.getConfigMaxMessageSize() < Integer.MAX_VALUE) {
    			mWriter.println("250-SIZE " + mServer.getConfigMaxMessageSize());
   		} else {
    			mWriter.println("250-SIZE ");
    	}
    	mWriter.println("250 PIPELINING");
    	mWriter.flush();
    	mLhloArg = arg;
    	reset();
    }
    
    private void doMAIL(String arg) {
		if (arg == null || arg.length() == 0) {
			doSyntaxError("no parameter to mail from");
			return;
		}
		
		if (mEnvelope.hasSender()) {
			sendResponse("503 5.5.1 Nested MAIL command");
			return;
		}
		
		LmtpAddress addr = new LmtpAddress(arg, new String[] { "BODY", "SIZE" });
		if (!addr.isValid()) {
		    sendResponse("501 5.5.4 Syntax error in parameters");
		    return;
		}

		LmtpBodyType type = null;
		String body = addr.getParameter("BODY");
		if (body != null) {
			type = LmtpBodyType.getInstance(body);
			if (type == null) {
				sendResponse("501 5.5.4 Syntax error in parameter BODY can not be: " + body);
				return;
			}
		}
		
		int size = 0;
		String sz = addr.getParameter("SIZE");
		if (sz != null) {
			try {
				size = Integer.parseInt(sz);;
			} catch (NumberFormatException nfe) {
				sendResponse("501 5.5.4 Syntax error in parameter SIZE not a number: " + sz);
				return;
			}
		}

		mEnvelope.setSender(addr);
		mEnvelope.setBodyType(type);
		mEnvelope.setSize(size);
		
		sendResponse("250 2.0.0 Sender OK");
    }
    
    private void doRCPT(String arg) {
		if (arg == null || arg.length() == 0) {
			doSyntaxError("no parameter to rcpt to");
			return;
		}
    	
		if (!mEnvelope.hasSender()) {
    		sendResponse("503 5.5.1 Need MAIL command");
    		return;
    	}
    	
		LmtpAddress addr = new LmtpAddress(arg, null);
		if (!addr.isValid()) {
			sendResponse("501 5.5.4 Syntax error in parameters");
			return;
		}
		
		LmtpStatus status = mServer.getConfigBackend().getAddressStatus(addr);

		if (status == LmtpStatus.REJECT) {
			sendResponse("550 5.1.1 No such user here");
			return;
		}
		
		if (status == LmtpStatus.TRYAGAIN) {
		    sendResponse("450 4.2.1 Mailbox disabled, not accepting messages");
		    return;
		}
		
		mEnvelope.addRecipient(addr);
		sendResponse("250 2.1.5 Recipient OK");
    }
    
    private void reset() {
    	// Reset must not change any earlier LHLO argument
    	mEnvelope = new LmtpEnvelope();
    	mCurrentCommandLine = null; 
    }
    
    private void doDATA(String arg) throws IOException {
    	if (!mEnvelope.hasRecipients()) {
    		sendResponse("503 5.5.1 No recipients");
    		return;
    	}

    	sendResponse("354 End data with <CR><LF>.<CR><LF>");
    	
    	byte[] data = mInputStream.readMessage(mEnvelope.getSize());
    	if (data == null || data.length == 0) {
    		sendResponse("554 5.6.0 Empty message not allowed");
    		return;
    	}

    	if (mLog.isDebugEnabled()) {
    		DEBUG("size hint=" + mEnvelope.getSize() + " read data=" + data.length);
    	}

    	if (data.length > mServer.getConfigMaxMessageSize()) {
    		sendResponse("552 5.2.3 Message size " + data.length + " exceeds allowed size (" + mServer.getConfigMaxMessageSize() + ")");
    		return;
    	}
    	
    	// TODO cleanup maybe add Date if not present
    	// TODO cleanup maybe add From header from envelope is not present
    	// TODO add Received header for this lmtp transaction
    	// TODO should there be a too many recipients test?

        int dataLength = data.length;
        int numRecipients = mEnvelope.getRecipients().size();
        mServer.mLmtpRcvdMsgs.increment();
        mServer.mLmtpRcvdData.increment(dataLength);
        mServer.mLmtpRcvdRcpt.increment(numRecipients);
    	
        mServer.getConfigBackend().deliver(mEnvelope, data);

        int numDelivered = 0;
    	List recipients = mEnvelope.getRecipients();
    	for (Iterator iter = recipients.iterator(); iter.hasNext();) {
    		LmtpAddress recipient = (LmtpAddress)iter.next();
            LmtpStatus status = recipient.getDeliveryStatus();
    		if (status == LmtpStatus.ACCEPT) {
                numDelivered++;
    			sendResponse("250 2.1.5 OK");
    		} else if (status == LmtpStatus.OVERQUOTA) {
                sendResponse("552 5.2.2 Over quota");
            } else if (status == LmtpStatus.TRYAGAIN) {
                sendResponse("451 4.0.0 Temporary message delivery failure try again");
            } else {
    			sendResponse("554 5.0.0 Permanent message delivery failure"); 
    		}
    	}
        
        mServer.mLmtpDlvdMsgs.increment(numDelivered);
        mServer.mLmtpDlvdData.increment(numDelivered * dataLength);
        
    	reset();
    }
    
	/*
	 * Logging support so we log client info on each log line
	 */ 
	private static Log mLog = LogFactory.getLog(LmtpHandler.class);
	
	private StringBuffer withClientInfo(String message) {
		int length = 64;
		if (message != null) length += message.length();
		return new StringBuffer(length).append("[").append(mRemoteAddress).append("] ").append(message);
	}
	
    private void INFO(String message, Throwable e) {
    	if (mLog.isInfoEnabled()) mLog.info(withClientInfo(message), e); 
    }
    
    private void INFO(String message) {
    	if (mLog.isInfoEnabled()) mLog.info(withClientInfo(message));
    }

    private void DEBUG(String message, Throwable e) {
    	if (mLog.isDebugEnabled()) mLog.debug(withClientInfo(message), e);
    }

    private void DEBUG(String message) {
    	if (mLog.isDebugEnabled()) mLog.debug(withClientInfo(message));
    }

    private void WARN(String message, Throwable e) {
    	if (mLog.isWarnEnabled()) mLog.warn(withClientInfo(message), e);
    }

    private void WARN(String message) {
    	if (mLog.isWarnEnabled()) mLog.warn(withClientInfo(message));
    }
}
