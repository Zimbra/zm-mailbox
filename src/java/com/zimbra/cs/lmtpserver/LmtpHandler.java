/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.lmtpserver;

import java.io.IOException;
import java.util.Date;
import java.net.InetAddress;

import javax.mail.internet.MailDateFormat;
import javax.mail.internet.MimeUtility;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.tcpserver.ProtocolHandler;
import com.zimbra.cs.util.Config;
import com.zimbra.cs.store.Blob;

public abstract class LmtpHandler extends ProtocolHandler {
    // Connection specific data
    protected LmtpConfig mConfig;
    protected LmtpWriter mWriter;
    protected String mRemoteAddress;
    protected String mRemoteHostname;
    private String mLhloArg;

    // Message specific data
    protected LmtpEnvelope mEnvelope;
    private String mCurrentCommandLine;

    LmtpHandler(LmtpServer server) {
	super(server instanceof TcpLmtpServer ? (TcpLmtpServer) server : null);
	mConfig = server.getConfig();
    }

    protected boolean setupConnection(InetAddress remoteAddr) {
	mRemoteAddress = remoteAddr.getHostAddress();
	if (StringUtil.isNullOrEmpty(mRemoteAddress)) {
	    // Logging for bug 47643.
	    ZimbraLog.lmtp.info("Unable to determine client IP address.");
	}
	mRemoteHostname = remoteAddr.getHostName();
	if (mRemoteHostname == null || mRemoteHostname.length() == 0) {
	    mRemoteHostname = mRemoteAddress;
	}
	ZimbraLog.addIpToContext(mRemoteAddress);
	ZimbraLog.lmtp.debug("connected");
	if (!Config.userServicesEnabled()) {
	    sendReply(LmtpReply.SERVICE_DISABLED);
	    dropConnection();
	    return false;
	}
	sendReply(LmtpReply.GREETING);
	return true;
    }

    protected void notifyIdleConnection() {
	sendReply(LmtpReply.TIMEOUT);
    }

    protected boolean authenticate() {
	// LMTP doesn't need auth.
	return true;
    }

