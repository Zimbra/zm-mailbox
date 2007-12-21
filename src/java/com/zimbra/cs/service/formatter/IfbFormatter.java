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

import javax.servlet.http.HttpServletResponse;

import com.zimbra.cs.account.IDNUtil;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.MailboxIndex;
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

        String url = context.req.getRequestURL() + "?" + context.req.getQueryString();
        String acctName = null;
        FreeBusy fb = null;
        if (context.targetMailbox != null) {
            fb = context.targetMailbox.getFreeBusy(rangeStart, rangeEnd);
            acctName = context.targetMailbox.getAccount().getName();
        } else {
            // Unknown mailbox.  Fake an always-free response, to avoid harvest attacks.
            fb = FreeBusy.createDummyFreeBusy(rangeStart, rangeEnd);
            acctName = fixupAccountName(context.accountPath);
        }
        String fbMsg = fb.toVCalendar(FreeBusy.Method.PUBLISH, acctName, null, url);
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

    private String fixupAccountName(String emailAddress) throws ServiceException {
        int index = emailAddress.indexOf('@');
        String domain = null;
        if (index == -1) {
            // domain is already in ASCII name
            domain = Provisioning.getInstance().getConfig().getAttr(Provisioning.A_zimbraDefaultDomainName, null);
            if (domain != null)
                emailAddress = emailAddress + "@" + domain;            
        } else
            emailAddress = IDNUtil.toAsciiEmail(emailAddress);

        return emailAddress;
    }
}
