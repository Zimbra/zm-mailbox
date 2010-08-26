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

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import com.zimbra.cs.util.tnef.mapi.GlobalObjectId;
import com.zimbra.cs.util.tnef.mapi.MapiPropertyId;
import com.zimbra.cs.util.tnef.mapi.MeetingTypeFlag;
import com.zimbra.cs.util.tnef.mapi.RecurrenceDefinition;
import com.zimbra.cs.util.tnef.mapi.TZRule;
import com.zimbra.cs.util.tnef.mapi.TaskMode;
import com.zimbra.cs.util.tnef.mapi.TaskStatus;
import com.zimbra.cs.util.tnef.mapi.TimeZoneDefinition;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.property.Clazz;
import net.freeutils.tnef.Attachment;
import net.freeutils.tnef.Attr;
import net.freeutils.tnef.CompressedRTFInputStream;
import net.freeutils.tnef.MAPIProps;
import net.freeutils.tnef.MAPIValue;
import net.freeutils.tnef.Message;
import net.freeutils.tnef.RawInputStream;
import net.freeutils.tnef.TNEFInputStream;
import net.freeutils.tnef.TNEFUtils;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.util.tnef.mapi.AppointmentStateFlags;
import com.zimbra.cs.util.tnef.mapi.BusyStatus;
import java.util.EnumSet;
/**
 * The <code>SchedulingViewOfTnef</code> class encapsulates the Scheduling related
 * aspects of a TNEF file.
 *
 * A cut down version of net.freeutils.tnef.Message which ignores attributes
 * and attachments which are not regarded as important for Zimbra scheduling.
 *
 * @author Gren Elliot
 */
public class SchedulingViewOfTnef extends Message {

    static Log sLog = ZimbraLog.tnef;

    private String messageClass;
    private GlobalObjectId globalObjectId;
    private GlobalObjectId cleanGlobalObjectId;
    private boolean tzinfoInitialized;
    private TimeZoneDefinition startTimeTZinfo;
    private TimeZoneDefinition endTimeTZinfo;
    private TimeZoneDefinition recurrenceTZinfo;
    
    private EnumSet <AppointmentStateFlags> appointmentStateFlagsMask;
    private TaskMode taskMode;
    private TaskStatus taskStatus;
    private Integer percentComplete;
    private DateTime dateTaskCompleted;
    private ICALENDAR_TYPE icalType;

    public SchedulingViewOfTnef() {
        List <Attr> attribs = new ArrayList <Attr>();
        List <Attachment> attaches = new ArrayList <Attachment>();
        setAttributes(attribs);
        setAttachments(attaches);
        messageClass = null;
        globalObjectId = null;
        cleanGlobalObjectId = null;
        appointmentStateFlagsMask = null;
        tzinfoInitialized = false;
        startTimeTZinfo = null;
        endTimeTZinfo = null;
        recurrenceTZinfo = null;
        taskMode = null;
        taskStatus = null;
        percentComplete = null;
        icalType = null;
    }

    /**
     * Constructs a SchedulingView of the attributes in the supplied
     * TNEFInputStream.
     *
     * @param in the TNEFInputStream containing message data
     * @throws IOException if an I/O error occurs
     */
    public SchedulingViewOfTnef(TNEFInputStream in) throws IOException {
        this();
        read(in);
    }

    /**
     * Gathers scheduling related information from the TNEFInputStream.
     *
     * @param in the TNEFInputStream containing message data
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void read(TNEFInputStream in) throws IOException {
        Attr attr;
        Attachment attachmnt = null;
        while ((attr = in.readAttr()) != null) {
            switch (attr.getLevel()) {
                case Attr.LVL_ATTACHMENT:
                    switch (attr.getID()) {
                        case Attr.attAttachRenddata:
                            if (attachmnt != null) {
                                super.addAttachment(attachmnt);
                            }
                            attachmnt = new Attachment();
                            attachmnt.addAttribute(attr);
                            break;
                        case Attr.attAttachment:
                            MAPIProps mps = new MAPIProps((RawInputStream)attr.getValue());
                            attachmnt.setMAPIProps(mps);
                            break;
                        case Attr.attAttachData:
                            RawInputStream ris = (RawInputStream)attr.getValue();
                            attachmnt.setRawData(ris);
                            break;
                        case Attr.attAttachTransportFilename:
                            RawInputStream data = (RawInputStream)attr.getValue();
                            String filename = TNEFUtils.removeTerminatingNulls(
                                    new String(data.toByteArray(), super.getOEMCodePage())
                                    );
                            attachmnt.setFilename(filename);
                            break;
                        default:
                            attachmnt.addAttribute(attr);
                            break;
                    } // switching on ID for LVL_ATTACHMENT
                    break;
                case Attr.LVL_MESSAGE:
                    super.addAttribute(attr);
                    break;
                default:
                    throw new IOException("Invalid attribute level: " + attr.getLevel());
            } // switch level
        } // while

        if (attachmnt != null) {
            super.addAttachment(attachmnt);
        }
    }

    /**
     * @return the value of the attMessageClass TNEF attribute
     * @throws IOException
     */
    public String getMessageClass() throws IOException {
        if (messageClass != null) {
            return messageClass;
        }
        Attr attMsgClass = getAttr(Attr.attMessageClass);
        if (attMsgClass == null) {
            return null;
        }
        messageClass = attMsgClass.getValue().toString();
        return messageClass;
    }

