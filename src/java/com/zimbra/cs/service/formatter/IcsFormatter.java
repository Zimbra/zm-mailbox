/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZCalendarBuilder;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.operation.Operation;
import com.zimbra.cs.service.UserServlet.Context;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.HttpUtil.Browser;

public class IcsFormatter extends Formatter {

    public static class Format {};
    public static class Save {};
    static int sFormatLoad = Operation.setLoad(IcsFormatter.Format.class, 10);
    static int sSaveLoad = Operation.setLoad(IcsFormatter.Save.class, 10);
    int getFormatLoad() { return  sFormatLoad; }
    int getSaveLoad() { return sSaveLoad; }
    
    public String getType() {
        return "ics";
    }

    public String[] getDefaultMimeTypes() {
        return new String[] { Mime.CT_TEXT_CALENDAR, "text/x-vcalendar" };
    }

    public String getDefaultSearchTypes() {
        return MailboxIndex.SEARCH_FOR_APPOINTMENTS;
    }

    public void formatCallback(Context context, MailItem mailItem) throws IOException, ServiceException {
        Iterator<? extends MailItem> iterator = null;
        List<CalendarItem> calItems = new ArrayList<CalendarItem>();
        //ZimbraLog.mailbox.info("start = "+new Date(context.getStartTime()));
        //ZimbraLog.mailbox.info("end = "+new Date(context.getEndTime()));
        try {
            iterator = getMailItems(context, mailItem, context.getStartTime(), context.getEndTime(), Integer.MAX_VALUE);

            // this is lame
            while (iterator.hasNext()) {
                MailItem item = iterator.next();
                if (item instanceof CalendarItem)
                    calItems.add((CalendarItem) item);
            }
        } finally {
            if (iterator instanceof QueryResultIterator)
                ((QueryResultIterator) iterator).finished();
        }
        
        context.resp.setCharacterEncoding(Mime.P_CHARSET_UTF8);
        context.resp.setContentType(Mime.CT_TEXT_CALENDAR );

        Browser browser = HttpUtil.guessBrowser(context.req);
        boolean useOutlookCompatMode = Browser.IE.equals(browser);
//        try {
            ZVCalendar cal = context.targetMailbox.getZCalendarForCalendarItems(calItems, useOutlookCompatMode);
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            OutputStreamWriter wout = new OutputStreamWriter(buf, Mime.P_CHARSET_UTF8);
            boolean forceOlsonTZID = Browser.APPLE_ICAL.equals(browser);  // bug 15549
            cal.toICalendar(wout, forceOlsonTZID);
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

    public void saveCallback(byte[] body, Context context, Folder folder) throws ServiceException, IOException {
        // TODO: Modify Formatter.save() API to pass in charset of body, then
        // use that charset in String() constructor.
        Reader reader = new StringReader(new String(body, Mime.P_CHARSET_UTF8));
        List<ZVCalendar> icals = ZCalendarBuilder.buildMulti(reader);
        List<Invite> invites = Invite.createFromCalendar(context.targetAccount, null, icals, false);
        for (Invite inv : invites) {
            // handle missing UIDs on remote calendars by generating them as needed
            if (inv.getUid() == null)
                inv.setUid(LdapUtil.generateUUID());
            // and add the invite to the calendar!
            folder.getMailbox().addInvite(context.opContext, inv, folder.getId(), false, null);
        }
    }
}
