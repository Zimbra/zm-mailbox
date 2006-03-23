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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZCalendarBuilder;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.UserServlet.Context;
import com.zimbra.cs.util.Constants;

public class IcsFormatter extends Formatter {

    public String getType() {
        return "ics";
    }

    public String[] getDefaultMimeTypes() {
        return new String[] { Mime.CT_TEXT_CALENDAR, "text/x-vcalendar" };
    }

    public String getDefaultSearchTypes() {
        return MailboxIndex.SEARCH_FOR_APPOINTMENTS;
    }

    public void format(Context context, MailItem mailItem) throws IOException, ServiceException {
        //ZimbraLog.mailbox.info("start = "+new Date(context.getStartTime()));
        //ZimbraLog.mailbox.info("end = "+new Date(context.getEndTime()));
        Iterator iterator = getMailItems(context, mailItem, context.getStartTime(), context.getEndTime());
        
        List<Appointment> appts = new ArrayList<Appointment>();
        // this is lame
        while (iterator.hasNext()) {
            MailItem item = (MailItem) iterator.next();
            if (item instanceof Appointment) appts.add((Appointment) item);
        }
        
        context.resp.setCharacterEncoding(Mime.P_CHARSET_UTF8);
        context.resp.setContentType(Mime.CT_TEXT_CALENDAR );

//        try {
            ZVCalendar cal = context.targetMailbox.getZCalendarForAppointments(appts);
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            OutputStreamWriter wout = new OutputStreamWriter(buf, Mime.P_CHARSET_UTF8);
            cal.toICalendar(wout);
            wout.flush();
            context.resp.getOutputStream().write(buf.toByteArray());
//        } catch (ValidationException e) {
//            throw ServiceException.FAILURE(" mbox:"+context.targetMailbox.getId()+" unable to get calendar "+e, e);
//        }
    }
    
    // get the whole calendar
    public long getDefaultStartTime() {    
        return 0;
    }

    // eventually get this from query param ?end=long|YYYYMMMDDHHMMSS
    public long getDefaultEndTime() {
        return System.currentTimeMillis() + (365 * 100 * Constants.MILLIS_PER_DAY);            
    }

    public boolean canBeBlocked() {
        return false;
    }

    public void save(byte[] body, Context context, Folder folder) throws ServiceException, IOException {
        Reader reader = new StringReader(new String(body, Mime.P_CHARSET_UTF8));
        ZVCalendar ical = ZCalendarBuilder.build(reader);
        List<Invite> invites = Invite.createFromCalendar(context.authAccount, null, ical, false);
        for (Invite inv : invites) {
            // handle missing UIDs on remote calendars by generating them as needed
            if (inv.getUid() == null)
                inv.setUid(LdapUtil.generateUUID());
            // and add the invite to the calendar!
            folder.getMailbox().addInvite(context.opContext, inv, folder.getId(), false, null);
        }
    }
}
