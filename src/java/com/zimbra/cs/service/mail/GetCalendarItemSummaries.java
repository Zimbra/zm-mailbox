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

/*
 * Created on Feb 17, 2005
 */
package com.zimbra.cs.service.mail;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;

import com.zimbra.common.calendar.Geo;
import com.zimbra.common.calendar.ParsedDateTime;
import com.zimbra.common.calendar.ParsedDuration;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.CalendarItem.AlarmData;
import com.zimbra.cs.mailbox.Task;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.InviteInfo;
import com.zimbra.cs.mailbox.calendar.RecurId;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;
import com.zimbra.cs.mailbox.calendar.cache.CacheToXML;
import com.zimbra.cs.mailbox.calendar.cache.CalendarItemData;
import com.zimbra.cs.mailbox.calendar.cache.CalSummaryCache.CalendarDataResult;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.type.AppointmentData;
import com.zimbra.soap.mail.type.CalendaringDataInterface;
import com.zimbra.soap.mail.type.GeoInfo;
import com.zimbra.soap.mail.type.InstanceDataInfo;
import com.zimbra.soap.mail.type.InstanceDataInterface;
import com.zimbra.soap.mail.type.LegacyAppointmentData;
import com.zimbra.soap.mail.type.LegacyInstanceDataInfo;
import com.zimbra.soap.mail.type.LegacyTaskData;
import com.zimbra.soap.mail.type.TaskData;


/**
 * @author tim
 */
public class GetCalendarItemSummaries extends CalendarRequest {

    private static Log mLog = LogFactory.getLog(GetCalendarItemSummaries.class);

    private static final String[] TARGET_FOLDER_PATH = new String[] { MailConstants.A_FOLDER };
    private static final String[] RESPONSE_ITEM_PATH = new String[] { };
    protected String[] getProxiedIdPath(Element request)     { return TARGET_FOLDER_PATH; }
    protected boolean checkMountpointProxy(Element request)  { return true; }
    protected String[] getResponseItemPath()  { return RESPONSE_ITEM_PATH; }

    private static final String DEFAULT_FOLDER = "" + Mailbox.ID_AUTO_INCREMENT;
    
    private static final long MSEC_PER_DAY = 1000*60*60*24;
    private static final long MAX_PERIOD_SIZE_IN_DAYS = 200;
    
    static class EncodeCalendarItemResult {
        Element element;
        int numInstancesExpanded;
    }
    
