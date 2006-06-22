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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.formatter;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import javax.servlet.http.HttpServletResponse;

import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.InviteInfo;
import com.zimbra.cs.operation.Operation;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.UserServlet.Context;
import com.zimbra.cs.util.Constants;
import com.zimbra.cs.util.DateUtil;
import com.zimbra.soap.Element;

public class AtomFormatter extends Formatter {
    
    public static class Format {};
    public static class Save {};
    static int sFormatLoad = Operation.setLoad(AtomFormatter.Format.class, 10);
    static int sSaveLoad = Operation.setLoad(AtomFormatter.Save.class, 10);
    int getFormatLoad() { return  sFormatLoad; }
    int getSaveLoad() { return sSaveLoad; }
    
    public void formatCallback(Context context, MailItem item) throws IOException, ServiceException {
        Iterator<? extends MailItem> iterator = null;
        StringBuffer sb = new StringBuffer();
        Element.XMLElement feed = new Element.XMLElement("feed");
        try {
            iterator = getMailItems(context, item, context.getStartTime(), context.getEndTime());
            
            context.resp.setCharacterEncoding("UTF-8");
            context.resp.setContentType("application/atom+xml");

            sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
                
            feed.addAttribute("xmlns", "http://www.w3.org/2005/Atom");
    
            feed.addElement("title").setText("Zimbra " + context.itemPath);
            feed.addElement("generator").setText("Zimbra Atom Feed Servlet");
            feed.addElement("id").setText(context.req.getRequestURL().toString());
            feed.addElement("updated").setText(DateUtil.toISO8601(new Date(context.targetMailbox.getLastChangeDate())));
                    
            while (iterator.hasNext()) {
                MailItem itItem = iterator.next();
                if (itItem instanceof Appointment) {
                    addAppointment((Appointment) itItem, feed, context);                
                } else if (itItem instanceof Message) {
                    addMessage((Message) itItem, feed, context);
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
    
    private void addAppointment(Appointment appt, Element feed, Context context) {
        Collection instances = appt.expandInstances(context.getStartTime(), context.getEndTime());
        for (Iterator instIt = instances.iterator(); instIt.hasNext(); ) {
            Appointment.Instance inst = (Appointment.Instance) instIt.next();
            InviteInfo invId = inst.getInviteInfo();
            Invite inv = appt.getInvite(invId.getMsgId(), invId.getComponentId());
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

    public void saveCallback(byte[] body, Context context, Folder folder) throws UserServletException {
        throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "format not supported for save");
    }
}
