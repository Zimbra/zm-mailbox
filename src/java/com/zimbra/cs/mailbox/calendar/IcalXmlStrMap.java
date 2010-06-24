/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

/**
 * 
 */
package com.zimbra.cs.mailbox.calendar;

import java.util.HashMap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.fb.FreeBusy;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;

public class IcalXmlStrMap {

    IcalXmlStrMap(String name) {
        mMapName = name;
    }
    
    public void add(String ical, String xml) {
        fwdMap.put(ical.toUpperCase(), xml);
        bakMap.put(xml.toUpperCase(), ical);
    }
    
    public String toXml(String name) {
        return (String)(fwdMap.get(name.toUpperCase()));
    }
    public String toIcal(String name) throws ServiceException {
        String toRet = (String)(bakMap.get(name.toUpperCase()));
        if (toRet == null) {
            throw MailServiceException.INVALID_REQUEST("Unknown string '"+name+"' for parameter "+mMapName, null);
        }
        return toRet;
    }
    
    public boolean validXml(String str) {
        return bakMap.containsKey(str.toUpperCase());
    }
    
    public boolean validICal(String str) {
        return fwdMap.containsKey(str.toUpperCase());
    }

    public static IcalXmlStrMap sFreqMap = new IcalXmlStrMap("Freq");
    public static IcalXmlStrMap sClassMap = new IcalXmlStrMap("Class");
    public static IcalXmlStrMap sTranspMap = new IcalXmlStrMap("Transparency");
    public static IcalXmlStrMap sFreeBusyMap = new IcalXmlStrMap("FreeBusy");
    public static IcalXmlStrMap sOutlookFreeBusyMap = new IcalXmlStrMap("OutlookFreeBusy");
    public static IcalXmlStrMap sStatusMap = new IcalXmlStrMap("Status");
    public static IcalXmlStrMap sPartStatMap = new IcalXmlStrMap("PartStat");
    public static IcalXmlStrMap sRoleMap = new IcalXmlStrMap("Role");
    public static IcalXmlStrMap sCUTypeMap = new IcalXmlStrMap("CUType");
    
    private HashMap<String, String> fwdMap = new HashMap<String, String>();
    private HashMap<String, String> bakMap = new HashMap<String, String>();
    private String mMapName;
    

    
    // frequency 
    public final static String FREQ_YEARLY = "YEA";
    public final static String FREQ_WEEKLY = "WEE";
    public final static String FREQ_SECONDLY = "SEC";
    public final static String FREQ_MONTHLY = "MON";
    public final static String FREQ_MINUTELY = "MIN";
    public final static String FREQ_HOURLY = "HOU";
    public final static String FREQ_DAILY = "DAI";

    // class
    public final static String CLASS_PUBLIC = "PUB";
    public final static String CLASS_PRIVATE = "PRI";
    public final static String CLASS_CONFIDENTIAL = "CON";

    // free-busy
    public final static String FBTYPE_BUSY = "B";
    public final static String FBTYPE_FREE = "F";
    public final static String FBTYPE_BUSY_TENTATIVE = "T";
    public final static String FBTYPE_BUSY_UNAVAILABLE = "O";
    public final static String FBTYPE_NODATA = "N";

    // transparency 
    public final static String TRANSP_OPAQUE = "O";
    public final static String TRANSP_TRANSPARENT = "T";

    // Even status can be TENTATIVE, CONFIRMED or CANCELLED.
    // Todo status can be NEEDS-ACTION, COMPLETED, IN-PROCESS or CANCELLED.
    // Journal status (not yet supported) can be DRAFT, FINAL or CANCELLED.
    public final static String STATUS_TENTATIVE = "TENT";
    public final static String STATUS_CONFIRMED = "CONF";
    public final static String STATUS_CANCELLED = "CANC";
    public final static String STATUS_NEEDS_ACTION = "NEED";
    public final static String STATUS_COMPLETED = "COMP";
    public final static String STATUS_IN_PROCESS = "INPR";
    public final static String STATUS_ZCO_WAITING = "WAITING";
    public final static String STATUS_ZCO_DEFERRED = "DEFERRED";

    // attendee participation status =
    //   NEeds-action, TEntative, ACcept, DEclined,
    //   DG (delegated), COmpleted (for todo), IN-process (for todo)
    public final static String PARTSTAT_TENTATIVE = "TE";
    public final static String PARTSTAT_NEEDS_ACTION = "NE";
    public final static String PARTSTAT_DELEGATED = "DG";
    public final static String PARTSTAT_DECLINED = "DE";
    public final static String PARTSTAT_COMPLETED = "CO";
    public final static String PARTSTAT_ACCEPTED = "AC";
    public final static String PARTSTAT_IN_PROCESS = "IN";
    public final static String PARTSTAT_ZCO_WAITING = "WA";
    public final static String PARTSTAT_ZCO_DEFERRED = "DF";

    // attendee role
    public final static String ROLE_NON_PARTICIPANT = "NON";
    public final static String ROLE_OPT_PARTICIPANT = "OPT";
    public final static String ROLE_REQUIRED = "REQ";
    public final static String ROLE_CHAIR = "CHA";

    // attendee calendar user type
    public final static String CUTYPE_INDIVIDUAL = "IND";
    public final static String CUTYPE_GROUP      = "GRO";
    public final static String CUTYPE_RESOURCE   = "RES";
    public final static String CUTYPE_ROOM       = "ROO";
    public final static String CUTYPE_UNKNOWN    = "UNK";