    /**
     * Encodes a calendar item 
     * 
     * @param parent
     * @param elementName
     *         name of element to add (MailConstants .E_APPOINTMENT or MailConstants.E_TASK)
     * @param rangeStart 
     *         start period to expand instances
     * @param rangeEnd
     *         end period to expand instances
     * @param newFormat
     *         temporary HACK - true: SearchRequest, false: GetAppointmentSummaries        
     * @return
     */
    static EncodeCalendarItemResult encodeCalendarItemInstances(
            ZimbraSoapContext lc, OperationContext octxt, CalendarItem calItem,
            Account acct, long rangeStart, long rangeEnd, boolean newFormat)
    throws ServiceException {
        EncodeCalendarItemResult toRet = new EncodeCalendarItemResult();
        ItemIdFormatter ifmt = new ItemIdFormatter(lc);
        Account authAccount = getAuthenticatedAccount(lc);
        boolean hidePrivate = !calItem.allowPrivateAccess(authAccount, lc.isUsingAdminPrivileges());

        try {
            boolean expandRanges;
            if (calItem instanceof Task) {
                expandRanges = true;
                if (rangeStart == -1 && rangeEnd == -1) {
                    rangeStart = Long.MIN_VALUE;
                    rangeEnd = Long.MAX_VALUE;
                }
            } else {
                expandRanges = (rangeStart > 0 && rangeEnd > 0 && rangeStart < rangeEnd);
            }
            
            boolean isAppointment = calItem instanceof Appointment;
            
            // don't initialize until we find at least one valid instance
            CalendaringDataInterface calData = null;
            
            Invite defaultInvite = calItem.getDefaultInviteOrNull();
            
            if (defaultInvite == null) {
                mLog.info("Could not load defaultinfo for calendar item with id="+calItem.getId()+" SKIPPING");
                return toRet; 
            }

            ParsedDuration defDuration = defaultInvite.getEffectiveDuration();
            // Duration is null if no DTEND or DURATION was present.  Assume 1 day for all-day
            // events and 1 second for non all-day.  (bug 28615)
            if (defDuration == null && !defaultInvite.isTodo()) {
                if (defaultInvite.isAllDayEvent())
                    defDuration = ParsedDuration.ONE_DAY;
                else
                    defDuration = ParsedDuration.ONE_SECOND;
            }
            long defDurationMsecs = 0;
            if (defaultInvite.getStartTime() != null && defDuration != null) {
                ParsedDateTime s = defaultInvite.getStartTime();
                long et = s.add(defDuration).getUtcTime();
                defDurationMsecs = et - s.getUtcTime();
            }
            
            String defaultFba = null;
            if (calItem instanceof Appointment)
                defaultFba = ((Appointment) calItem).getEffectiveFreeBusyActual(defaultInvite, null);
            
            String defaultPtSt = calItem.getEffectivePartStat(defaultInvite, null);

            AlarmData alarmData = calItem.getAlarmData();

            // add all the instances:
            int numInRange = 0;
            
            if (expandRanges) {
                Collection<CalendarItem.Instance> instances = calItem.expandInstances(rangeStart, rangeEnd, true);
                long alarmTime = 0;
                long alarmInst = 0;
                if (alarmData != null) {
                    alarmTime = alarmData.getNextAt();
                    alarmInst = alarmData.getNextInstanceStart();
                }
                for (CalendarItem.Instance inst : instances) {
                    try {
                        InviteInfo invId = inst.getInviteInfo();
                        Invite inv = calItem.getInvite(invId.getMsgId(), invId.getComponentId());
                        boolean showAll = !hidePrivate || inv.isPublic();

                        // figure out which fields are different from the default and put their data here...

                        ParsedDuration invDuration = inv.getEffectiveDuration();
                        long instStart = inst.getStart();
                        // For an instance whose alarm time is within the time range, we must
                        // include it even if its start time is after the range.
                        long startOrAlarm = instStart == alarmInst ? alarmTime : instStart;

                        // Duration is null if no DTEND or DURATION was present.  Assume 1 day for all-day
                        // events and 1 second for non all-day.  (bug 28615)
                        if (invDuration == null) {
                            if (inv.isAllDayEvent())
                                invDuration = ParsedDuration.ONE_DAY;
                            else
                                invDuration = ParsedDuration.ONE_SECOND;
                        }
                        if (!inst.hasStart() ||
                            (startOrAlarm < rangeEnd && invDuration.addToTime(instStart) > rangeStart)) {
                            numInRange++;
                        } else {
                            continue;
                        }

                        if (calData == null) {
                            String uid = calItem.getUid();
                            if (newFormat) {
                                if (isAppointment)
                                    calData = new AppointmentData(uid, uid);
                                else
                                    calData = new TaskData(uid, uid);
                            } else {
                                if (isAppointment)
                                    calData =
                                        new LegacyAppointmentData(uid, uid);
                                else
                                    calData = new LegacyTaskData(uid, uid);
                            }
                            if (showAll) {
                                // flags and tags
                                String flags = calItem.getFlagString();
                                if (flags != null && !flags.equals(""))
                                    calData.setFlags(flags);
                                String tags = calItem.getTagString();
                                if (tags != null && !tags.equals(""))
                                    calData.setTags(tags);
                            }

                            // Organizer
                            if (inv.hasOrganizer()) {
                                ZOrganizer org = inv.getOrganizer();
                                calData.setOrganizer(org.toJaxb());
                            }
                        }
                        InstanceDataInterface instance = (newFormat
                                ? new InstanceDataInfo()
                                : new LegacyInstanceDataInfo());
                        calData.addCalendaringInstance(instance);

                        if (showAll) {
                            if (isAppointment && inv.isEvent()) {
                                String instFba = ((Appointment) calItem).getEffectiveFreeBusyActual(inv, inst);
                                if (instFba != null &&
                                        (!instFba.equals(defaultFba) ||
                                                inst.isException())) {
                                    instance.setFreeBusyActual(instFba);
                                }
                            }
                            String instPtSt =
                                calItem.getEffectivePartStat(inv, inst);
                            if (!defaultPtSt.equals(instPtSt) ||
                                    inst.isException()) {
                                instance.setPartStat(instPtSt);
                            }
                        }

                        if (inst.hasStart()) {
                            instance.setStartTime(instStart);
                            if (inv.isAllDayEvent()) {
                                instance.setTzOffset(
                                        new Long(inst.getStartTzOffset()));
                            }
                        }


                        if (inst.isException() && inv.hasRecurId()) {
                            RecurId rid = inv.getRecurId();
                            instance.setRecurIdZ(rid.getDtZ());
                        } else {
                            instance.setRecurIdZ(inst.getRecurIdZ());
                        }

                        if (inst.isException()) {

                            instance.setIsException(true);

                            instance.setInvId(ifmt.formatItemId(
                                            calItem, inst.getMailItemId()));
                            instance.setComponentNum(inst.getComponentNum());

                            if (showAll) {
                                // fragment has already been sanitized...
                                String frag = inv.getFragment();
                                if (frag != null && !frag.equals("")) {
                                    instance.setFragment(frag);
                                }

                                instance.setPriority(inv.getPriority());

                                if (inv.isEvent()) {
                                    instance.setFreeBusyIntended(
                                            inv.getFreeBusy());
                                    instance.setTransparency(
                                                inv.getTransparency());
                                }

                                if (inv.isTodo()) {
                                    instance.setTaskPercentComplete(
                                                inv.getPercentComplete());
                                }

                                instance.setName(inv.getName());

                                instance.setLocation(inv.getLocation());

                                List<String> categories = inv.getCategories();
                                if (categories != null) {
                                    for (String cat : categories) {
                                        instance.addCategory(cat);
                                    }
                                }
                                Geo geo = inv.getGeo();
                                if (geo != null) {
                                    instance.setGeo(new GeoInfo(geo));
                                }

                                if (inv.hasOtherAttendees()) {
                                    instance.setHasOtherAttendees(true);
                                }

                                if (inv.hasAlarm()) {
                                    instance.setHasAlarm(true);
                                }
                            }

                            instance.setIsOrganizer(inv.isOrganizer());

                            if (inv.isTodo()) {
                                if (inst.hasEnd()) {
                                    instance.setTaskDueDate(inst.getEnd());
                                    if (inv.isAllDayEvent()) {
                                        instance.setTaskTzOffsetDue(inst.getEndTzOffset());
                                    }
                                }
                            } else {
                                if (inst.hasStart() && inst.hasEnd()) {
                                    instance.setDuration(
                                            inst.getEnd() - inst.getStart());
                                }
                            }

                            instance.setStatus(inv.getStatus());
                            instance.setCalClass(inv.getClassProp());
                            if (inv.isAllDayEvent()) {
                                instance.setAllDay(true);
                            }
                            if (inv.isDraft()) {
                                instance.setDraft(true);
                            }
                            if (inv.isNeverSent()) {
                                instance.setNeverSent(true);
                            }
                            if (inv.isRecurrence()) {
                                instance.setIsRecurring(true);
                            }
                        } else {
                            if (inv.isTodo()) {
                                if (inst.hasEnd()) {
                                    instance.setTaskDueDate(inst.getEnd());
                                    if (inv.isAllDayEvent()) {
                                        instance.setTaskTzOffsetDue(
                                                inst.getEndTzOffset());
                                    }
                                }
                            } else {
                                // A non-exception instance can have duration
                                // that is different from the default duration
                                // due to daylight savings time transitions.
                                if (inst.hasStart() && inst.hasEnd() &&
                                        defDurationMsecs !=
                                            inst.getEnd()-inst.getStart()) {
                                    instance.setDuration(
                                            inst.getEnd() - inst.getStart());
                                }
                            }
                        }
                    } catch (MailServiceException.NoSuchItemException e) {
                        mLog.info("Error could not get instance "+
                                inst.getMailItemId()+"-"+inst.getComponentNum()+
                            " for appt "+calItem.getId(), e);
                    }
                } // iterate all the instances
            } // if expandRanges

            // if we found any calItems at all, we have to encode the "Default" data here
            if (!expandRanges || numInRange > 0) {
                boolean showAll = !hidePrivate || defaultInvite.isPublic();
                if (calData == null) {
                    String uid = calItem.getUid();
                    if (newFormat) {
                        if (isAppointment)
                            calData = new AppointmentData(uid, uid);
                        else
                            calData = new TaskData(uid, uid);
                    } else {
                        if (isAppointment)
                            calData =
                                new LegacyAppointmentData(uid, uid);
                        else
                            calData = new LegacyTaskData(uid, uid);
                    }
                    if (showAll) {
                        // flags and tags
                        String flags = calItem.getFlagString();
                        if (flags != null && !flags.equals(""))
                            calData.setFlags(flags);
                        String tags = calItem.getTagString();
                        if (tags != null && !tags.equals(""))
                            calData.setTags(tags);
                    }

                    // Organizer
                    if (defaultInvite.hasOrganizer()) {
                        ZOrganizer org = defaultInvite.getOrganizer();
                        calData.setOrganizer(org.toJaxb());
                    }
                }

                if (showAll) {
                    String defaultPriority = defaultInvite.getPriority();
                    
                    calData.setPriority(defaultPriority);
                    calData.setPartStat(defaultPtSt);
                    if (defaultInvite.isEvent()) {
                        calData.setFreeBusyIntended(defaultInvite.getFreeBusy());
                        calData.setFreeBusyActual(defaultFba);
                        calData.setTransparency(defaultInvite.getTransparency());
                    }
                    if (defaultInvite.isTodo()) {
                        String pctComplete = defaultInvite.getPercentComplete();
                        calData.setTaskPercentComplete(pctComplete);
                    }

                    calData.setName(defaultInvite.getName());
                    calData.setLocation(defaultInvite.getLocation());

                    List<String> categories = defaultInvite.getCategories();
                    if (categories != null) {
                        for (String cat : categories) {
                            calData.addCategory(cat);
                        }
                    }
                    Geo geo = defaultInvite.getGeo();
                    if (geo != null) {
                        calData.setGeo(new GeoInfo(geo));
                    }

                    // fragment has already been sanitized...
                    String fragment = defaultInvite.getFragment();
                    if (!fragment.equals("")) {
                        calData.setFragment(fragment);
                    }

                    if (alarmData != null) {
                        calData.setAlarmData(
                                ToXML.alarmDataToJaxb(calItem, alarmData));
                    }

                    if (defaultInvite.hasOtherAttendees()) {
                        calData.setHasOtherAttendees(defaultInvite.hasOtherAttendees());
                    }
                    if (defaultInvite.hasAlarm()) {
                        calData.setHasAlarm(defaultInvite.hasAlarm());
                    }
                }

                calData.setIsOrganizer(defaultInvite.isOrganizer());
                calData.setId(ifmt.formatItemId(calItem));
                calData.setInvId(ifmt.formatItemId(
                        calItem, defaultInvite.getMailItemId()));
                calData.setComponentNum(defaultInvite.getComponentNum());
                calData.setFolderId(ifmt.formatItemId(calItem.getFolderId()));
                calData.setStatus(defaultInvite.getStatus());
                calData.setCalClass(defaultInvite.getClassProp());
                if (!defaultInvite.isTodo()) {
                    calData.setDuration(defDurationMsecs);
                }
                if (defaultInvite.isAllDayEvent()) {
                    calData.setAllDay(defaultInvite.isAllDayEvent());
                }
                if (defaultInvite.isDraft()) {
                    calData.setDraft(defaultInvite.isDraft());
                }
                if (defaultInvite.isNeverSent()) {
                    calData.setNeverSent(defaultInvite.isNeverSent());
                }
                if (defaultInvite.isRecurrence()) {
                    calData.setIsRecurring(defaultInvite.isRecurrence());
                }
                toRet.element = lc.jaxbToNamedElement(
                        isAppointment ? MailConstants.E_APPOINTMENT :
                            MailConstants.E_TASK, MailConstants.NAMESPACE_STR,
                            calData);
            }
            toRet.numInstancesExpanded = numInRange;
        } catch(MailServiceException.NoSuchItemException e) {
            mLog.info("Error could not get default invite for calendar item: "+
                    calItem.getId(), e);
        } catch (RuntimeException e) {
            mLog.info("Caught Exception "+e+
                    " while getting summary info for calendar item: "+
                    calItem.getId(), e);
        }
        
        return toRet;
    }
    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        Account acct = getRequestedAccount(zsc);
        
