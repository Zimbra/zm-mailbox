/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.formatter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.mail.Part;
import javax.servlet.ServletException;

import com.zimbra.common.calendar.ZCalendar.ZCalendarBuilder;
import com.zimbra.common.calendar.ZCalendar.ZICalendarParseHandler;
import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.FileBufferedWriter;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.HttpUtil.Browser;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.CalendarItem.Instance;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.IcsImportParseHandler;
import com.zimbra.cs.mailbox.calendar.IcsImportParseHandler.ImportInviteVisitor;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.UserServletContext;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.formatter.FormatterFactory.FormatType;

public class IcsFormatter extends Formatter {

    @Override
    public FormatType getType() {
        return FormatType.ICS;
    }

    @Override
    public String[] getDefaultMimeTypes() {
        return new String[] { MimeConstants.CT_TEXT_CALENDAR, "text/x-vcalendar" };
    }

    @Override
    public Set<MailItem.Type> getDefaultSearchTypes() {
        return EnumSet.of(MailItem.Type.APPOINTMENT);
    }

    @Override
    public void formatCallback(UserServletContext context) throws IOException, ServiceException {
        Iterator<? extends MailItem> iterator = null;
        List<CalendarItem> calItems = new ArrayList<CalendarItem>();
        //ZimbraLog.mailbox.info("start = "+new Date(context.getStartTime()));
        //ZimbraLog.mailbox.info("end = "+new Date(context.getEndTime()));
        try {
            long start = context.getStartTime();
            long end = context.getEndTime();
            boolean hasTimeRange = start != TIME_UNSPECIFIED && end != TIME_UNSPECIFIED;
            iterator = getMailItems(context, start, end, Integer.MAX_VALUE);

            // this is lame
            while (iterator.hasNext()) {
                MailItem item = iterator.next();
                if (item instanceof CalendarItem) {
                    CalendarItem calItem = (CalendarItem) item;
                    if (hasTimeRange) {
                        Collection<Instance> instances = calItem.expandInstances(start, end, false);
                        if (!instances.isEmpty())
                            calItems.add(calItem);
                    } else {
                        calItems.add(calItem);
                    }
                }
            }
        } finally {
            if (iterator instanceof QueryResultIterator)
                ((QueryResultIterator) iterator).finished();
        }

        // todo: get from folder name
        String filename = context.itemPath;

        if (mayAttach(context)) {
            if (filename == null || filename.length() == 0)
                filename = "contacts";

            String requestFilename = context.req.getParameter("filename"); // Let the client specify the filename to save as
            if (requestFilename != null)
                filename = requestFilename;
            else
                filename = filename.replaceAll("^\\W",""); // Trim off leading non-word characters (e.g. forward slash)

            if (filename.toLowerCase().endsWith(".ics") == false)
                filename = filename + ".ics";
            String cd = HttpUtil.createContentDisposition(context.req, Part.ATTACHMENT, filename);
            context.resp.addHeader("Content-Disposition", cd);
        }
        Browser browser = HttpUtil.guessBrowser(context.req);
        boolean useOutlookCompatMode = Browser.IE.equals(browser);
        boolean needAppleICalHacks = Browser.APPLE_ICAL.equals(browser);  // bug 15549
        boolean htmlFormat = !mayAttach(context) && Browser.IE.equals(browser); // Use only htmlFormat when the file isn't supposed to be downloaded (ie. it's supposed to be shown in the browser). Mangles the code so it can be displayed correctly, especially by IE

        context.resp.setCharacterEncoding(MimeConstants.P_CHARSET_UTF8);

        String contentType;
        if (htmlFormat) {
            contentType = MimeConstants.CT_TEXT_HTML;
        } else if (mayAttach(context)) {
            contentType = MimeConstants.CT_TEXT_CALENDAR;
        } else {
            contentType = MimeConstants.CT_TEXT_PLAIN;
        }
        context.resp.setContentType(contentType);

        OperationContext octxt = new OperationContext(context.getAuthAccount(), context.isUsingAdminPrivileges());
        FileBufferedWriter fileBufferedWriter = new FileBufferedWriter(
                context.resp.getWriter(),
                LC.calendar_ics_export_buffer_size.intValueWithinRange(0, FileBufferedWriter.MAX_BUFFER_SIZE));
        try {
            if (htmlFormat)
                fileBufferedWriter.write("<html><body><pre>");
            context.targetMailbox.writeICalendarForCalendarItems(
                    fileBufferedWriter, octxt, calItems,
                    (context.target != null && context.target instanceof Folder) ? (Folder)context.target : null,
                    useOutlookCompatMode, true, needAppleICalHacks, true, htmlFormat, includeInlineAttaches(context));
            if (htmlFormat)
                fileBufferedWriter.write("</pre></body></html>");
        } finally {
            fileBufferedWriter.finish();
        }
    }

    /**
     * At some point in the future, this may become a multi-value - "none", "inline", "uri"
     * @return true if attachments should be returned inline in the iCalendar (defaults to false)
     */
    protected boolean includeInlineAttaches(UserServletContext context) {
        String inlineAttaches = context.req.getParameter("icalAttach");
        if (inlineAttaches == null) {
            return false;
        }
        return inlineAttaches.equals("inline");
    }

    @Override
    public boolean supportsSave() {
        return true;
    }

    @Override
    public void saveCallback(UserServletContext context, String contentType, Folder folder, String filename)
    throws UserServletException, ServiceException, IOException, ServletException {
        boolean continueOnError = context.ignoreAndContinueOnError();
        boolean preserveExistingAlarms = context.preserveAlarms();
        InputStream is = context.getRequestInputStream(-1);
        String charset = MimeConstants.P_CHARSET_UTF8;
        String ctStr = context.req.getContentType();
        if (ctStr != null) {
            String cs = Mime.getCharset(ctStr);
            if (cs != null)
                charset = cs;
        }

        try {
            if (context.req.getContentLength() <= LC.calendar_ics_import_full_parse_max_size.intValue()) {
                // Build a list of ZVCalendar objects by fully parsing the ics file, then iterate them
                // and add them one by one.  Memory hungry if there are very many events/tasks, but it allows
                // TZID reference before VTIMEZONE of that timezone appears in the ics file.
                List<ZVCalendar> icals = ZCalendarBuilder.buildMulti(is, charset);
                ImportInviteVisitor visitor = new ImportInviteVisitor(context.opContext, folder, preserveExistingAlarms);
                Invite.createFromCalendar(context.targetAccount, null, icals, true, continueOnError, visitor);
            } else {
                // Events/tasks are added in callbacks during parse.  This is more memory efficient than the
                // other method, but it doesn't allow forward referencing TZIDs.  ics files generated by
                // clients that put VTIMEZONEs at the end will not parse.  Evolution client does this.
                ZICalendarParseHandler handler =
                    new IcsImportParseHandler(context.opContext, context.targetAccount, folder,
                                              continueOnError, preserveExistingAlarms);
                ZCalendarBuilder.parse(is, charset, handler);
            }
        } finally {
            is.close();
        }
    }

}
