/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

package com.zimbra.cs.util.tnef;

import com.zimbra.common.service.ServiceException;

/**
 *
 * @author Gren Elliot
 */
public class TNEFtoIcalendarServiceException extends ServiceException {

    private static final long serialVersionUID = -7850038065077241157L;

    public static final String NON_CALENDARING_CLASS       = "tnef2ical.NON_CALENDARING_CLASS";
    public static final String NON_SOLAR_CALENDAR          = "tnef2ical.NON_SOLAR_CALENDAR";
    public static final String UNSUPPORTED_RECURRENCE_TYPE = "tnef2ical.UNSUPPORTED_RECURRENCE_TYPE";
    public static final String RRULE_PARSING_PROBLEM       = "tnef2ical.RRULE_PARSING_PROBLEM";

    /**
     * A public inner subclass whose purpose is to group various "TooComplex"
     * exceptions into a common type so that one can write a catch block to
     * catch all such exceptions.
     */
    public static class UnsupportedTnefCalendaringMsgException extends TNEFtoIcalendarServiceException {
        private static final long serialVersionUID = -3367247762350948868L;

        UnsupportedTnefCalendaringMsgException(String message, String code,
                boolean isReceiversFault, Throwable cause, Argument... args) {
            super(message, code, isReceiversFault, cause, args);
        }
        UnsupportedTnefCalendaringMsgException(String message, String code,
                boolean isReceiversFault, Argument... args) {
            super(message, code, isReceiversFault, null, args);
        }
    }


    /**
     * Creates a new instance of <code>TNEFtoIcalendarException</code>.
     */
    TNEFtoIcalendarServiceException(String message, String code,
            boolean isReceiversFault, Throwable cause, Argument... args) {
        super(message, code, isReceiversFault, cause, args);
    }
    /**
     * Creates a new instance of <code>TNEFtoIcalendarException</code>.
     */
    TNEFtoIcalendarServiceException(String message, String code,
            boolean isReceiversFault, Argument... args) {
        super(message, code, isReceiversFault, null, args);
    }

    public static TNEFtoIcalendarServiceException
            RRULE_PARSING_PROBLEM(Throwable cause) {
        return new UnsupportedTnefCalendaringMsgException
                ("Internal error parsing RRULE generated to represent rule in TNEF",
                RRULE_PARSING_PROBLEM, RECEIVERS_FAULT, cause);
    }

    public static TNEFtoIcalendarServiceException
            NON_CALENDARING_CLASS(String msgClass) {
        return new UnsupportedTnefCalendaringMsgException
                ("TNEF represents non-Calendaring (or unrecognised) message class:" + msgClass,
                NON_CALENDARING_CLASS, RECEIVERS_FAULT);
    }
    public static TNEFtoIcalendarServiceException
            UNSUPPORTED_RECURRENCE_TYPE(String patt) {
        return new UnsupportedTnefCalendaringMsgException
                ("TNEF represents recurrence which uses unsupported pattern:" + patt,
                UNSUPPORTED_RECURRENCE_TYPE, RECEIVERS_FAULT);
    }

    public static TNEFtoIcalendarServiceException NON_SOLAR_CALENDAR() {
        return new UnsupportedTnefCalendaringMsgException
                ("TNEF represents recurrence based on a non-Solar calendar",
                NON_SOLAR_CALENDAR, RECEIVERS_FAULT);
    }

}
