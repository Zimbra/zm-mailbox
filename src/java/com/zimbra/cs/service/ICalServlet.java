/*
***** BEGIN LICENSE BLOCK *****
Version: ZPL 1.1

The contents of this file are subject to the Zimbra Public License
Version 1.1 ("License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.zimbra.com/license

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
the License for the specific language governing rights and limitations
under the License.

The Original Code is: Zimbra Collaboration Suite.

The Initial Developer of the Original Code is Zimbra, Inc.  Portions
created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
Reserved.

Contributor(s): 

***** END LICENSE BLOCK *****
*/

package com.zimbra.cs.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ValidationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * simple iCal servlet on a mailbox. URL is:
 * 
 *  http://server/service/ical/cal.ics[?...support-range-at-some-point...]
 *  
 *  need to support a range query at some point, right now get -30 thorugh +90 days from today
 *
 */

public class ICalServlet extends ZimbraBasicAuthServlet {

    private static final String  WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";
    private static final String  WWW_AUTHENTICATE_VALUE = "BASIC realm=\"Zimbra iCal\"";
    
    private static final String PARAM_QUERY = "query";

    private static Log mLog = LogFactory.getLog(ICalServlet.class);

    public void doAuthGet(HttpServletRequest req, HttpServletResponse resp, Account acct, Mailbox mailbox)
    throws ServiceException, IOException
    {
        resp.setContentType("text/calendar");
        StringBuffer sb = new StringBuffer();

        long MSECS_PER_DAY = 1000*60*60*24;
        long rangeStart = System.currentTimeMillis() - (30*MSECS_PER_DAY);
        long rangeEnd = rangeStart+(90*MSECS_PER_DAY);

        try {
            Calendar cal = mailbox.getCalendarForRange(null, rangeStart, rangeEnd);
                
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            CalendarOutputter calOut = new CalendarOutputter();
            calOut.output(cal, buf);            
            resp.getOutputStream().write(buf.toByteArray());
        } catch (ValidationException e) {
            throw ServiceException.FAILURE("unable to get calendar "+e, e);
        }
    }
}
