/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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
package com.zimbra.cs.service.formatter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.UserServlet.Context;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.Pair;

public class SyncFormatter extends Formatter {
    
    public static final String QP_NOHDR = "nohdr";

    public String getType() {
        return "sync";
    }

    public String getDefaultSearchTypes() {
        // TODO: all?
        return MailboxIndex.SEARCH_FOR_MESSAGES;
    }

    /**
     * add to content as well as http headers for now (unless told not to)... 
     */
    private static List<Pair<String, String>> getXZimbraHeaders(MailItem item) {
    	List<Pair<String, String>> hdrs = new ArrayList<Pair<String, String>>();
    	hdrs.add(new Pair<String, String>("X-Zimbra-ItemId", item.getId() + ""));
    	hdrs.add(new Pair<String, String>("X-Zimbra-FolderId", item.getFolderId() + ""));
        hdrs.add(new Pair<String, String>("X-Zimbra-Tags", item.getTagString()));
        hdrs.add(new Pair<String, String>("X-Zimbra-Flags", item.getFlagString()));
        hdrs.add(new Pair<String, String>("X-Zimbra-Received", item.getDate() + ""));
        hdrs.add(new Pair<String, String>("X-Zimbra-Modified", item.getChangeDate() + ""));
        hdrs.add(new Pair<String, String>("X-Zimbra-Change", item.getModifiedSequence() + ""));
        hdrs.add(new Pair<String, String>("X-Zimbra-Revision", item.getSavedSequence() + ""));
        if (item instanceof Message) {
        	hdrs.add(new Pair<String, String>("X-Zimbra-Conv", ((Message) item).getConversationId() + ""));
        }
        return hdrs;
    }
    
    private static byte[] getXZimbraHeadersBytes(List<Pair<String, String>> hdrs) {
    	StringBuilder sb = new StringBuilder();
    	for (Pair<String, String> pair :  hdrs) {
    		sb.append(pair.getFirst()).append(": ").append(pair.getSecond()).append("\r\n");
    	}
    	return sb.toString().getBytes();
    }
    
    public static byte[] getXZimbraHeadersBytes(MailItem item) {
    	return getXZimbraHeadersBytes(getXZimbraHeaders(item));
    }
    
    private static void addXZimbraHeaders(Context context, MailItem item, long size) throws IOException {
    	List<Pair<String, String>> hdrs = getXZimbraHeaders(item);
    	for (Pair<String, String> pair :  hdrs) {
    		context.resp.addHeader(pair.getFirst(), pair.getSecond());
    	}

    	//inline X-Zimbra headers with response body if nohdr parameter is not present
    	//also explicitly set the Content-Length header, as it's only done implicitly for short payloads
    	if (context.params.get(QP_NOHDR) == null) {
    		byte[] inline = getXZimbraHeadersBytes(hdrs);
    		if (size > 0) {
    			context.resp.setContentLength(inline.length + (int)size);
    		}
    		context.resp.getOutputStream().write(inline);
    	} else if (size > 0) {
    		context.resp.setContentLength((int)size);
    	}
    }

    public void formatCallback(Context context) throws IOException, ServiceException, UserServletException {
        try {
            if (context.hasPart()) {
                handleMessagePart(context, context.target);
            } else if (context.target instanceof Message) {
                handleMessage(context, (Message) context.target);
            } else if (context.target instanceof CalendarItem) {
                // Don't return private appointments/tasks if the requester is not the mailbox owner.
                CalendarItem calItem = (CalendarItem) context.target;
                if (calItem.isPublic() || calItem.allowPrivateAccess(context.authAccount))
                    handleCalendarItem(context, calItem);
                else
                    context.resp.sendError(HttpServletResponse.SC_FORBIDDEN, "permission denied");
            }
        } catch (MessagingException me) {
            throw ServiceException.FAILURE(me.getMessage(), me);
        }
    }

    private void handleCalendarItem(Context context, CalendarItem calItem) throws IOException, ServiceException, MessagingException {
        context.resp.setContentType(Mime.CT_TEXT_PLAIN);
        if (context.itemId.hasSubpart()) {
            // unfortunately, MimeMessage won't give you the length including headers...
            Pair<MimeMessage,Integer> calItemMsgData = calItem.getSubpartMessageData(context.itemId.getSubpartId());
            addXZimbraHeaders(context, calItem, calItemMsgData.getSecond());
            calItemMsgData.getFirst().writeTo(context.resp.getOutputStream());
        } else {
            InputStream is = calItem.getRawMessage();
            addXZimbraHeaders(context, calItem, calItem.getSize());
            ByteUtil.copy(is, true, context.resp.getOutputStream(), false);
        }        
    }

    private void handleMessage(Context context, Message msg) throws IOException, ServiceException {
        context.resp.setContentType(Mime.CT_TEXT_PLAIN);
        InputStream is = msg.getContentStream();
        addXZimbraHeaders(context, msg, msg.getSize());
        ByteUtil.copy(is, true, context.resp.getOutputStream(), false);
    }

    private void handleMessagePart(Context context, MailItem item) throws IOException, ServiceException, MessagingException, UserServletException {
        if (!(item instanceof Message))
            throw UserServletException.notImplemented("can only handle messages");
        Message message = (Message) item;
        
        MimePart mp = getMimePart(message, context.getPart());
        if (mp != null) {
            String contentType = mp.getContentType();
            if (contentType == null)
                contentType = Mime.CT_APPLICATION_OCTET_STREAM;
            sendbackOriginalDoc(mp, contentType, context.req, context.resp);
            return;
        }
        context.resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "part not found");
    }

    public static MimePart getMimePart(CalendarItem calItem, String part) throws IOException, MessagingException, ServiceException {
        return Mime.getMimePart(calItem.getMimeMessage(), part);
    }

    public static MimePart getMimePart(Message msg, String part) throws IOException, MessagingException, ServiceException {
        return Mime.getMimePart(msg.getMimeMessage(), part);
    }

    public static void sendbackOriginalDoc(MimePart mp, String contentType, HttpServletRequest req, HttpServletResponse resp) throws IOException, MessagingException {
        String filename = Mime.getFilename(mp);
        if (filename == null)
            filename = "unknown";
        String cd = Part.INLINE + "; filename=" + HttpUtil.encodeFilename(req, filename);
        resp.addHeader("Content-Disposition", cd);
        String desc = mp.getDescription();
        if (desc != null)
            resp.addHeader("Content-Description", desc);
        sendbackOriginalDoc(mp.getInputStream(), contentType, resp);
    }

    public static void sendbackOriginalDoc(InputStream is, String contentType, HttpServletResponse resp) throws IOException {
        resp.setContentType(contentType);
        ByteUtil.copy(is, true, resp.getOutputStream(), false);
    }

    public boolean canBeBlocked() {
        return true;
    }

    public boolean supportsSave() {
        return true;
    }

    // FIXME: need to support tags, flags, date, etc...
    public void saveCallback(byte[] body, Context context, String contentType, Folder folder, String filename) throws IOException, ServiceException, UserServletException {
        try {
            Mailbox mbox = folder.getMailbox();
            ParsedMessage pm = new ParsedMessage(body, mbox.attachmentsIndexingEnabled());
            mbox.addMessage(context.opContext, pm, folder.getId(), true, 0, null);
        } catch (MessagingException e) {
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "error parsing message");
        }
    }
}

