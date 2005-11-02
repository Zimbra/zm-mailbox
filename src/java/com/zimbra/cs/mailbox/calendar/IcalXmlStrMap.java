/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/**
 * 
 */
package com.zimbra.cs.mailbox.calendar;

import java.util.HashMap;

import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.parameter.PartStat;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.property.Status;
import net.fortuna.ical4j.model.property.Transp;

import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.service.ServiceException;

public class IcalXmlStrMap
{
    IcalXmlStrMap(String name) {
        mMapName = name;
    }
    
    public void add(String ical, String xml) {
        ical = ical.toUpperCase();
        xml = xml.toUpperCase();
        
        fwdMap.put(ical, xml);
        bakMap.put(xml, ical);
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
        return bakMap.containsKey(str);
    }
    
    public boolean validICal(String str) {
        return fwdMap.containsKey(str);
    }
    
    public static IcalXmlStrMap sFreqMap = new IcalXmlStrMap("Freq");
    public static IcalXmlStrMap sTranspMap = new IcalXmlStrMap("Transparency");
    public static IcalXmlStrMap sFreeBusyMap = new IcalXmlStrMap("FreeBusy");
    public static IcalXmlStrMap sOutlookFreeBusyMap = new IcalXmlStrMap("OutlookFreeBusy");
    public static IcalXmlStrMap sStatusMap = new IcalXmlStrMap("Status");
    public static IcalXmlStrMap sPartStatMap = new IcalXmlStrMap("PartStat");
    public static IcalXmlStrMap sRoleMap = new IcalXmlStrMap("Role");
    
    private HashMap fwdMap = new HashMap();
    private HashMap bakMap = new HashMap();
    private String mMapName;
    

    
    // frequency 
    public final static String FREQ_YEARLY= "YEA";
    public final static String FREQ_WEEKLY= "WEE";
    public final static String FREQ_SECONDLY = "SEC";
    public final static String FREQ_MONTHLY = "MON";
    public final static String FREQ_MINUTELY = "MIN";
    public final static String FREQ_HOURLY = "HOU";
    public final static String FREQ_DAILY = "DAI";

    // free-busy
    public final static String FBTYPE_BUSY = "B";
    public final static String FBTYPE_FREE = "F";
    public final static String FBTYPE_BUSY_TENTATIVE = "T";
    public final static String FBTYPE_BUSY_UNAVAILABLE = "O";

    // transparency 
    public final static String TRANSP_OPAQUE = "O";
    public final static String TRANSP_TRANSPARENT = "T";

    // event/todo/journal status
    public final static String STATUS_TENTATIVE = "TENT";
    public final static String STATUS_CONFIRMED = "CONF";
    public final static String STATUS_CANCELLED = "CANC";
    // there are more values for todo and journal...

    // attendee participation status =
    //   NEeds-action, TEntative, ACcept, DEclined,
    //   DG (delegated), COmpleted (todo), IN-process (todo)
    public final static String PARTSTAT_TENTATIVE = "TE";
    public final static String PARTSTAT_NEEDS_ACTION = "NE";
    public final static String PARTSTAT_DELEGATED = "DG";
    public final static String PARTSTAT_DECLINED = "DE";
    public final static String PARTSTAT_COMPLETED = "CO";
    public final static String PARTSTAT_ACCEPTED = "AC";
    public final static String PARTSTAT_IN_PROCESS = "IN";
    
    // attendee role
    public final static String ROLE_NON_PARTICIPANT = "NON";
    public final static String ROLE_OPT_PARTICIPANT = "OPT";
    public final static String ROLE_REQUIRED = "REQ";
    public final static String ROLE_CHAIR = "CHA";
    
    static {
        sRoleMap.add(Role.CHAIR.getName(), ROLE_CHAIR);
        sRoleMap.add(Role.REQ_PARTICIPANT.getValue(), ROLE_REQUIRED);
        sRoleMap.add(Role.OPT_PARTICIPANT.getValue(), ROLE_OPT_PARTICIPANT);
        sRoleMap.add(Role.NON_PARTICIPANT.getValue(), ROLE_NON_PARTICIPANT);

        sStatusMap.add(Status.VEVENT_TENTATIVE.getValue(), STATUS_TENTATIVE);
        sStatusMap.add(Status.VEVENT_CONFIRMED.getValue(), STATUS_CONFIRMED);
        sStatusMap.add(Status.VEVENT_CANCELLED.getValue(), STATUS_CANCELLED);

        sPartStatMap.add(PartStat.ACCEPTED.getValue(), PARTSTAT_ACCEPTED);
        sPartStatMap.add(PartStat.COMPLETED.getValue(), PARTSTAT_COMPLETED);
        sPartStatMap.add(PartStat.DECLINED.getValue(), PARTSTAT_DECLINED);
        sPartStatMap.add(PartStat.DELEGATED.getValue(), PARTSTAT_DELEGATED);
        sPartStatMap.add(PartStat.IN_PROCESS.getValue(), PARTSTAT_IN_PROCESS);
        sPartStatMap.add(PartStat.NEEDS_ACTION.getValue(), PARTSTAT_NEEDS_ACTION);
        sPartStatMap.add(PartStat.TENTATIVE.getValue(), PARTSTAT_TENTATIVE);

        sFreeBusyMap.add(FreeBusy.FBTYPE_FREE, FBTYPE_FREE);
        sFreeBusyMap.add(FreeBusy.FBTYPE_BUSY, FBTYPE_BUSY);
        sFreeBusyMap.add(FreeBusy.FBTYPE_BUSY_TENTATIVE, FBTYPE_BUSY_TENTATIVE);
        sFreeBusyMap.add(FreeBusy.FBTYPE_BUSY_UNAVAILABLE, FBTYPE_BUSY_UNAVAILABLE);

        sOutlookFreeBusyMap.add(FreeBusy.FBTYPE_OUTLOOK_FREE, FBTYPE_FREE);
        sOutlookFreeBusyMap.add(FreeBusy.FBTYPE_OUTLOOK_BUSY, FBTYPE_BUSY);
        sOutlookFreeBusyMap.add(FreeBusy.FBTYPE_OUTLOOK_TENTATIVE, FBTYPE_BUSY_TENTATIVE);
        sOutlookFreeBusyMap.add(FreeBusy.FBTYPE_OUTLOOK_OUTOFOFFICE, FBTYPE_BUSY_UNAVAILABLE);

        sTranspMap.add(Transp.TRANSPARENT.getValue(), TRANSP_TRANSPARENT);
        sTranspMap.add(Transp.OPAQUE.getValue(), TRANSP_OPAQUE);
        
        sFreqMap.add(Recur.DAILY, FREQ_DAILY);
        sFreqMap.add(Recur.HOURLY, FREQ_HOURLY);
        sFreqMap.add(Recur.MINUTELY, FREQ_MINUTELY);
        sFreqMap.add(Recur.MONTHLY, FREQ_MONTHLY);
        sFreqMap.add(Recur.SECONDLY, FREQ_SECONDLY);
        sFreqMap.add(Recur.WEEKLY, FREQ_WEEKLY);
        sFreqMap.add(Recur.YEARLY, FREQ_YEARLY);
    }
}