        long rangeStart = request.getAttributeLong(MailConstants.A_CAL_START_TIME);
        long rangeEnd = request.getAttributeLong(MailConstants.A_CAL_END_TIME);
        
        if (rangeEnd < rangeStart) {
            throw ServiceException.INVALID_REQUEST("End time must be after Start time", null);
        }
        
        long days = (rangeEnd-rangeStart)/MSEC_PER_DAY;
        if (days > MAX_PERIOD_SIZE_IN_DAYS) {
            throw ServiceException.INVALID_REQUEST("Requested range is too large (Maximum "+MAX_PERIOD_SIZE_IN_DAYS+" days)", null);
        }
        
        
        ItemId iidFolder = new ItemId(request.getAttribute(MailConstants.A_FOLDER, DEFAULT_FOLDER), zsc);
        
        Element response = getResponseElement(zsc);

        OperationContext octxt = getOperationContext(zsc, context);

        if (LC.calendar_cache_enabled.booleanValue()) {
            ItemIdFormatter ifmt = new ItemIdFormatter(zsc);
            int folderId = iidFolder.getId();
            if (folderId != Mailbox.ID_AUTO_INCREMENT) {
                CalendarDataResult result = mbox.getCalendarSummaryForRange(
                        octxt, folderId, getItemType(), rangeStart, rangeEnd);
                if (result != null) {
    	            for (Iterator<CalendarItemData> itemIter = result.data.calendarItemIterator(); itemIter.hasNext(); ) {
    	            	CalendarItemData calItemData = itemIter.next();
                        int numInstances = calItemData.getNumInstances();
                        if (numInstances > 0) {
                            Element calItemElem = CacheToXML.encodeCalendarItemData(
                                    zsc, ifmt, calItemData, result.allowPrivateAccess, true);
                            response.addElement(calItemElem);
                        }
    	            }
                }
            } else {
                List<CalendarDataResult> calDataResultList = mbox.getAllCalendarsSummaryForRange(
                        octxt, getItemType(), rangeStart, rangeEnd);
                for (CalendarDataResult result : calDataResultList) {
                    for (Iterator<CalendarItemData> itemIter = result.data.calendarItemIterator(); itemIter.hasNext(); ) {
                        CalendarItemData calItemData = itemIter.next();
                        int numInstances = calItemData.getNumInstances();
                        if (numInstances > 0) {
                            Element calItemElem = CacheToXML.encodeCalendarItemData(
                                    zsc, ifmt, calItemData, result.allowPrivateAccess, true);
                            response.addElement(calItemElem);
                        }
                    }
                }
            }
        } else {
	        Collection<CalendarItem> calItems = mbox.getCalendarItemsForRange(
	                octxt, getItemType(), rangeStart, rangeEnd, iidFolder.getId(), null);
	        for (CalendarItem calItem : calItems) {
	            EncodeCalendarItemResult encoded = encodeCalendarItemInstances(
	                    zsc, octxt, calItem, acct, rangeStart, rangeEnd, false);
	            if (encoded.element != null)
	                response.addElement(encoded.element);
	        }
        }
        
        return response;
    }
}