    protected boolean processCommand(String cmd) throws IOException {
	mCurrentCommandLine = cmd;
	String arg = null;

	if (cmd == null) {
	    ZimbraLog.lmtp.info("disconnected without quit");
	    dropConnection();
	    return false;
	}

	if (!Config.userServicesEnabled()) {
	    sendReply(LmtpReply.SERVICE_DISABLED);
	    dropConnection();
	    return false;
	}

	setIdle(false);

	int i = cmd.indexOf(' ');
	if (i > 0) {
	    arg = cmd.substring(i + 1); 
	    cmd = cmd.substring(0, i);
	}

	ZimbraLog.lmtp.debug("command=%s arg=%s", cmd, arg);

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

    private void sendReply(LmtpReply reply) {
	String cl = mCurrentCommandLine != null ? mCurrentCommandLine : "<none>";
	if (ZimbraLog.lmtp.isDebugEnabled()) {
	    ZimbraLog.lmtp.debug("%s (%s)", reply.toString(), cl);
	} else {
	    if (!reply.success()) {
		ZimbraLog.lmtp.info("%s (%s)", reply.toString(), cl);
	    }
	}
	mWriter.println(reply.toString());
	mWriter.flush();
    }

    private void doSyntaxError(String why) {
	sendReply(LmtpReply.SYNTAX_ERROR);
    }

    private void doNOOP() {
	sendReply(LmtpReply.OK);
    }

    private void doQUIT() {
	sendReply(LmtpReply.BYE);
	ZimbraLog.lmtp.debug("quit from client");
	dropConnection();
    }

    private void doRSET(String arg) {
	if (arg != null) {
	    doSyntaxError("parameter supplied to rset");
	    return;
	}
	reset();
	sendReply(LmtpReply.OK);
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
	sendReply(LmtpReply.USE_RCPT_INSTEAD);
    }

    private void doLHLO(String arg) {
	mLhloArg = arg;
	if (arg == null || arg.length() == 0) {
	    doSyntaxError("no parameter to lhlo");
	    return;
	}
	mWriter.println("250-" + mConfig.getServerName());
	mWriter.println("250-8BITMIME");
	mWriter.println("250-ENHANCEDSTATUSCODES");
	mWriter.println("250-SIZE ");
	mWriter.println("250 PIPELINING");
	mWriter.flush();
	reset();
    }

    private void doMAIL(String arg) {
	if (arg == null || arg.length() == 0) {
	    doSyntaxError("no parameter to mail from");
	    return;
	}

	if (mEnvelope.hasSender()) {
	    sendReply(LmtpReply.NESTED_MAIL_COMMAND);
	    return;
	}

	LmtpAddress addr = new LmtpAddress(arg, new String[] { "BODY", "SIZE" }, null);
	if (!addr.isValid()) {
	    sendReply(LmtpReply.INVALID_SENDER_ADDRESS);
	    return;
	}

	LmtpBodyType type = null;
	String body = addr.getParameter("BODY");
	if (body != null) {
	    type = LmtpBodyType.getInstance(body);
	    if (type == null) {
		sendReply(LmtpReply.INVALID_BODY_PARAMETER);
		return;
	    }
	}

	int size = 0;
	String sz = addr.getParameter("SIZE");
	if (sz != null) {
	    try {
		size = Integer.parseInt(sz);;
	    } catch (NumberFormatException nfe) {
		sendReply(LmtpReply.INVALID_SIZE_PARAMETER);
		return;
	    }
	}

	mEnvelope.setSender(addr);
	mEnvelope.setBodyType(type);
	mEnvelope.setSize(size);
	sendReply(LmtpReply.SENDER_OK);
    }

    private void doRCPT(String arg) {
	if (arg == null || arg.length() == 0) {
	    doSyntaxError("no parameter to rcpt to");
	    return;
	}

	if (!mEnvelope.hasSender()) {
	    sendReply(LmtpReply.MISSING_MAIL_TO);
	    return;
	}

	LmtpAddress addr = new LmtpAddress(arg, null, mConfig.getMtaRecipientDelimiter());
	if (!addr.isValid()) {
	    sendReply(LmtpReply.INVALID_RECIPIENT_ADDRESS);
	    return;
	}

	LmtpReply reply = mConfig.getLmtpBackend().getAddressStatus(addr);
	if (reply.success()) {
	    if (addr.isOnLocalServer())
            mEnvelope.addLocalRecipient(addr);
        else
            mEnvelope.addRemoteRecipient(addr);
	}
	sendReply(reply);
    }

    protected void reset() {
	// Reset must not change any earlier LHLO argument
	mEnvelope = new LmtpEnvelope();
	mCurrentCommandLine = null;                
    }

    private void doDATA(String arg) throws IOException {
	if (!mEnvelope.hasRecipients()) {
	    sendReply(LmtpReply.NO_RECIPIENTS);
	    return;
	}
	sendReply(LmtpReply.OK_TO_SEND_DATA);
	continueDATA();
    }

    protected abstract void continueDATA() throws IOException;

    protected void processMessageData(Blob blob) throws IOException {
        mConfig.getLmtpBackend().deliver(mEnvelope, blob);
        finishMessageData(blob.getRawSize());
    }
    
    protected void processMessageData(LmtpMessageInputStream in) throws IOException {
	// TODO cleanup: add Date if not present
	// TODO cleanup: add From header from envelope if not present
	// TODO there should be a too many recipients test (for now protected by postfix config)

	mConfig.getLmtpBackend().deliver(mEnvelope, in, mEnvelope.getSize());
        finishMessageData(in.getMessageSize());
    }

    private void finishMessageData(long size) throws IOException {
	int numRecipients = mEnvelope.getRecipients().size();
	ZimbraPerf.COUNTER_LMTP_RCVD_MSGS.increment();
	ZimbraPerf.COUNTER_LMTP_RCVD_BYTES.increment(size);
	ZimbraPerf.COUNTER_LMTP_RCVD_RCPT.increment(numRecipients);

	int numDelivered = 0;
	for (LmtpAddress recipient : mEnvelope.getRecipients()) {
	    LmtpReply reply = recipient.getDeliveryStatus();
	    sendReply(reply);
	    if (reply.success()) {
        	numDelivered++;
	    }
	}

	ZimbraPerf.COUNTER_LMTP_DLVD_MSGS.increment(numDelivered);
	ZimbraPerf.COUNTER_LMTP_DLVD_BYTES.increment(numDelivered * size);

	reset();
    }

    /*
     * Generates the <tt>Return-Path</tt> and <tt>Received</tt> headers
     * for the current incoming message.
     */
    protected String getAdditionalHeaders() {
	StringBuilder headers = new StringBuilder();

	// Assemble Return-Path header
	if (mEnvelope.hasSender()) {
	    String sender = mEnvelope.getSender().getEmailAddress();
	    if (!StringUtil.isNullOrEmpty(sender)) {
		headers.append(String.format("Return-Path: %s\r\n", sender));
	    }
	}

	// Assemble Received header
	String localHostname = "unknown";
	try {
	    localHostname = Provisioning.getInstance().getLocalServer().getName();
	} catch (ServiceException e) {
	    ZimbraLog.lmtp.warn("Unable to determine local hostname", e);
	}
	String timestamp = new MailDateFormat().format(new Date());
	String name = "Received: ";
	String value = String.format("from %s (LHLO %s) (%s) by %s with LMTP; %s",
		mRemoteHostname, mLhloArg, mRemoteAddress, localHostname, timestamp);
	headers.append(name);
	headers.append(MimeUtility.fold(name.length(), value));
	headers.append("\r\n");

	return headers.toString();
    }
}