    static {
        sCUTypeMap.add(ICalTok.INDIVIDUAL.toString(), CUTYPE_INDIVIDUAL);
    	sCUTypeMap.add(ICalTok.GROUP.toString(), CUTYPE_GROUP);
    	sCUTypeMap.add(ICalTok.RESOURCE.toString(), CUTYPE_RESOURCE);
    	sCUTypeMap.add(ICalTok.ROOM.toString(), CUTYPE_ROOM);
    	sCUTypeMap.add(ICalTok.UNKNOWN.toString(), CUTYPE_UNKNOWN);

    	sRoleMap.add(ICalTok.CHAIR.toString(), ROLE_CHAIR);
        sRoleMap.add(ICalTok.REQ_PARTICIPANT.toString(), ROLE_REQUIRED);
        sRoleMap.add(ICalTok.OPT_PARTICIPANT.toString(), ROLE_OPT_PARTICIPANT);
        sRoleMap.add(ICalTok.NON_PARTICIPANT.toString(), ROLE_NON_PARTICIPANT);

        sStatusMap.add(ICalTok.TENTATIVE.toString(), STATUS_TENTATIVE);
        sStatusMap.add(ICalTok.CONFIRMED.toString(), STATUS_CONFIRMED);
        sStatusMap.add(ICalTok.CANCELLED.toString(), STATUS_CANCELLED);
        sStatusMap.add(ICalTok.NEEDS_ACTION.toString(), STATUS_NEEDS_ACTION);
        sStatusMap.add(ICalTok.COMPLETED.toString(), STATUS_COMPLETED);
        sStatusMap.add(ICalTok.IN_PROCESS.toString(), STATUS_IN_PROCESS);
        sStatusMap.add(ICalTok.X_ZIMBRA_STATUS_WAITING.toString(), STATUS_ZCO_WAITING);
        sStatusMap.add(ICalTok.X_ZIMBRA_STATUS_DEFERRED.toString(), STATUS_ZCO_DEFERRED);

        sPartStatMap.add(ICalTok.ACCEPTED.toString(), PARTSTAT_ACCEPTED);
        sPartStatMap.add(ICalTok.COMPLETED.toString(), PARTSTAT_COMPLETED);
        sPartStatMap.add(ICalTok.DECLINED.toString(), PARTSTAT_DECLINED);
        sPartStatMap.add(ICalTok.DELEGATED.toString(), PARTSTAT_DELEGATED);
        sPartStatMap.add(ICalTok.IN_PROCESS.toString(), PARTSTAT_IN_PROCESS);
        sPartStatMap.add(ICalTok.NEEDS_ACTION.toString(), PARTSTAT_NEEDS_ACTION);
        sPartStatMap.add(ICalTok.TENTATIVE.toString(), PARTSTAT_TENTATIVE);
        sPartStatMap.add(ICalTok.X_ZIMBRA_PARTSTAT_WAITING.toString(), PARTSTAT_ZCO_WAITING);
        sPartStatMap.add(ICalTok.X_ZIMBRA_PARTSTAT_DEFERRED.toString(), PARTSTAT_ZCO_DEFERRED);

        sClassMap.add(ICalTok.PUBLIC.toString(), CLASS_PUBLIC);
        sClassMap.add(ICalTok.PRIVATE.toString(), CLASS_PRIVATE);
        sClassMap.add(ICalTok.CONFIDENTIAL.toString(), CLASS_CONFIDENTIAL);

        sFreeBusyMap.add(FreeBusy.FBTYPE_FREE, FBTYPE_FREE);
        sFreeBusyMap.add(FreeBusy.FBTYPE_BUSY, FBTYPE_BUSY);
        sFreeBusyMap.add(FreeBusy.FBTYPE_BUSY_TENTATIVE, FBTYPE_BUSY_TENTATIVE);
        sFreeBusyMap.add(FreeBusy.FBTYPE_BUSY_UNAVAILABLE, FBTYPE_BUSY_UNAVAILABLE);
        sFreeBusyMap.add(FreeBusy.FBTYPE_NODATA, FBTYPE_NODATA);

        sOutlookFreeBusyMap.add(FreeBusy.FBTYPE_OUTLOOK_FREE, FBTYPE_FREE);
        sOutlookFreeBusyMap.add(FreeBusy.FBTYPE_OUTLOOK_BUSY, FBTYPE_BUSY);
        sOutlookFreeBusyMap.add(FreeBusy.FBTYPE_OUTLOOK_TENTATIVE, FBTYPE_BUSY_TENTATIVE);
        sOutlookFreeBusyMap.add(FreeBusy.FBTYPE_OUTLOOK_OUTOFOFFICE, FBTYPE_BUSY_UNAVAILABLE);

        sTranspMap.add(ICalTok.TRANSPARENT.toString(), TRANSP_TRANSPARENT);
        sTranspMap.add(ICalTok.OPAQUE.toString(), TRANSP_OPAQUE);
        
        sFreqMap.add(ZRecur.Frequency.DAILY.toString(), FREQ_DAILY);
        sFreqMap.add(ZRecur.Frequency.HOURLY.toString(), FREQ_HOURLY);
        sFreqMap.add(ZRecur.Frequency.MINUTELY.toString(), FREQ_MINUTELY);
        sFreqMap.add(ZRecur.Frequency.MONTHLY.toString(), FREQ_MONTHLY);
        sFreqMap.add(ZRecur.Frequency.SECONDLY.toString(), FREQ_SECONDLY);
        sFreqMap.add(ZRecur.Frequency.WEEKLY.toString(), FREQ_WEEKLY);
        sFreqMap.add(ZRecur.Frequency.YEARLY.toString(), FREQ_YEARLY);
    }
}