    /**
     * @return A valid ICAL UID
     * @throws IOException
     */
    public String getIcalUID() throws IOException {
        String currUid;
        // For Meeting related objects, the GlobalObjectIds
        // should contain the information needed to form the UID
        // regardless of whether the original UID was chosen by
        // Microsoft products or third party ones.
        getGlobalObjectId();
        if (globalObjectId != null) {
            return globalObjectId.getIcalUid();
        }
        getCleanGlobalObjectId();
        if (cleanGlobalObjectId != null) {
            return cleanGlobalObjectId.getIcalUid();
        }
        // The only known unique handle for an Outlook task
        // is PidLidTaskGlobalId
        // which is documented as being a GUID and thus is not able
        // to wrap third party UIDs.
        // It is possible that some code paths might lead to the setting
        // of {00020329-0000-0000-C000-000000000046}/urn:schemas:calendar:uid
        // so, look for that.
        currUid = getUrnSchemasCalendarUid();
        if (currUid != null) {
            return currUid;
        }
        return getUidFromPidLidTaskGlobalId();
    }

    /**
     * @return A valid ICAL Sequence Number - 0 if the underlying property is absent.
     * @throws IOException
     */
    public Integer getSequenceNumber() throws IOException {
        if (this.getIcalType() == ICALENDAR_TYPE.VTODO) {
            Integer retVal = MapiPropertyId.PidLidTaskVersion.getIntegerValue(this);
            if (retVal != null) {
                return retVal;
            }
        }
        // PidLidAppointmentSequence - Specifies the sequence number of a Meeting object.
        return MapiPropertyId.PidLidAppointmentSequence.getIntegerValue(this, 0 /* default */);
    }

    /**
     * @return Value of PidTagImportance property
     * @throws IOException
     */
    public Integer getMapiImportance() throws IOException {
        return MapiPropertyId.PidTagImportance.getIntegerValue(this);
    }

    /**
     * The PidTagOwnerAppointmentId has the alternate name PR_OWNER_APPT_ID
     * @return Value of PidTagOwnerAppointmentId
     * @throws IOException
     */
    public Integer getOwnerAppointmentId() throws IOException {
        return MapiPropertyId.PidTagOwnerAppointmentId.getIntegerValue(this);
    }

    /**
     * Canonical name: PidTagSensitivity - Property ID: 0x0036
     *    MS-OXICAL recommends ICAL Class <--> PidTagSensitivity mapping :
     *        CLASS           PidTagSensitivity
     *        PRIVATE         0x00000002
     *        CONFIDENTIAL    0x00000003
     *        PUBLIC          0x00000000
     *        X-PERSONAL      0x00000001
     * @return Value of PidTagSensitiviy property
     * @throws IOException
     */
    public Clazz getIcalClass() throws IOException {
        Integer mapiSensitivity = getMapiSensitivity();
        if (mapiSensitivity == null) {
            return Clazz.PUBLIC;
        }
        if (mapiSensitivity == 0) {
            return Clazz.PUBLIC;
        } else if (mapiSensitivity == 1) {
            // not sure X-PERSONAL will be well supported
            return Clazz.PRIVATE;
        } else if (mapiSensitivity == 2) {
            return Clazz.PRIVATE;
        } else if (mapiSensitivity == 3) {
            return Clazz.CONFIDENTIAL;
        }
        return Clazz.PUBLIC;
    }

    /**
     * Canonical name: PidTagSensitivity - Property ID: 0x0036
     *    MS-OXICAL recommends ICAL Class <--> PidTagSensitivity mapping :
     *        CLASS           PidTagSensitivity
     *        PRIVATE         0x00000002
     *        CONFIDENTIAL    0x00000003
     *        PUBLIC          0x00000000
     *        X-PERSONAL      0x00000001
     * @return Value of PidTagSensitiviy property
     * @throws IOException
     */
    public Integer getMapiSensitivity() throws IOException {
        return MapiPropertyId.PidTagSensitivity.getIntegerValue(this);
    }

