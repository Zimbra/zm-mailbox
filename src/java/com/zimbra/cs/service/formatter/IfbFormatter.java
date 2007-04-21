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

import javax.servlet.http.HttpServletResponse;

import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.calendar.FreeBusy;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.UserServlet.Context;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;

public class IfbFormatter extends Formatter {
   
    private static final long MAX_PERIOD_SIZE_IN_DAYS = 200;
    
    private static final long ONE_MONTH = Constants.MILLIS_PER_DAY*31;
    
    public String getType() {
        return "ifb";
    }

    public boolean requiresAuth() {
        return false;
    }
    
    public String getDefaultSearchTypes() {
        return MailboxIndex.SEARCH_FOR_APPOINTMENTS;
    }

    public void formatCallback(Context context) throws IOException, ServiceException, UserServletException {
        context.resp.setCharacterEncoding("UTF-8");
        context.resp.setContentType(Mime.CT_TEXT_CALENDAR);

        long rangeStart = Math.max(context.getStartTime(), getDefaultStartTime());
        long rangeEnd = Math.max(context.getEndTime(), getDefaultEndTime());
        
        if (rangeEnd < rangeStart)
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "End time must be after Start time");
        
        long days = (rangeEnd-rangeStart)/Constants.MILLIS_PER_DAY;
        if (days > MAX_PERIOD_SIZE_IN_DAYS)
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "Requested range is too large (Maximum "+MAX_PERIOD_SIZE_IN_DAYS+" days)");

        FreeBusy fb = context.targetMailbox.getFreeBusy(rangeStart, rangeEnd);
        String url = context.req.getRequestURL() + "?" + context.req.getQueryString();
        String fbMsg = fb.toVCalendar(FreeBusy.Method.PUBLISH, context.targetMailbox.getAccount().getName(), null, url);
        context.resp.getOutputStream().write(fbMsg.getBytes("UTF-8"));
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

    public void saveCallback(byte[] body, Context context, String contentType, Folder folder, String filename) throws UserServletException {
        throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "format not supported for save");
    }
}
