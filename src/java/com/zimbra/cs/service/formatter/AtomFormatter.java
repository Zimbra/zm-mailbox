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
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import javax.servlet.http.HttpServletResponse;

import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.InviteInfo;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.UserServlet.Context;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.soap.Element;

public class AtomFormatter extends Formatter {
    
    public void formatCallback(Context context) throws IOException, ServiceException {
        Iterator<? extends MailItem> iterator = null;
        StringBuffer sb = new StringBuffer();
        Element.XMLElement feed = new Element.XMLElement("feed");
        int offset = context.getOffset();
        int limit = context.getLimit();
        try {
            iterator = getMailItems(context, context.getStartTime(), context.getEndTime(), limit-offset);
            
            context.resp.setCharacterEncoding("UTF-8");
            context.resp.setContentType("application/atom+xml");

            sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
                
            feed.addAttribute("xmlns", "http://www.w3.org/2005/Atom");
    
            feed.addElement("title").setText("Zimbra " + context.itemPath);
            feed.addElement("generator").setText("Zimbra Atom Feed Servlet");
            feed.addElement("id").setText(context.req.getRequestURL().toString());
            feed.addElement("updated").setText(DateUtil.toISO8601(new Date(context.targetMailbox.getLastChangeDate())));
            
            int curHit = 0;
                    
            while (iterator.hasNext()) {
                MailItem itItem = iterator.next();
                curHit++;
                if (curHit > limit)
                    break;
                if (curHit >= offset) {
                    if (itItem instanceof CalendarItem) {
                        // Don't return private appointments/tasks if the requester is not the mailbox owner.
                        CalendarItem calItem = (CalendarItem) itItem;
                        if (calItem.isPublic() || calItem.allowPrivateAccess(context.authAccount))
                            addCalendarItem(calItem, feed, context);                
                    } else if (itItem instanceof Message) {
                        addMessage((Message) itItem, feed, context);
                    }
                }
            }
        } finally {
            if (iterator instanceof QueryResultIterator)
                ((QueryResultIterator) iterator).finished();
        }
        sb.append(feed.toString());
        context.resp.getOutputStream().write(sb.toString().getBytes("UTF-8"));
    }

    public long getDefaultStartTime() {    
        return System.currentTimeMillis() - (7*Constants.MILLIS_PER_DAY);
    }

    // eventually get this from query param ?end=long|YYYYMMMDDHHMMSS
    public long getDefaultEndTime() {
        return System.currentTimeMillis() + (7*Constants.MILLIS_PER_DAY);
    }
    
    private void addCalendarItem(CalendarItem calItem, Element feed, Context context) {
        Collection instances = calItem.expandInstances(context.getStartTime(), context.getEndTime(), false);
        for (Iterator instIt = instances.iterator(); instIt.hasNext(); ) {
            CalendarItem.Instance inst = (CalendarItem.Instance) instIt.next();
            InviteInfo invId = inst.getInviteInfo();
            Invite inv = calItem.getInvite(invId.getMsgId(), invId.getComponentId());
            Element entry = feed.addElement("entry");
            entry.addElement("title").setText(inv.getName());
            entry.addElement("updated").setText(DateUtil.toISO8601(new Date(inst.getStart())));
            entry.addElement("summary").setText(inv.getFragment());
            // TODO: only personal part in name
            if (inv.hasOrganizer()) {
                Element author = entry.addElement("author");
                author.addElement("name").setText(inv.getOrganizer().getCn());
                author.addElement("email").setText(inv.getOrganizer().getAddress());
            }
        }                    
        
    }
     
    private void addMessage(Message m, Element feed, Context context) {
        Element entry = feed.addElement("entry");
        entry.addElement("title").setText(m.getSubject());
        entry.addElement("summary").setText(m.getFragment());
        Element author = entry.addElement("author");
        // TODO: only personal part in name
        author.addElement("name").setText(m.getSender());
        author.addElement("email").setText(m.getSender());
        entry.addElement("modified").setText(DateUtil.toISO8601(new Date(m.getDate())));
    }

    public String getType() {
        return "atom";
    }

    public boolean canBeBlocked() {
        return false;
    }

    public void saveCallback(byte[] body, Context context, String contentType, Folder folder, String filename) throws UserServletException {
        throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "format not supported for save");
    }
}