    /**
     * PidLidBusyStatus Specifies the availability of a user for the event
     * described by the object.
     *    MS-OXICAL recommends PidLidBusyStatus <--> ICAL TRANSP mapping :
     *  Mapping to TRANSP : 0x00000000 TRANSPARENT
     *                      0x00000001 OPAQUE
     *                      0x00000002 OPAQUE
     *                      0x00000003 OPAQUE
     * @return Value of PidLidBusyStatus property
     * @throws IOException
     */
    public BusyStatus getBusyStatus() throws IOException {
        Integer intVal = MapiPropertyId.PidLidBusyStatus.getIntegerValue(this);
        if (intVal == null) {
            return null;
        }
        for (BusyStatus curr : BusyStatus.values()) {
            if (curr.mapiPropValue() == intVal) {
                return curr;
            }
        }
        return null;
    }

    /**
     * PidLidIntendedBusyStatus - Contains the value of the PidLidBusyStatus
     * property on the Meeting object in the organizer's calendar at the time
     * the Meeting Request object or Meeting Update object was sent.
     * @return Value of PidLidIntendedBusyStatus property
     * @throws IOException
     */
    public BusyStatus getIntendedBusyStatus() throws IOException {
        Integer intVal = MapiPropertyId.PidLidIntendedBusyStatus.getIntegerValue(this);
        if (intVal == null) {
            return null;
        }
        for (BusyStatus curr : BusyStatus.values()) {
            if (curr.mapiPropValue() == intVal) {
                return curr;
            }
        }
        return null;
    }

    /**
     * PidLidAppointmentStateFlags - Specifies a bit field that describes the state of the object.
     * This property is not required. The following are the individual flags that can be set.
     * (asfMeeting, 0x00000001): object is a Meeting object or a meeting related object.
     * (asfReceived, 0x00000002): object was received from someone else.
     * (asfCanceled, 0x00000004): Meeting object that is represented by the object has been cancelled.
     * @return Value representing known flags in PidLidAppointmentStateFlags property
     * @throws IOException
     */
    public EnumSet <AppointmentStateFlags> getAppointmentStateFlags() throws IOException {
        if (appointmentStateFlagsMask != null) {
            return appointmentStateFlagsMask;
        }
        appointmentStateFlagsMask = EnumSet.noneOf(AppointmentStateFlags.class);
        Integer apptFlags = MapiPropertyId.PidLidAppointmentStateFlags.getIntegerValue(this);
        if (apptFlags == null) {
            return null;
        }
        if ( (apptFlags & 0x00000001) == 0x00000001) {
            appointmentStateFlagsMask.add(AppointmentStateFlags.ASF_MEETING);
        }
        if ( (apptFlags & 0x00000002) == 0x00000002) {
            appointmentStateFlagsMask.add(AppointmentStateFlags.ASF_RECEIVED);
        }
        if ( (apptFlags & 0x00000004) == 0x00000004) {
            appointmentStateFlagsMask.add(AppointmentStateFlags.ASF_CANCELED);
        }
        return appointmentStateFlagsMask;
    }

    /**
     * @return whether responses are desired for this calendaring object or not
     * @throws IOException
     */
    public boolean getResponseRequested() throws IOException {
        Boolean responseRequested =
            MapiPropertyId.PidTagResponseRequested.getBooleanValue(this);
        if (responseRequested != null) {
            return responseRequested;
        }
        Boolean replyRequested =
            MapiPropertyId.PidTagReplyRequested.getBooleanValue(this);
        if (replyRequested != null) {
            return replyRequested;
        }
        return false;
    }

    /**
     * PidLidAppointmentSubType - Specifies whether the event is an all-day event.
     * @return value of PidLidAppointmentSubType property or false if absent
     * @throws IOException
     */
    public Boolean isAllDayEvent() throws IOException {
        return MapiPropertyId.PidLidAppointmentSubType.getBooleanValue(this, false);
    }

    /**
     * PidLidAppointmentCounterProposal Indicates whether a Meeting Response
     * object is a counter proposal.
     * @return value of PidLidAppointmentCounterProposal or false if absent
     * @throws IOException
     */
    public Boolean isCounterProposal() throws IOException {
        return MapiPropertyId.PidLidAppointmentCounterProposal.getBooleanValue(this, false);
    }

    /**
     * PidLidAppointmentNotAllowPropose - Indicates whether attendees are not
     * allowed to propose a new date and/or time for the meeting.
     * @return value of PidLidAppointmentNotAllowPropose or null
     * @throws IOException
     */
    public Boolean isDisallowCounter() throws IOException {
        return MapiPropertyId.PidLidAppointmentNotAllowPropose.getBooleanValue(this);
    }

    /**
     * @return value of PidLidLocation property or null if absent
     * @throws IOException
     */
    public String getLocation() throws IOException {
        return MapiPropertyId.PidLidLocation.getStringValue(this);
    }

    /**
     * @return value of PidNameKeywords property or null if absent
     * @throws IOException
     */
    public List <String> getCategories() throws IOException {
        MAPIValue[] values = MapiPropertyId.PidNameKeywords.getValues(this);
        if (values == null) {
            return null;
        }
        ArrayList <String> categories = new ArrayList <String> ();
        for (MAPIValue val:values) {
            categories.add(val.toString());
        }
        return categories;
    }

