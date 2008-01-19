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

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.HttpUtil.Browser;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.calendar.IcsImportParseHandler;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.IcsImportParseHandler.ImportInviteVisitor;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZCalendarBuilder;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZICalendarParseHandler;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.UserServlet.Context;

import javax.mail.Part;
import javax.servlet.ServletException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

    public void formatCallback(Context context) throws IOException, ServiceException {
        Iterator<? extends MailItem> iterator = null;
        List<CalendarItem> calItems = new ArrayList<CalendarItem>();
        //ZimbraLog.mailbox.info("start = "+new Date(context.getStartTime()));
        //ZimbraLog.mailbox.info("end = "+new Date(context.getEndTime()));
        try {
            iterator = getMailItems(context, context.getStartTime(), context.getEndTime(), Integer.MAX_VALUE);

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
        
        // todo: get from folder name
        String filename = context.itemPath;
        if (filename == null || filename.length() == 0)
            filename = "contacts";
        String cd = Part.ATTACHMENT + "; filename=" + HttpUtil.encodeFilename(context.req, filename + ".ics");
        context.resp.addHeader("Content-Disposition", cd);
        context.resp.setCharacterEncoding(Mime.P_CHARSET_UTF8);
        context.resp.setContentType(Mime.CT_TEXT_CALENDAR );

        Browser browser = HttpUtil.guessBrowser(context.req);
        boolean useOutlookCompatMode = Browser.IE.equals(browser);
//        try {
            ZVCalendar cal = context.targetMailbox.getZCalendarForCalendarItems(
                    calItems, useOutlookCompatMode, true,
                    Account.allowPrivateAccess(context.authAccount, context.targetAccount));
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

    public boolean supportsSave() {
        return true;
    }

    public void saveCallback(UserServlet.Context context, String contentType, Folder folder, String filename)
    throws UserServletException, ServiceException, IOException, ServletException {
        // TODO: Modify Formatter.save() API to pass in charset of body, then
        // use that charset in String() constructor.
        boolean continueOnError = context.ignoreAndContinueOnError();
        Reader reader = null;
        try {
            reader = new InputStreamReader(context.req.getInputStream(), Mime.P_CHARSET_UTF8);
            if (context.req.getContentLength() <= LC.calendar_ics_import_full_parse_max_size.intValue()) {
                // Build a list of ZVCalendar objects by fully parsing the ics file, then iterate them
                // and add them one by one.  Memory hungry if there are very many events/tasks, but it allows
                // TZID reference before VTIMEZONE of that timezone appears in the ics file.
                List<ZVCalendar> icals = ZCalendarBuilder.buildMulti(reader);
                ImportInviteVisitor visitor = new ImportInviteVisitor(context.opContext, folder);
                Invite.createFromCalendar(context.targetAccount, null, icals, true, continueOnError, visitor);
            } else {
                // Events/tasks are added in callbacks during parse.  This is more memory efficient than the
                // other method, but it doesn't allow forward referencing TZIDs.  ics files generated by
                // clients that put VTIMEZONEs at the end will not parse.  Evolution client does this.
                ZICalendarParseHandler handler =
                    new IcsImportParseHandler(context.opContext, context.targetAccount, folder, continueOnError);
                ZCalendarBuilder.parse(reader, handler);
            }
        } finally {
            if (reader != null)
                reader.close();
        }
    }
}
