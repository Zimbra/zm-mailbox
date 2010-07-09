/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.mime.InternetAddress;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.server.ServerManager;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.stats.ZimbraPerf;

public class DeliveryServlet extends ZimbraServlet {

    private static final long serialVersionUID = 1L;

    public static final String DELIVERY_PATH = "/service/deliver";
    
    public static final String PARAM_FROM = "from";
    public static final String PARAM_TO_PREFIX = "to";
    public static final String PARAM_FOLDER_PREFIX = "folder";
    public static final String PARAM_FLAGS_PREFIX = "flags";
    public static final String PARAM_TAGS_PREFIX = "tags";
    
    public static final String RESPONSE_REPLY_PREFIX = "X-Zimbra-LMTP-Reply-";
    public static final String RESPONSE_DELIVERED = "X-Zimbra-LMTP-Delivered";
    public static final String RESPONSE_PROCESSED = "X-Zimbra-LMTP-Processed";
    
    public static final int MAX_RECIPIENTS = 50;
	
    @Override public void doPut(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {
        doPost(req, resp);
    }
    
    private LmtpAddress toLmtpAddress(String addrParam) {
	InternetAddress ia = new InternetAddress(addrParam);
	String address = ia.getAddress();
	if (address == null)
	    return null;
	return new LmtpAddress("<" + address + ">", null, null);
    }
    
    private int deliver(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int numDelivered = 0;
        int contentLength = req.getContentLength();
        String from = req.getParameter(PARAM_FROM);
        if (from == null) {
            return numDelivered;
        }
        LmtpEnvelope envelope = new LmtpEnvelope();
        LmtpAddress sender = toLmtpAddress(from);
        if (sender == null || !sender.isValid()) {
            return numDelivered;
        }
        envelope.setSender(sender);

        LmtpServer lmtpServer = ServerManager.getInstance().getLmtpServer();
        if (lmtpServer == null) {
            return numDelivered;
        }
        LmtpBackend lmtpBackend = lmtpServer.getConfig().getLmtpBackend();

        List<String> toList = new LinkedList<String>();
        while (toList.size() < MAX_RECIPIENTS) {
            String to = req.getParameter(PARAM_TO_PREFIX + toList.size());
            if (to == null) {
        	break;
            }
            toList.add(to);
        }

        String[] tos = toList.toArray(new String[toList.size()]);
        
        resp.addHeader(RESPONSE_PROCESSED, Integer.toString(tos.length));
        if (contentLength > -1) {
            ZimbraPerf.COUNTER_LMTP_RCVD_MSGS.increment();
            ZimbraPerf.COUNTER_LMTP_RCVD_BYTES.increment(contentLength);
            ZimbraPerf.COUNTER_LMTP_RCVD_RCPT.increment(tos.length);
        }

        String[] status = new String[tos.length];
        HashMap<LmtpAddress, Integer> responseIndex = new HashMap<LmtpAddress, Integer>();

        for (int i = 0; i < tos.length; i++) {
            LmtpAddress recipient = toLmtpAddress(tos[i]);
            if (recipient == null || !recipient.isValid()) {
        	status[i] = LmtpReply.INVALID_RECIPIENT_ADDRESS.toString();
        	continue;
            }
            responseIndex.put(recipient, i);

            LmtpReply lmtpReply = lmtpBackend.getAddressStatus(recipient);
            if (!lmtpReply.success()) {
        	status[i] = lmtpReply.toString();
        	continue;
            } 

            recipient.setSkipFilters(true);

            String folder = req.getParameter(PARAM_FOLDER_PREFIX + i);
            recipient.setFolder(folder);

            String flags = req.getParameter(PARAM_FLAGS_PREFIX + i);
            recipient.setFlags(flags);

            String tags = req.getParameter(PARAM_TAGS_PREFIX + i);
            recipient.setTags(tags);

            envelope.addRecipient(recipient);
        }

        if (!envelope.getRecipients().isEmpty()) {
            lmtpBackend.deliver(envelope, req.getInputStream(), 0);
        }
        
        for (LmtpAddress recipient : envelope.getRecipients()) {
            LmtpReply lmtpReply = recipient.getDeliveryStatus();
            if (lmtpReply.success()) {
        	numDelivered++;
            }
            int index = responseIndex.get(recipient);
            status[index] = lmtpReply.toString();
        }

        for (int i = 0; i < tos.length; i++) { 
            ZimbraLog.lmtp.info("lmtp over http delivery for " + tos[i] +  ": " + status[i]);
            resp.addHeader(RESPONSE_REPLY_PREFIX + i, status[i]);
        }

        if (contentLength > -1) {
            ZimbraPerf.COUNTER_LMTP_DLVD_MSGS.increment(numDelivered);
            ZimbraPerf.COUNTER_LMTP_DLVD_BYTES.increment(numDelivered * contentLength);
        }
        return numDelivered;
    }
    
    /**
     * Adds an item to a folder specified in the URI.  The item content is
     * provided in the POST request's body.
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {
        ZimbraLog.clearContext();
        addRemoteIpToLoggingContext(req);
        int numDelivered = 0;
        try {
            numDelivered = deliver(req, resp);
        } finally {
            ZimbraLog.clearContext();
            resp.addHeader(RESPONSE_DELIVERED, Integer.toString(numDelivered));
        }
    }
    
    public static void main(String[] args) {
	InternetAddress addr = new InternetAddress(args[0]);
	System.out.println("addr=" + addr.getAddress());
	System.out.println("pers=" + addr.getPersonal());
    }
}