    public String getMileage() throws IOException {
        return MapiPropertyId.PidLidMileage.getStringValue(this);
    }

    public String getBillingInfo() throws IOException {
        return MapiPropertyId.PidLidBilling.getStringValue(this);
    }

    public String getCompanies() throws IOException {
        return MapiPropertyId.PidLidCompanies.getStringValue(this);
    }

    public Integer getEstimatedEffort() throws IOException {
        Integer myVal = MapiPropertyId.PidLidTaskEstimatedEffort.getIntegerValue(this);
        if ( (myVal == null) || (myVal == 0) ) {
            return null;
        }
        return myVal;
    }

    public Integer getActualEffort() throws IOException {
        Integer myVal = MapiPropertyId.PidLidTaskActualEffort.getIntegerValue(this);
        if ( (myVal == null) || (myVal == 0) ) {
            return null;
        }
        return myVal;
    }

    /**
     * PidLidOwnerCriticalChange - Specifies the date and time at which a Meeting
     * Request object was sent by the organizer.
     * @return the time in UTC that the meeting object was sent or null
     * @throws IOException
     */
    public DateTime getOwnerCriticalChange() throws IOException {
        return MapiPropertyId.PidLidOwnerCriticalChange.getDateTimeAsUTC(this);
    }

    /**
     * PidLidAppointmentReplyTime - Specifies the date and time at which the
     * attendee responded to a received meeting request or Meeting Update object.
     * @return Value of PidLidAppointmentReplyTime -
     * @throws IOException
     */
    public DateTime getAppointmentReplyTime() throws IOException {
        return MapiPropertyId.PidLidAppointmentReplyTime.getDateTimeAsUTC(this);
    }

    /**
     * PidLidReminderDelta is the Time before the start of an appointment in minutes
     * at which an alarm should be raised.
     * If PidLidReminderDelta is set to 0x5AE980E1, this is a synonym for
     * 15 minutes before the start.
     *
     * @return
     * @throws IOException
     */
    public Integer getReminderDelta() throws IOException {
        Integer reminderDelta =
            MapiPropertyId.PidLidReminderDelta.getIntegerValue(this);
        if ( (reminderDelta != null) && (reminderDelta == 0x5AE980E1) ) {
            reminderDelta= 15;
        }
        return reminderDelta;
    }

    /**
     * 
     * @return value of PidLidReminderSet property which specifies whether
     * a reminder is set on the object.
     * @throws IOException
     */
    public boolean getReminderSet() throws IOException {
        // Specifies the start date and time of the event in UTC
        return MapiPropertyId.PidLidReminderSet.getBooleanValue(this, false);
    }

    /**
     * @return the time in UTC that the meeting object was sent or null
     * @throws IOException
     */
    public DateTime getStartTime() throws IOException {
        // Specifies the start date and time of the event in UTC
        DateTime timeVal = null;
        if (this.getIcalType() == ICALENDAR_TYPE.VTODO) {
            /* PidLidTaskStartDate Unset or has value 0x5AE980E0 
             *        --> task does not have a start date
             */
            Long taskStartDateNum = 
                MapiPropertyId.PidLidTaskStartDate.get100nsPeriodsSince1601(this);
            if (taskStartDateNum == null) {
                return null;
            }
            if (taskStartDateNum == 0x5AE980E0) {
                sLog.debug("PidLidTaskStartDate as num 0x%s [means NO start date]",
                        Long.toHexString(taskStartDateNum));
                return null;
            }
            timeVal =
                MapiPropertyId.PidLidTaskStartDate.getDateTimeAsUTC(this);
        } else {
            // PidLidAppointmentStartWhole - UTC start date and time for the event.
            timeVal =
                MapiPropertyId.PidLidAppointmentStartWhole.getDateTimeAsUTC(this);
        }
        if (timeVal == null) {
            timeVal = MapiPropertyId.PidTagStartDate.getDateTimeAsUTC(this);
        }
        return timeVal;
    }

    /**
     * PidLidAppointmentEndWhole - Specifies the end date and time for the event.
     * @return the time in UTC that the meeting object was sent or null
     * @throws IOException
     */
    public DateTime getEndTime() throws IOException {
        DateTime timeVal =
            MapiPropertyId.PidLidAppointmentEndWhole.getDateTimeAsUTC(this);
        if (timeVal == null) {
            timeVal = MapiPropertyId.PidTagEndDate.getDateTimeAsUTC(this);
        }
        return timeVal;
    }

    /**
     * @return the time in UTC that the meeting object was sent or null
     * @throws IOException
     */
    public DateTime getDueDate() throws IOException {
        DateTime timeVal = null;
        if (this.getIcalType() != ICALENDAR_TYPE.VTODO) {
            return null;
        }
        timeVal =
            MapiPropertyId.PidLidTaskDueDate.getDateTimeAsUTC(this);
        return timeVal;
    }

