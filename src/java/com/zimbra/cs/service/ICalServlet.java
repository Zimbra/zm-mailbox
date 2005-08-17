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
    private static final String  WWW_AUTHENTICATE_VALUE = "BASIC realm=\"LiquidSystems iCal\"";
    
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
