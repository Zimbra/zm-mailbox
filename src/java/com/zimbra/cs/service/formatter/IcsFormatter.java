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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ValidationException;

import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.UserServlet.Context;
import com.zimbra.cs.util.Constants;

public class IcsFormatter extends Formatter {

    public String getType() {
        return "ics";
    }
    
    public String getDefaultSearchTypes() {
        return MailboxIndex.SEARCH_FOR_APPOINTMENTS;
    }
  
    
    public boolean format(Context context, MailItem mailItem) throws IOException, ServiceException {
        
        Iterator iterator = getMailItems(context, mailItem, getDefaultStartTime(), getDefaultEndTime());
        
        List appts = new ArrayList();
        // this is lame
        while (iterator.hasNext()) {
            MailItem item = (MailItem) iterator.next();
            if (item instanceof Appointment) appts.add(item);
        }
        
        context.resp.setContentType("text/calendar");

        try {
            long start = 0;
            long end = System.currentTimeMillis() + (365 * 100 * Constants.MILLIS_PER_DAY);            
            Calendar cal = context.targetMailbox.getCalendarForAppointments(appts);
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            CalendarOutputter calOut = new CalendarOutputter();
            calOut.output(cal, buf);            
            context.resp.getOutputStream().write(buf.toByteArray());
        } catch (ValidationException e) {
            throw ServiceException.FAILURE(" mbox:"+context.targetMailbox.getId()+" unable to get calendar "+e, e);
        }
        return true;
    }
    
    public long getDefaultStartTime() {    
        return 0;
    }

    // eventually get this from query param ?end=long|YYYYMMMDDHHMMSS
    public long getDefaultEndTime() {
        return  System.currentTimeMillis() + (365 * 100 * Constants.MILLIS_PER_DAY);            
    }

}
