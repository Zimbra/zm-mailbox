/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 VMware, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.caldav;

import java.util.Iterator;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.calendar.ZCalendar.ZComponent;
import com.zimbra.common.calendar.ZCalendar.ZProperty;
import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.util.AccountUtil.AccountAddressMatcher;

public class CalDavUtils {

    // Fixup for ATTENDEE properties set by Apple iCal
    // Don't let the organizer to be also listed as an attendee.
    public static void removeAttendeeForOrganizer(ZComponent comp) {
        if (!DebugConfig.caldavAllowAttendeeForOrganizer &&
            (ICalTok.VEVENT.equals(comp.getTok()) || ICalTok.VTODO.equals(comp.getTok()))) {
            String organizer = comp.getPropVal(ICalTok.ORGANIZER, null);
            if (organizer != null) {
                organizer = organizer.trim();
                AccountAddressMatcher acctMatcher = null;
                String address = stripMailto(organizer);
                if (address != null) {
                    try {
                        Account acct = Provisioning.getInstance().get(AccountBy.name, address);
                        if (acct != null) {
                            acctMatcher = new AccountAddressMatcher(acct);
                        }
                    } catch (ServiceException e) {
                        // ignore
                        ZimbraLog.dav.warn("could not get the account matcher for " + address, e);
                    }
                }
                for (Iterator<ZProperty> propIter = comp.getPropertyIterator(); propIter.hasNext(); ) {
                    ZProperty prop = propIter.next();
                    if (ICalTok.ATTENDEE.equals(prop.getToken())) {
                        String att = prop.getValue();
                        if (att != null) {
                            att = att.trim();
                            try {
                                // iCal has a habit of listing the organizer as an ATTENDEE.  Undo it.
                                if (att.equalsIgnoreCase(organizer) || (acctMatcher != null && acctMatcher.matches(stripMailto(att)))) {
                                    propIter.remove();
                                }
                            } catch (ServiceException e) {
                                // ignore
                                ZimbraLog.dav.warn("exception while matching the attendee address " + att, e);
                            }
                        } else {
                            // We haven't seen this case occur, but just in case.
                            propIter.remove();
                        }
                    }
                }
            }
        }
    }

    public static void removeAttendeeForOrganizer(ZVCalendar cal) {
        for (Iterator<ZComponent> compIter = cal.getComponentIterator(); compIter.hasNext(); ) {
            removeAttendeeForOrganizer(compIter.next());
        }
    }

    public static void adjustPercentCompleteForToDos(ZVCalendar cal) {
        for (Iterator<ZComponent> compIter = cal.getComponentIterator(); compIter.hasNext(); ) {
            ZComponent comp = compIter.next();
            if (ICalTok.VTODO.equals(comp.getTok()))
                adjustPercentCompleteForTodo(comp);
        }
    }

    private static void adjustPercentCompleteForTodo(ZComponent comp) {
        boolean isCompleted = false;
        if (comp.getProperty(ICalTok.COMPLETED) != null)
            isCompleted = true;
        else {
            ZProperty status = comp.getProperty(ICalTok.STATUS);
            if (status != null && status.getValue().equals(ICalTok.COMPLETED.toString()))
                isCompleted = true;
        }
        if (!isCompleted) {
            // iCal5 doesn't have a percent-complete field in the UI, but it preserves that
            // attribute when present. When the todo is made incomplete in iCal it unsets the
            // COMPLETED datetime but preserves the percent-complete which can be 100.
            // Reset percentcomplete if it is 100, so that todo does not get treated as
            // completed down the line.
            ZProperty percentComplete = comp.getProperty(ICalTok.PERCENT_COMPLETE);
            if (percentComplete != null && percentComplete.getIntValue() == 100)
                percentComplete.setValue(Integer.toString(0));
        }
    }

    public static String stripMailto(String address) {
        if (address != null && address.toLowerCase().startsWith("mailto:")) {
            return address.substring(7);
        } else {
            return address;
        }
    }
}