    /**
     * Candidate properties :
     *
     * PidLidExceptionReplaceTime specifies UTC date and time
     * within a recurrence pattern that an exception will replace.
     *
     * @return A suitable time for the RECURRENCE-ID property for this
     * calendaring item if one exists.
     * @throws IOException
     */
    public DateTime getRecurrenceIdTime() throws IOException {
        // Note: Was assuming PidLidOldWhenStartWhole was a good candidate
        //       but that is used for non-recurrence related objects
        //       to give the previous start time of a single appointment.
        DateTime recurrenceIdTime =
            MapiPropertyId.PidLidExceptionReplaceTime.getDateTimeAsUTC(this);
        if (recurrenceIdTime != null) {
            sLog.debug("RECURRENCE-ID taken from PidLidExceptionReplaceTime");
        } else {
            getGlobalObjectId();
            if (this.globalObjectId == null) {
                return null;
            }
            int origYear = globalObjectId.getOrigInstanceYear();
            if (origYear == 0) {
                return null;
            }
            int origMonth = globalObjectId.getOrigInstanceMonth();
            int origDay = globalObjectId.getOrigInstanceDay();
            // PidLidStartRecurrenceTime identifies start time of the recurrence pattern.
            Integer recurTime =
                MapiPropertyId.PidLidStartRecurrenceTime.getIntegerValue(this);
            int secs = 0;
            int mins = 0;
            int hrs = 0;
            if (recurTime != null) {
                // bottom 6 bits are seconds, next 6 bits are minutes, rest is hours
                secs = recurTime & 0x0000003f;
                mins = (recurTime & 0x00000fc0) >>6;
                hrs = (recurTime & 0xfffff000) >>12;
                sLog.debug("RECURRENCE-ID taken from GlobalObjectId/PidLidStartRecurrenceTime");
            } else {
                sLog.debug("RECURRENCE-ID taken from JUST GlobalObjectId");
            }
            try {
                String dateString;
                if (recurrenceTZinfo == null) {
                    dateString = String.format("%04d%02d%02dT%02d%02d%02dZ",
                        origYear, origMonth, origDay, hrs, mins, secs);
                    recurrenceIdTime = new DateTime(dateString);
                } else {
                    dateString = String.format("%04d%02d%02dT%02d%02d%02d",
                        origYear, origMonth, origDay, hrs, mins, secs);
                    recurrenceIdTime = new DateTime(dateString, recurrenceTZinfo.getTimeZone());
                }
            } catch (ParseException e) {
                sLog.debug("Problem constructing recurrence-id time", e);
            }
        }
        return recurrenceIdTime;
    }

    /**
     * @return PidLidAppointmentProposedStartWhole which specifies the proposed
     *         value for PidLidAppointmentStartWhole for a counter proposal.
     * @throws IOException
     */
    public DateTime getProposedStartTime() throws IOException {
        return MapiPropertyId.PidLidAppointmentProposedStartWhole.getDateTimeAsUTC(this);
    }

    /**
     * PidLidAppointmentProposedEndWhole - Specifies the proposed
     *         value for PidLidAppointmentEndWhole for a counter proposal.
     * @return 
     * @throws IOException
     */
    public DateTime getProposedEndTime() throws IOException {
        return MapiPropertyId.PidLidAppointmentProposedEndWhole.getDateTimeAsUTC(this);
    }

    /**
     * PidLidAttendeeCriticalChange - Specifies the date and time at which the meeting-related object was sent.
     * @return the time in UTC that the meeting-related object was sent or null
     * @throws IOException
     */
    public DateTime getAttendeeCriticalChange() throws IOException {
        return MapiPropertyId.PidLidAttendeeCriticalChange.getDateTimeAsUTC(this);
    }

    /**
     * @return TimeZone information relevant to the start of an appointment.
     * @throws IOException
     */
    public TimeZoneDefinition getStartDateTimezoneInfo() throws IOException {
        initTZinfo();
        return startTimeTZinfo;
    }

    /**
     * @return TimeZone information relevant to the end of an appointment.
     * @throws IOException
     */
    public TimeZoneDefinition getEndDateTimezoneInfo() throws IOException {
        initTZinfo();
        return endTimeTZinfo;
    }

    /**
     * @return TimeZone information relevant to the rule of a recurrence.
     * @throws IOException
     */
    public TimeZoneDefinition getRecurrenceTimezoneInfo() throws IOException {
        initTZinfo();
        return recurrenceTZinfo;
    }

    /**
     * PidLidTimeZoneDescription Specifies a human-readable description
     * of the time zone that is represented by the data in the
     * PidLidTimeZoneStruct property.
     * @return value of PidLidTimeZoneDescription property or null if absent
     * @throws IOException
     */
    public String getTimeZoneDescription() throws IOException {
        return MapiPropertyId.PidLidTimeZoneDescription.getStringValue(this);
    }

