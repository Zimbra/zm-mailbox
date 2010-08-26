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
    public static final String RECURDEF_BAD_PATTERN        = "tnef2ical.RECURDEF_BAD_PATTERN";
    public static final String RECURDEF_BAD_FREQ           = "tnef2ical.RECURDEF_BAD_FREQ";
    public static final String RECURDEF_BAD_MSCALSCALE     = "tnef2ical.RECURDEF_BAD_MSCALSCALE";
    public static final String RECURDEF_BAD_ENDTYPE        = "tnef2ical.RECURDEF_BAD_ENDTYPE";
    public static final String RECURDEF_BAD_1ST_DOW        = "tnef2ical.RECURDEF_BAD_1ST_DOW";
    public static final String RECURDEF_BAD_CHANGED_SUBJ   = "tnef2ical.RECURDEF_BAD_CHANGED_SUBJ";
    public static final String RECURDEF_BAD_CHANGED_LOC    = "tnef2ical.RECURDEF_BAD_CHANGED_LOC";

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

    public static TNEFtoIcalendarServiceException RECURDEF_BAD_PATTERN(String pattType) {
        return new UnsupportedTnefCalendaringMsgException
                ("TNEF recurrence definition contains unexpected PatternType:" + pattType,
                RECURDEF_BAD_PATTERN, RECEIVERS_FAULT);
    }

    public static TNEFtoIcalendarServiceException RECURDEF_BAD_FREQ(String freqency) {
        return new UnsupportedTnefCalendaringMsgException
                ("TNEF recurrence definition contains unexpected frequency:" + freqency,
                RECURDEF_BAD_FREQ, RECEIVERS_FAULT);
    }

    public static TNEFtoIcalendarServiceException RECURDEF_BAD_MSCALSCALE(String msCalScale) {
        return new UnsupportedTnefCalendaringMsgException
                ("TNEF recurrence definition contains unexpected X-MICROSOFT-CALSCALE setting:" + msCalScale,
                RECURDEF_BAD_MSCALSCALE, RECEIVERS_FAULT);
    }

    public static TNEFtoIcalendarServiceException RECURDEF_BAD_ENDTYPE(String endType) {
        return new UnsupportedTnefCalendaringMsgException
                ("TNEF recurrence definition contains bad endtype specification:" + endType,
                RECURDEF_BAD_ENDTYPE, RECEIVERS_FAULT);
    }

    public static TNEFtoIcalendarServiceException RECURDEF_BAD_1ST_DOW(String dow) {
        return new UnsupportedTnefCalendaringMsgException
                ("TNEF recurrence definition contains bad First Day Of Week specification:" + dow,
                RECURDEF_BAD_1ST_DOW, RECEIVERS_FAULT);
    }

    public static TNEFtoIcalendarServiceException RECURDEF_BAD_CHANGED_SUBJ(String desc) {
        return new UnsupportedTnefCalendaringMsgException
                ("TNEF recurrence definition changed subject length info corrupt:" + desc,
                RECURDEF_BAD_CHANGED_SUBJ, RECEIVERS_FAULT);
    }

    public static TNEFtoIcalendarServiceException RECURDEF_BAD_CHANGED_LOC(String desc) {
        return new UnsupportedTnefCalendaringMsgException
                ("TNEF recurrence definition changed location length info corrupt:" + desc,
                RECURDEF_BAD_CHANGED_LOC, RECEIVERS_FAULT);
    }
}
