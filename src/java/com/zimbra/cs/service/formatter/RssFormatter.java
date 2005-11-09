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
package com.zimbra.cs.service.formatter;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import javax.servlet.http.HttpServletResponse;

import net.fortuna.ical4j.model.Parameter;

import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.InviteInfo;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.mail.CalendarUtils;
import com.zimbra.cs.util.Constants;
import com.zimbra.soap.Element;

public class RssFormatter {
    public static void format(UserServlet.Context context, Folder f) throws IOException, ServiceException {
        if (f.getDefaultView() != Folder.TYPE_APPOINTMENT) {
            context.resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "support for requested folder type not implemented yet");
            return;
        }
        
        context.resp.setContentType("application/rss+xml");
        
        StringBuffer sb = new StringBuffer();

        sb.append("<?xml version=\"1.0\"?>");
            
        Element.XMLElement rss = new Element.XMLElement("rss");
        rss.addAttribute("version", "2.0");

        Element channel = rss.addElement("channel");
        channel.addElement("title").setText("Zimbra " + context.itemPath);
            
        channel.addElement("generator").setText("Zimbra RSS Feed Servlet");

        long start = System.currentTimeMillis() - (7 * Constants.MILLIS_PER_DAY);
        long end = start + (14 * Constants.MILLIS_PER_DAY);
        Collection appts = context.targetMailbox.getAppointmentsForRange(context.opContext, start, end, f.getId(), null);
                
        //channel.addElement("description").setText(query);

        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
//        MailDateFormat mdf = new MailDateFormat();
        for (Iterator apptIt = appts.iterator(); apptIt.hasNext(); ) {            
            Appointment appt = (Appointment) apptIt.next();

            Collection instances = appt.expandInstances(start, end);
            for (Iterator instIt = instances.iterator(); instIt.hasNext(); ) {
                Appointment.Instance inst = (Appointment.Instance) instIt.next();
                InviteInfo invId = inst.getInviteInfo();
                Invite inv = appt.getInvite(invId.getMsgId(), invId.getComponentId());
                Element item = channel.addElement("item");
                item.addElement("title").setText(inv.getName());
                item.addElement("pubDate").setText(sdf.format(new Date(inst.getStart())));
                /*                
                StringBuffer desc = new StringBuffer();
                sb.append("Start: ").append(sdf.format(new Date(inst.getStart()))).append("\n");
                sb.append("End: ").append(sdf.format(new Date(inst.getEnd()))).append("\n");
                sb.append("Location: ").append(inv.getLocation()).append("\n");
                sb.append("Notes: ").append(inv.getFragment()).append("\n");
                item.addElement("description").setText(sb.toString());
                */
                item.addElement("description").setText(inv.getFragment());
                item.addElement("author").setText(CalendarUtils.paramVal(inv.getOrganizer(), Parameter.CN));
                /* TODO: guid, links, etc */
                //Element guid = item.addElement("guid");
                //guid.setText(appt.getUid()+"-"+inv.getStartTime().getUtcTime());
                //guid.addAttribute("isPermaLink", "false");
            }                    
        }
        sb.append(rss.toString());
        context.resp.getOutputStream().write(sb.toString().getBytes());
    }
}