    public RecurrenceDefinition getRecurrenceDefinition(
            TimeZoneDefinition tzDef) throws IOException, TNEFtoIcalendarServiceException {
        RawInputStream tzRis;
        if (this.getIcalType() == ICALENDAR_TYPE.VTODO) {
            tzRis = MapiPropertyId.PidLidTaskRecurrence.getRawInputStreamValue(this);
        } else {
            tzRis = MapiPropertyId.PidLidAppointmentRecur.getRawInputStreamValue(this);
        }
        if (tzRis == null) {
            return null;
        }
        String oemCodePage = super.getOEMCodePage();
        RecurrenceDefinition recurDef = new RecurrenceDefinition(tzRis, tzDef, oemCodePage);
        return recurDef;
    }

    /**
     * Useful for X-ALT-DESC?  Would need to convert to HTML, which, even for wrapped HTML
     * which isn't necessarily common place, isn't 100% straight forward.
     * 
     * @return
     * @throws IOException
     */
    public String getRTF() throws IOException {
        RawInputStream ris = MapiPropertyId.PidTagRtfCompressed.getRawInputStreamValue(this);
        if (ris == null) {
            sLog.debug("No PR_RTF_COMPRESSED property found");
            return null;
        }
        // Question - does the OEMCodePage for the TNEF need to be factored in
        String rtfTxt = new String(
                CompressedRTFInputStream.decompressRTF(
                        ris.toByteArray()));
        if (sLog.isDebugEnabled()) {
            sLog.debug("RTF from PR_RTF_COMPRESSED\n%s\n", rtfTxt);
        }
        return rtfTxt;
    }

    /**
     *  Gets an attribute with the given id.
     *
     * @param attrib is the TNEF attribute ID
     * @return the requested attribute, or null if no such attribute exists
     * @throws IOException
     */
    public Attr getAttr(int attrib) throws IOException {
        List <?> attribs = (List <?>) super.getAttributes();
        for (Object thisObj : attribs) {
            if (! (thisObj instanceof Attr)) {
                continue;
            }
            Attr thisAtt = (Attr) thisObj;
            if (thisAtt.getID() == (attrib)) {
                return thisAtt;
            }
        }
        return null;
    }
    /**
     * PidLidGlobalObjectId is The unique identifier of the Calendar object.
     * After it is set for a Calendar object, the value of this property MUST NOT change.
     * 
     * @return value of PidLidGlobalObjectId property
     */
    private GlobalObjectId getGlobalObjectId() {
        if (globalObjectId != null) {
            return globalObjectId;
        }
        globalObjectId = this.getGlobalObjectIdType(MapiPropertyId.PidLidGlobalObjectId);
        return globalObjectId;
    }

    /**
     * PidLidCleanGlobalObjectId
     * The format of this property is the same as that of PidLidGlobalObjectId.
     * The value of this property MUST be equal to the value of PidLidGlobalObjectId,
     * except the YH, YL, M, and D fields MUST all be zero.
     * All objects that refer to an instance of a recurring series (including
     * an orphan instance), as well as the recurring series itself, will have the
     * same value for this property.
     * 
     * @return value of PidLidCleanGlobalObjectId property
     */
    private GlobalObjectId getCleanGlobalObjectId() {
        if (cleanGlobalObjectId != null) {
            return cleanGlobalObjectId;
        }
        cleanGlobalObjectId = this.getGlobalObjectIdType(
                MapiPropertyId.PidLidCleanGlobalObjectId);
        return cleanGlobalObjectId;
    }

    private GlobalObjectId getGlobalObjectIdType(MapiPropertyId mpi) {
        GlobalObjectId gid = null;
        RawInputStream ris;
        try {
            ris = mpi.getRawInputStreamValue(this);
            if (ris != null) {
                gid = new GlobalObjectId(ris);
            }
        } catch (IOException e) {
            sLog.debug("Problem getting value of MAPI property " + mpi.toString() + " from TNEF", e);
        }
        return gid;
    }

    public EnumSet <MeetingTypeFlag> getMeetingTypeFlags() throws IOException {
        EnumSet <MeetingTypeFlag> meetingTypeFlags = EnumSet.noneOf(MeetingTypeFlag.class);
        Integer mtgType = MapiPropertyId.PidLidMeetingType.getIntegerValue(this);
        if (mtgType != null) {
            for (MeetingTypeFlag curr : MeetingTypeFlag.values()) {
                int currFlagBit = curr.mapiFlagBit();
                if ( (mtgType & currFlagBit) == currFlagBit) {
                    meetingTypeFlags.add(curr);
                }
            }
        }
        return meetingTypeFlags;
    }

