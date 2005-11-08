package com.zimbra.cs.service.formatter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ValidationException;

import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.util.Constants;

public class IcsFormatter {
    public static void format(UserServlet.Context context, Folder f) throws IOException, ServiceException {
        if (f.getDefaultView() != Folder.TYPE_APPOINTMENT) {
            context.resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "support for requested folder type not implemented yet");
            return;
        }
        context.resp.setContentType("text/calendar");

        try {
            long start = 0;
            long end = System.currentTimeMillis() + (365 * 100 * Constants.MILLIS_PER_DAY);            
            Calendar cal = context.targetMailbox.getCalendarForRange(context.opContext, start, end, f.getId());
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            CalendarOutputter calOut = new CalendarOutputter();
            calOut.output(cal, buf);            
            context.resp.getOutputStream().write(buf.toByteArray());
        } catch (ValidationException e) {
            throw ServiceException.FAILURE(" mbox:"+context.targetMailbox.getId()+" unable to get calendar "+e, e);
        }
    }
}
