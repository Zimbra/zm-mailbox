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
import java.util.Iterator;

import javax.servlet.http.HttpServletResponse;

import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.calendar.FreeBusy;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.ParsedDateTime;
import com.zimbra.cs.mailbox.calendar.ZCalendar;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.operation.Operation;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.UserServlet.Context;
import com.zimbra.cs.util.Constants;

public class IfbFormatter extends Formatter {
   
    public static class Format {};
    public static class Save {};
    static int sFormatLoad = Operation.setLoad(IfbFormatter.Format.class, 10);
    static int sSaveLoad = Operation.setLoad(IfbFormatter.Save.class, 10);
    int getFormatLoad() { return  sFormatLoad; }
    int getSaveLoad() { return sSaveLoad; }
    
    
    private static final long MAX_PERIOD_SIZE_IN_DAYS = 200;
    
    private static final long ONE_MONTH = Constants.MILLIS_PER_DAY*31;
    
    private static final String NL = "\n";

    public String getType() {
        return "ifb";
    }

    public boolean requiresAuth() {
        return false;
    }
    
    public String getDefaultSearchTypes() {
        return MailboxIndex.SEARCH_FOR_APPOINTMENTS;
    }

    public void formatCallback(Context context, MailItem item) throws IOException, ServiceException, UserServletException {
        context.resp.setCharacterEncoding("UTF-8");
        context.resp.setContentType(Mime.CT_TEXT_CALENDAR);

        long now = System.currentTimeMillis();
        long rangeStart = Math.max(context.getStartTime(), getDefaultStartTime());
        long rangeEnd = Math.max(context.getEndTime(), getDefaultEndTime());
        
        if (rangeEnd < rangeStart)
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "End time must be after Start time");
        
        long days = (rangeEnd-rangeStart)/Constants.MILLIS_PER_DAY;
        if (days > MAX_PERIOD_SIZE_IN_DAYS)
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "Requested range is too large (Maximum "+MAX_PERIOD_SIZE_IN_DAYS+" days)");
            
        ParsedDateTime dtStart = ParsedDateTime.fromUTCTime(rangeStart);
        ParsedDateTime dtEnd = ParsedDateTime.fromUTCTime(rangeEnd);
        ParsedDateTime dtNow = ParsedDateTime.fromUTCTime(now);

        //StringBuffer toRet = new StringBuffer("BEGIN:VFREEBUSY").append(NL);
        StringBuffer toRet = new StringBuffer("BEGIN:VCALENDAR").append(NL);
        toRet.append("VERSION:2.0").append(NL);
        toRet.append("METHOD:PUBLISH").append(NL);
        toRet.append("PRODID:").append(ZCalendar.sZimbraProdID).append(NL);
        toRet.append("BEGIN:VFREEBUSY").append(NL);
            
        toRet.append("ORGANIZER:").append(context.targetMailbox.getAccount().getName()).append(NL);
        toRet.append("DTSTAMP:").append(dtNow.toString()).append(NL);
        toRet.append("DTSTART:").append(dtStart.toString()).append(NL);
        toRet.append("DTEND:").append(dtEnd.toString()).append(NL);
        toRet.append("URL:").append(context.req.getRequestURL()).append('?').append(context.req.getQueryString()).append(NL);

        FreeBusy fb = context.targetMailbox.getFreeBusy(rangeStart, rangeEnd);

//            BEGIN:VFREEBUSY
//            ORGANIZER:jsmith@host.com
//            DTSTART:19980313T141711Z
//            DTEND:19980410T141711Z
//            FREEBUSY:19980314T233000Z/19980315T003000Z
//            FREEBUSY:19980316T153000Z/19980316T163000Z
//            FREEBUSY:19980318T030000Z/19980318T040000Z
//            URL:http://www.host.com/calendar/busytime/jsmith.ifb
//            END:VFREEBUSY
            
            
        for (Iterator iter = fb.iterator(); iter.hasNext(); ) {
            FreeBusy.Interval cur = (FreeBusy.Interval)iter.next();
            String status = cur.getStatus();
                
            if (status.equals(IcalXmlStrMap.FBTYPE_FREE)) {
                continue;
            } else if (status.equals(IcalXmlStrMap.FBTYPE_BUSY)) {
                toRet.append("FREEBUSY:");
            } else if (status.equals(IcalXmlStrMap.FBTYPE_BUSY_TENTATIVE)) {
                toRet.append("FREEBUSY;FBTYPE=BUSY-TENTATIVE:");
            } else if (status.equals(IcalXmlStrMap.FBTYPE_BUSY_UNAVAILABLE)) {
                toRet.append("FREEBUSY;FBTYPE=BUSY-UNAVAILABLE:");
            } else {
                assert(false);
                toRet.append(":");
            }
                
            ParsedDateTime curStart = ParsedDateTime.fromUTCTime(cur.getStart());
            ParsedDateTime curEnd = ParsedDateTime.fromUTCTime(cur.getEnd());
                
            toRet.append(curStart.toString()).append('/').append(curEnd.toString()).append(NL);
        }
            
        toRet.append("END:VFREEBUSY").append(NL);
        toRet.append("END:VCALENDAR").append(NL);
            
        context.resp.getOutputStream().write(toRet.toString().getBytes("UTF-8"));
    }
    
    public long getDefaultStartTime() {
        return System.currentTimeMillis() - ONE_MONTH;
    }

    // eventually get this from query param ?end=long|YYYYMMMDDHHMMSS
    public long getDefaultEndTime() {
        return System.currentTimeMillis() + (2 * ONE_MONTH);
    }

    public boolean canBeBlocked() {
        return false;
    }

    public void saveCallback(byte[] body, Context context, Folder folder) throws UserServletException {
        throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "format not supported for save");
    }
}