    /**
     * @return PidNameCalendarUid value if set.  Normal Outlook MAPI working does not set it.
     * @throws IOException
     */
    private String getUrnSchemasCalendarUid() throws IOException {
        return MapiPropertyId.PidNameCalendarUid.getStringValue(this);
    }

    /**
     * For a Task, PidLidTaskGlobalId contains a unique key.
     * As Outlook 2007 and earlier don't send assigned tasks using ICAL
     * in the same way as meeting requests can be sent and there doesn't
     * appear to be a way to save as ICAL, it isn't totally clear what
     * is the best source of a UID.  This property is a GUID, so does NOT
     * have the ability to encode third party UIDs in the same way as
     * GlobalObjectIds do;
     * 
     * @return 
     * @throws IOException 
     */
    private String getUidFromPidLidTaskGlobalId() throws IOException {
        byte[] theVal = MapiPropertyId.PidLidTaskGlobalId.getByteArrayValue(this);
        if (theVal == null) {
            return null;
        }
        return IcalUtil.toHexString(theVal, 0, theVal.length);
    }

    /**
     * @param icalType the icalType to set
     */
    public void setIcalType(ICALENDAR_TYPE icalType) {
        this.icalType = icalType;
    }

    /**
     * @return the icalType
     * @throws IOException 
     */
    public ICALENDAR_TYPE getIcalType() throws IOException {
        if (icalType == null) {
            String msgClass = this.getMessageClass();
            if (msgClass.startsWith("IPM.Task")) {
                icalType = ICALENDAR_TYPE.VTODO;
            } else {
                icalType = ICALENDAR_TYPE.VEVENT;
            }
        }
        return icalType;
    }

    /**
     * @return Value of PidLidTaskMode property
     * @throws IOException
     */
    public TaskMode getTaskMode() throws IOException {
        initTaskStatusInfo();
        return taskMode;
    }

    /**
     * @return Value of PidLidTaskStatus property
     * @throws IOException
     */
    public TaskStatus getTaskStatus() throws IOException {
        initTaskStatusInfo();
        return taskStatus;
    }

    /**
     * @return The value of PidLidPercentComplete adjusted to be between 0 and 100
     * @throws IOException
     */
    public int getPercentComplete() throws IOException {
        initTaskStatusInfo();
        if (percentComplete == null) {
            return 0;
        } else {
            return percentComplete;
        }
    }

    /**
     * @return the time in UTC that the meeting object was sent or null
     * @throws IOException
     */
    public DateTime getDateTaskCompleted() throws IOException {
        initTaskStatusInfo();
        return dateTaskCompleted;
    }

    private void initTaskStatusInfo() throws IOException {
        if (this.getIcalType() != ICALENDAR_TYPE.VTODO) {
            return;
        }
        if (percentComplete != null) {
            return;
        }
        dateTaskCompleted =
            MapiPropertyId.PidLidTaskDateCompleted.getDateTimeAsUTC(this);
        Integer intVal;
        taskMode = TaskMode.TASK_REQUEST;
        intVal = MapiPropertyId.PidLidTaskMode.getIntegerValue(this);
        if (intVal != null) {
            for (TaskMode curr : TaskMode.values()) {
                if (curr.mapiPropValue() == intVal) {
                    taskMode = curr;
                    break;
                }
            }
        }
        if (dateTaskCompleted != null) {
            taskStatus = TaskStatus.COMPLETE;
            percentComplete = new Integer(100);
        } else {
            taskStatus = TaskStatus.NOT_STARTED;
            intVal = MapiPropertyId.PidLidTaskStatus.getIntegerValue(this);
            if (intVal != null) {
                for (TaskStatus curr : TaskStatus.values()) {
                    if (curr.mapiPropValue() == intVal) {
                        taskStatus = curr;
                        break;
                    }
                }
            }
            Double fractionComplete;
            fractionComplete = MapiPropertyId.PidLidPercentComplete.getDoubleValue(this);
            if ( (fractionComplete == null) || (fractionComplete < 0) || (fractionComplete > 1) ) {
                percentComplete = new Integer(0);
            } else {
                fractionComplete = fractionComplete * 100;
                percentComplete = (int) Math.round(fractionComplete);
            }
        }
    }

    /**
     * Initialise timezone related fields.
     * Replies from Outlook 2007 related to a recurrence or an instance
     * of a recurrence had :
     *     PidLidTimeZoneDescription,PidLidTimeZoneStruct,
     *     PidLidAppointmentTimeZoneDefinitionStartDisplay and
     *     PidLidAppointmentTimeZoneDefinitionEndDisplay present but NOT
     *     PidLidAppointmentTimeZoneDefinitionRecur
     * further, the StartDisplay/EndDisplay properties were appropriate to the
     * Outlook client replying and were NOT related to the originally sent ICAL.
     *
     * The Outlook originated TimeZone names associated with
     * StartDisplay/EndDisplay/Recur props seem "nicer" than the names
     * used in PidLidTimeZoneDescription - so, tend to prefer those.
     *
     * @throws IOException
     */
    private void initTZinfo() {
        if (tzinfoInitialized) {
            return;
        }

        try {
            RawInputStream tzRis;
            MapiPropertyId mpi;
            mpi = MapiPropertyId.PidLidAppointmentTimeZoneDefinitionStartDisplay;
            tzRis =  mpi.getRawInputStreamValue(this);
            if (tzRis != null) {
                startTimeTZinfo = new TimeZoneDefinition(mpi, tzRis);
            }
            mpi = MapiPropertyId.PidLidAppointmentTimeZoneDefinitionEndDisplay;
            tzRis =  mpi.getRawInputStreamValue(this);
            if (tzRis != null) {
                endTimeTZinfo = new TimeZoneDefinition(mpi, tzRis);
            }
            mpi = MapiPropertyId.PidLidAppointmentTimeZoneDefinitionRecur;
            tzRis =  mpi.getRawInputStreamValue(this);
            if (tzRis != null) {
                recurrenceTZinfo = new TimeZoneDefinition(mpi, tzRis);
            }
            String tzDesc = this.getTimeZoneDescription();
            if (null != tzDesc) {
                TimeZoneDefinition tzStructInfo = this.getTimeZoneStructInfo(tzDesc);
                if (tzStructInfo != null) {
                    // We know we have a recurrence related TZ definition.  Make
                    // sure we have the most appropriate/nice definition for that.
                    TZRule tzsRule = tzStructInfo.getEffectiveRule();
                    if (recurrenceTZinfo == null) {
                        if  (   (startTimeTZinfo != null) &&
                                (tzsRule.equivalentRule(
                                        startTimeTZinfo.getEffectiveRule())) ) {
                            recurrenceTZinfo = startTimeTZinfo;
                            sLog.debug("Using %s for TZ info",
                                    "PidLidAppointmentTimeZoneDefinitionStart");
                        } else if  (   (endTimeTZinfo != null) &&
                                (tzsRule.equivalentRule(
                                        endTimeTZinfo.getEffectiveRule())) ) {
                            recurrenceTZinfo = endTimeTZinfo;
                            sLog.debug("Using %s for TZ info",
                                    "PidLidAppointmentTimeZoneDefinitionEnd");
                        } else {
                            recurrenceTZinfo = tzStructInfo;
                            sLog.debug("Using %s for TZ info",
                                    "PidLidTimeZoneStruct");
                        }
                    } else if (!tzsRule.equivalentRule(
                                    recurrenceTZinfo.getEffectiveRule())) {
                        recurrenceTZinfo = tzStructInfo;
                        sLog.debug("Using %s for TZ info",
                                "PidLidAppointmentTimeZoneDefinitionRecur");
                    }
                }
            }
        } catch (IOException e) {
            sLog.debug("Problem encountered initialising timezone information", e);
        }

        if (recurrenceTZinfo != null) {
            // For recurrences, we want just one TZ for consistency
            if (endTimeTZinfo == null) {
                endTimeTZinfo = recurrenceTZinfo;
            } else if (recurrenceTZinfo.getEffectiveRule().equivalentRule(
                            endTimeTZinfo.getEffectiveRule())) {
                endTimeTZinfo = recurrenceTZinfo;
            } else if (startTimeTZinfo != null) {
                // Sometimes, the start and end timezones are different
                // for cancel/request, even when related to an exception.
                if (startTimeTZinfo.getEffectiveRule().equivalentRule(
                            endTimeTZinfo.getEffectiveRule())) {
                    endTimeTZinfo = recurrenceTZinfo;
                }
            }
            startTimeTZinfo = recurrenceTZinfo;
        }
        if (endTimeTZinfo == null) {
            endTimeTZinfo = startTimeTZinfo;
        } else if (startTimeTZinfo == null) {
            startTimeTZinfo = endTimeTZinfo ;
        } else if (startTimeTZinfo.getEffectiveRule().equivalentRule(
                            endTimeTZinfo.getEffectiveRule())) {
            endTimeTZinfo = startTimeTZinfo;
        }
    }

    /**
     * PidLidTimeZoneStruct Specifies time zone information for a recurring meeting.
     *
     * @param tzName 
     * @return
     * @throws IOException
     */
    private TimeZoneDefinition getTimeZoneStructInfo(String tzName) throws IOException {
        RawInputStream tzRis =
            MapiPropertyId.PidLidTimeZoneStruct.getRawInputStreamValue(this);
        if (tzRis == null) {
            return null;
        }
        TimeZoneDefinition tzDef = new TimeZoneDefinition(tzRis, tzName);
        if (tzDef == null) {
            if (sLog.isDebugEnabled()) {
                sLog.debug("Failed to load TimeZoneDefinition from PidLidTimeZoneStruct and " + tzName);
            }
        }
        return tzDef;
    }

}
