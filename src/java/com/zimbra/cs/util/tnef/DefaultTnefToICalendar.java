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
import java.io.InputStream;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.EnumSet;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import net.fortuna.ical4j.data.ContentHandler;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.parameter.CuType;
import net.fortuna.ical4j.model.parameter.PartStat;
import net.fortuna.ical4j.model.parameter.Related;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.parameter.Rsvp;
import net.fortuna.ical4j.model.property.Clazz;
import net.fortuna.ical4j.model.property.Method;
import net.fortuna.ical4j.model.property.Transp;
import net.freeutils.tnef.MAPIProp;
import net.freeutils.tnef.TNEFInputStream;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.util.tnef.TNEFtoIcalendarServiceException.UnsupportedTnefCalendaringMsgException;
import com.zimbra.cs.util.tnef.mapi.BusyStatus;
import com.zimbra.cs.util.tnef.mapi.ChangedInstanceInfo;
import com.zimbra.cs.util.tnef.mapi.ExceptionInfoOverrideFlag;
import com.zimbra.cs.util.tnef.mapi.RecurrenceDefinition;
import com.zimbra.cs.util.tnef.mapi.TimeZoneDefinition;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.SentBy;
import net.fortuna.ical4j.model.property.Action;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.DtStamp;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.Trigger;
import net.fortuna.ical4j.model.property.XProperty;

/**
 * @author gren
 *
 * The <code>DefaultTnefToICalendar</code> class is responsible for mining
 * a TNEF (WINMAIL.DAT) attachment (and, potentially, the MIME of the
 * containing message) to derive an ICALENDAR equivalent of the scheduling
 * information if applicable.
 */
public class DefaultTnefToICalendar implements TnefToICalendar {

    static Log sLog = ZimbraLog.tnef;

    private RecurrenceDefinition recurDef;
    private DtStamp dtstamp;
    private String uid;
    private Method method;

    /* (non-Javadoc)
     * @see com.zimbra.cs.util.tnef.TnefToICalendar#convert(java.io.InputStream, net.fortuna.ical4j.data.ContentHandler)
     */
    public boolean convert(MimeMessage mimeMsg, InputStream tnefInput, ContentHandler icalOutput)
            throws ServiceException {

        boolean conversionSuccessful = false;
        recurDef = null;
        TNEFInputStream tnefStream = null;
        SchedulingViewOfTnef schedView = null;
        Integer sequenceNum = 0;

        try {
            tnefStream = new TNEFInputStream(tnefInput);
            schedView = new SchedulingViewOfTnef(tnefStream);

            // String oemCodePage = schedView.getOEMCodePage();
            String msgClass = schedView.getMessageClass();
            if (msgClass == null) {
                sLog.debug("Unable to determine Class of TNEF - cannot generate ICALENDER equivalent");
                // throw TNEFtoIcalendarServiceException.NON_CALENDARING_CLASS(msgClass);
                return false;
            }
            uid = schedView.getIcalUID();
            sequenceNum = schedView.getSequenceNumber();
            boolean replyWanted = schedView.getResponseRequested();
            boolean reminderSet = schedView.getReminderSet();
            String location = schedView.getLocation();
            Boolean isAllDayEvent = schedView.isAllDayEvent();
            Boolean isCounterProposal = schedView.isCounterProposal();
            Integer importance = schedView.getMapiImportance();
            Clazz  icalClass = schedView.getIcalClass();
            Integer ownerApptId = schedView.getOwnerAppointmentId();
            BusyStatus busyStatus = schedView.getBusyStatus();
            BusyStatus intendedBusyStatus = schedView.getIntendedBusyStatus();
            TimeZoneDefinition startTimeTZinfo = schedView.getStartDateTimezoneInfo();
            TimeZoneDefinition endTimeTZinfo = schedView.getEndDateTimezoneInfo();
            TimeZoneDefinition recurrenceTZinfo = schedView.getRecurrenceTimezoneInfo();
            recurDef = schedView.getRecurrenceDefinition(recurrenceTZinfo);

            DateTime icalStartDate = schedView.getStartTime();
            DateTime icalEndDate = schedView.getEndTime();
            DateTime icalCreateDate = schedView.getUtcDateTime(null, MAPIProp.PR_CREATION_TIME);
            DateTime icalLastModDate = schedView.getUtcDateTime(null, MAPIProp.PR_LAST_MODIFICATION_TIME);
            DateTime recurrenceIdDateTime = schedView.getRecurrenceIdTime();
            DateTime attendeeCriticalChange = schedView.getAttendeeCriticalChange();
            DateTime ownerCriticalChange = schedView.getOwnerCriticalChange();
            String[] categories = schedView.getCategories();
            method = null;
            PartStat partstat = null;
            String descriptionText = null;
            String summary = null;
            if (mimeMsg != null) {
                summary = mimeMsg.getSubject();
                PlainTextFinder finder = new PlainTextFinder();
                finder.accept(mimeMsg);
                descriptionText = finder.getPlainText();
            }
            if (msgClass != null) {
                // IPM.Microsoft Schedule.MtgRespP IPM.Schedule.Meeting.Resp.Pos
                // IPM.Microsoft Schedule.MtgRespN IPM.Schedule.Meeting.Resp.Neg
                // IPM.Microsoft Schedule.MtgRespA IPM.Schedule.Meeting.Resp.Tent
                // IPM.Microsoft Schedule.MtgReq   IPM.Schedule.Meeting.Request
                // IPM.Microsoft Schedule.MtgCncl  IPM.Schedule.Meeting.Canceled

                // With forms capability, the standard class can have a '.<FORM>'
                // suffix in some scenarios where <FORM> is the form name.  Not
                // sure if this applies inside TNEF as well but using "startsWith".
                // just in case.
                if (msgClass.startsWith("IPM.Microsoft Schedule.MtgReq")) {
                    method = Method.REQUEST;
                    partstat = PartStat.NEEDS_ACTION;
                } else if (msgClass.startsWith("IPM.Microsoft Schedule.MtgRespP")) {
                    method = Method.REPLY;
                    partstat = PartStat.ACCEPTED;
                } else if (msgClass.startsWith("IPM.Microsoft Schedule.MtgRespN")) {
                    method = Method.REPLY;
                    partstat = PartStat.DECLINED;
                } else if (msgClass.startsWith("IPM.Microsoft Schedule.MtgRespA")) {
                    if ((isCounterProposal != null) && isCounterProposal) {
                        method = Method.COUNTER;
                    } else {
                        method = Method.REPLY;
                    }
                    partstat = PartStat.TENTATIVE;
                } else if (msgClass.startsWith("IPM.Microsoft Schedule.MtgCncl")) {
                    method = Method.CANCEL;
                }
            }

            if (method == null) {
                sLog.debug("Unable to map class %s to ICALENDER", msgClass);
                return false;
//                throw TNEFtoIcalendarServiceException.NON_CALENDARING_CLASS(msgClass);
            }

            icalOutput.startCalendar();
            // Results in a 2nd PRODID in iCalendar
            // IcalUtil.addProperty(icalOutput, Property.PRODID,
            //         "Zimbra-TNEF-iCalendar-Converter");
            IcalUtil.addProperty(icalOutput, method);
            if (recurDef != null) {
                String MsCalScale = recurDef.xMicrosoftCalscale();
                if ( (MsCalScale == null) || (MsCalScale.equals("")) ) {
                    IcalUtil.addProperty(icalOutput, CalScale.GREGORIAN);
                } else {
                    IcalUtil.addProperty(icalOutput, "X-MICROSOFT-CALSCALE", MsCalScale);
                }
            } else {
                IcalUtil.addProperty(icalOutput, CalScale.GREGORIAN);
            }
            String startTZname = null;
            String endTZname = null;
            String recurTZname = null;
            if (startTimeTZinfo != null) {
                startTZname = startTimeTZinfo.getTimezoneName();
                startTimeTZinfo.addVtimezone(icalOutput);
            }
            if (endTimeTZinfo != null) {
                endTZname = endTimeTZinfo.getTimezoneName();
                if ( (startTZname == null) || (! endTZname.equals(startTZname)) ) {
                    endTimeTZinfo.addVtimezone(icalOutput);
                }
            }

            if (recurrenceTZinfo != null) {
                recurTZname = recurrenceTZinfo.getTimezoneName();
                boolean addName = true;
                if ( (startTZname != null) && (recurTZname.equals(startTZname)) ) {
                    addName = false;
                }
                if ( (endTZname != null) && (recurTZname.equals(endTZname)) ) {
                    addName = false;
                }
                if (addName) {
                    recurrenceTZinfo.addVtimezone(icalOutput);
                }
            }

            icalOutput.startComponent(Component.VEVENT);
            //TODO - for text properties, need to handle line endings and strange
            //       characters - also, OemCodePage may need to be taken into account.
            if (uid == null) {
                // TODO: Would it be better to reject this?
                uid = new String("No-original-valid-UID");
            }

            IcalUtil.addProperty(icalOutput, Property.UID, uid);
            if ( (attendeeCriticalChange != null) &&
                    ( method.equals(Method.REPLY) ||
                      method.equals(Method.COUNTER) ) ) {
                dtstamp = new DtStamp(attendeeCriticalChange);
            } else if (ownerCriticalChange != null) {
                dtstamp = new DtStamp(ownerCriticalChange);
            } else {
                DateTime stampTime = new DateTime("20000101T000000Z");
                dtstamp = new DtStamp(stampTime);
            }
            IcalUtil.addProperty(icalOutput, dtstamp);
            IcalUtil.addProperty(icalOutput, Property.CREATED, icalCreateDate, false);
            IcalUtil.addProperty(icalOutput, Property.LAST_MODIFIED, icalLastModDate, false);
            IcalUtil.addProperty(icalOutput, Property.SEQUENCE, sequenceNum, false);
            IcalUtil.addProperty(icalOutput, Property.SUMMARY, summary, false);
            IcalUtil.addProperty(icalOutput, Property.LOCATION, location, false);
            IcalUtil.addProperty(icalOutput, Property.DESCRIPTION, descriptionText, false);
            if ( method.equals(Method.COUNTER) ) {
                IcalUtil.addPropertyFromUtcTimeAndZone(icalOutput, Property.DTSTART,
                        schedView.getProposedStartTime(), startTimeTZinfo, isAllDayEvent);
                IcalUtil.addPropertyFromUtcTimeAndZone(icalOutput, Property.DTEND,
                        schedView.getProposedEndTime(), endTimeTZinfo, isAllDayEvent);
                IcalUtil.addPropertyFromUtcTimeAndZone(icalOutput,
                        "X-MS-OLK-ORIGINALSTART",
                        icalStartDate, startTimeTZinfo, isAllDayEvent);
                IcalUtil.addPropertyFromUtcTimeAndZone(icalOutput,
                        "X-MS-OLK-ORIGINALEND",
                        icalEndDate, endTimeTZinfo, isAllDayEvent);
            } else {
                IcalUtil.addPropertyFromUtcTimeAndZone(icalOutput, Property.DTSTART,
                        icalStartDate, startTimeTZinfo, isAllDayEvent);
                IcalUtil.addPropertyFromUtcTimeAndZone(icalOutput, Property.DTEND,
                        icalEndDate, endTimeTZinfo, isAllDayEvent);
            }

            // TODO: RECURRENCE-ID only makes sense in relation to a recurrence. It isn't
            // just the original start date. Make sure we only output one if this is
            // related to a recurring series.
            IcalUtil.addPropertyFromUtcTimeAndZone(icalOutput, Property.RECURRENCE_ID,
                    recurrenceIdDateTime, startTimeTZinfo, isAllDayEvent);

            if (recurDef != null) {
                Property recurrenceProp =
                        recurDef.icalRecurrenceProperty(isAllDayEvent, false);
                IcalUtil.addProperty(icalOutput, recurrenceProp);
                for (DateTime exDate : recurDef.getExdates()) {
                    IcalUtil.addPropertyFromUtcTimeAndZone(icalOutput, Property.EXDATE,
                            exDate, startTimeTZinfo, isAllDayEvent);
                }
                for (DateTime rDate : recurDef.getRdates()) {
                    IcalUtil.addPropertyFromUtcTimeAndZone(icalOutput, Property.RDATE,
                            rDate, startTimeTZinfo, isAllDayEvent);
                }
            }

            if (importance != null) {
                if (importance == 2) {
                    importance = new Integer(1);
                } else if (importance == 1) {
                    importance = new Integer(5);
                } else if (importance == 0) {
                    importance = new Integer(9);
                }
                IcalUtil.addProperty(icalOutput, Property.PRIORITY, importance, false);
            }

            IcalUtil.addProperty(icalOutput, icalClass);

            if ( (busyStatus != null) && (busyStatus.equals(BusyStatus.FREE)) ) {
                IcalUtil.addProperty(icalOutput, Transp.TRANSPARENT);
            } else {
                IcalUtil.addProperty(icalOutput, Transp.OPAQUE);
            }

            addAttendees(icalOutput, mimeMsg, partstat, replyWanted);
            // TODO RESOURCES from PidLidNonSendableBcc with ';' replaced
            // with ','.  These are resources without a mail address
            IcalUtil.addProperty(icalOutput,
                    "X-MICROSOFT-CDO-ALLDAYEVENT",
                    isAllDayEvent ? "TRUE" : "FALSE");
            IcalUtil.addProperty(icalOutput,
                    "X-MICROSOFT-CDO-BUSYSTATUS", busyStatus, false);
            if (method.equals(Method.REQUEST)) {
                IcalUtil.addProperty(icalOutput,
                        "X-MICROSOFT-CDO-INTENDEDSTATUS",
                        intendedBusyStatus, false);
            }
            IcalUtil.addProperty(icalOutput, "X-MICROSOFT-CDO-OWNERAPPTID",
                        ownerApptId, false);
            IcalUtil.addProperty(icalOutput, "X-MICROSOFT-CDO-REPLYTIME",
                    schedView.getAppointmentReplyTime(), false);
            IcalUtil.addProperty(icalOutput,
                    "X-MICROSOFT-CDO-OWNER-CRITICAL-CHANGE",
                    ownerCriticalChange, false);
            Boolean disallowCounter = schedView.isDisallowCounter();
            if (disallowCounter != null) {
                IcalUtil.addProperty(icalOutput,
                        "X-MICROSOFT-CDO-DISALLOW-COUNTER",
                        disallowCounter ? "TRUE" : "FALSE");
            }
            if (reminderSet) {
                addAlarmComponent(icalOutput, schedView.getReminderDelta());
            }
            icalOutput.endComponent(Component.VEVENT);
            addExceptions(icalOutput, recurDef, recurrenceTZinfo,
                sequenceNum, ownerApptId, summary, location, isAllDayEvent);

            icalOutput.endCalendar();
            conversionSuccessful = true;
            sLog.info("Calendaring TNEF message mapped to ICALENDAR with UID=%s", uid);
        } catch (ParserException e) {
            sLog.debug( "Unexpected ParserException thrown" , e);
        } catch (URISyntaxException e) {
            sLog.debug( "Unexpected URISyntaxException thrown" , e);
        } catch (ParseException e) {
            sLog.debug( "Unexpected ParseException thrown" , e);
        } catch (MessagingException e) {
            sLog.debug( "Unexpected MessagingException thrown" , e);
        } catch (IOException e) {
            sLog.debug( "Unexpected IOException thrown" , e);
        } catch (UnsupportedTnefCalendaringMsgException e) {
            sLog.debug("Unable to map this message to ICALENDAR", e);
        } catch (TNEFtoIcalendarServiceException e) {
            sLog.debug("Problem encountered mapping this message to ICALENDAR", e);
        } finally {
            try {
                if (tnefStream != null) {
                    tnefStream.close();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return conversionSuccessful;
    }

    /**
     * 
     * @param icalOutput
     * @param recurDef
     * @param tzDef
     * @param sequenceNum
     * @param ownerApptId
     * @param seriesSummary
     * @param seriesLocation
     * @param seriesIsAllDay
     * @throws ParserException
     * @throws URISyntaxException
     * @throws IOException
     * @throws ParseException
     */
    private void addExceptions(ContentHandler icalOutput,
                RecurrenceDefinition recurDef, TimeZoneDefinition tzDef,
                int sequenceNum, Integer ownerApptId, 
                String seriesSummary, String seriesLocation,
                boolean seriesIsAllDay)
            throws ParserException, URISyntaxException, IOException, ParseException {
        if (recurDef != null) {
            for (ChangedInstanceInfo cInst : recurDef.getChangedInstances()) {
                EnumSet <ExceptionInfoOverrideFlag> overrideFlags = cInst.getOverrideFlags();
                // Note that modifications which are just a new time are represented
                // in the ICALENDAR as an EXDATE/RDATE pair
                if ( (overrideFlags == null) || (overrideFlags.isEmpty()) ) {
                    continue;
                }
                icalOutput.startComponent(Component.VEVENT);
                Boolean exceptIsAllDayEvent = cInst.isAllDayEvent();
                if (exceptIsAllDayEvent == null) {
                    exceptIsAllDayEvent = seriesIsAllDay;
                }
                String exceptSumm = cInst.getSummary();
                if (exceptSumm == null) {
                    exceptSumm = seriesSummary;
                }
                IcalUtil.addProperty(icalOutput, Property.SUMMARY, exceptSumm, false);
                String exceptLocation = cInst.getLocation();
                if (exceptLocation == null) {
                    exceptLocation = seriesLocation;
                }
                IcalUtil.addProperty(icalOutput, Property.LOCATION, exceptLocation, false);
                IcalUtil.addPropertyFromUtcTimeAndZone(icalOutput, Property.DTSTART,
                        cInst.getStartDate(), tzDef, exceptIsAllDayEvent);
                IcalUtil.addPropertyFromUtcTimeAndZone(icalOutput, Property.DTEND,
                        cInst.getEndDate(), tzDef, exceptIsAllDayEvent);
                IcalUtil.addProperty(icalOutput, Property.UID, uid);
                IcalUtil.addPropertyFromUtcTimeAndZone(icalOutput, Property.RECURRENCE_ID,
                        cInst.getOriginalStartDate(), tzDef, seriesIsAllDay);
                IcalUtil.addProperty(icalOutput, dtstamp);
                BusyStatus exceptBusyStatus = cInst.getBusyStatus();
                if (exceptBusyStatus != null) {
                    if (exceptBusyStatus.equals(BusyStatus.FREE)) {
                        IcalUtil.addProperty(icalOutput, Transp.TRANSPARENT);
                    } else {
                        IcalUtil.addProperty(icalOutput, Transp.OPAQUE);
                    }
                }
                IcalUtil.addProperty(icalOutput, Property.SEQUENCE, sequenceNum, false);
                if (method.equals(Method.REQUEST)) {
                    IcalUtil.addProperty(icalOutput,
                            "X-MICROSOFT-CDO-INTENDEDSTATUS",
                            exceptBusyStatus, false);
                }
                IcalUtil.addProperty(icalOutput, "X-MICROSOFT-CDO-OWNERAPPTID",
                            ownerApptId, false);
                IcalUtil.addProperty(icalOutput,
                        "X-MICROSOFT-CDO-ALLDAYEVENT",
                        exceptIsAllDayEvent ? "TRUE" : "FALSE");
                addAlarmComponent(icalOutput, cInst.getReminderDelta());
                icalOutput.endComponent(Component.VEVENT);
            }
        }
    }

    /**
     * 
     * @param icalOutput
     * @param mimeMsg
     * @param partstat
     * @param replyWanted
     * @throws ParserException
     * @throws URISyntaxException
     * @throws IOException
     * @throws ParseException
     * @throws MessagingException
     */
    private void addAttendees(ContentHandler icalOutput, MimeMessage mimeMsg,
            PartStat partstat, boolean replyWanted)
            throws ParserException, URISyntaxException, IOException, ParseException, MessagingException {
        // ATTENDEEs
        InternetAddress firstFromIA = null;
        String firstFromEmailAddr = null;
        String senderMailto = null;  // Use for SENT-BY if applicable
        String senderCn = null;
        
        javax.mail.Address[] toRecips = null;
        javax.mail.Address[] ccRecips = null;
        javax.mail.Address[] bccRecips = null;
        javax.mail.Address[] msgFroms = null;
        javax.mail.Address msgSender = null;
        if (mimeMsg != null) {
            toRecips = mimeMsg.getRecipients(javax.mail.Message.RecipientType.TO);
            ccRecips = mimeMsg.getRecipients(javax.mail.Message.RecipientType.CC);
            bccRecips = mimeMsg.getRecipients(javax.mail.Message.RecipientType.BCC);
            msgFroms = mimeMsg.getFrom();
            msgSender = mimeMsg.getSender();
        }
        if (msgFroms != null) {
            if (msgFroms.length != 1) {
                sLog.debug(msgFroms.length + " From: recipients for " + method.getValue());
            }
            if (msgFroms.length >= 1) {
                firstFromIA = (InternetAddress) msgFroms[0];
                firstFromEmailAddr = firstFromIA.getAddress();
            }
            if (msgSender != null) {
                String senderAddr = msgSender.toString();
                if (msgSender instanceof InternetAddress) {
                    InternetAddress senderIA = (InternetAddress) msgSender;
                    senderAddr = senderIA.getAddress();
                    senderCn = senderIA.getPersonal();
                    if (!firstFromIA.equals(senderIA)) {
                        senderMailto = "Mailto:" + senderAddr;
                    }
                }
            }
        }

        if ( method.equals(Method.REPLY) || method.equals(Method.COUNTER) ) {
            // from ATTENDEE to ORGANIZER
            if (toRecips != null) {
                if (toRecips.length != 1) {
                    sLog.debug(toRecips.length + " To: recipients for " + method.getValue());
                }
                if (toRecips.length >= 1) {
                    InternetAddress ia = (InternetAddress) toRecips[0];
                    String email = ia.getAddress();
                    String displayName = ia.getPersonal();
                    icalOutput.startProperty(Property.ORGANIZER);
                    icalOutput.propertyValue("Mailto:" + email);
                    if (displayName != null) {
                        icalOutput.parameter(Parameter.CN, displayName);
                    }
                    icalOutput.endProperty(Property.ORGANIZER);
                }
            }
            if ( firstFromEmailAddr != null) {
                String displayName = firstFromIA.getPersonal();
                icalOutput.startProperty(Property.ATTENDEE);
                icalOutput.propertyValue("Mailto:" + firstFromEmailAddr);
                if (displayName != null) {
                    icalOutput.parameter(Parameter.CN, displayName);
                }
                // TODO: possibly only output if a RESOURCE - MS-OXCICAL :
                // For attendees exported from the recipient table, this parameter SHOULD only
                // be exported if the PidTagRecipientType is 0x00000003. In this case, the
                // CUTYPE SHOULD be set to resource. For attendees exported from
                // PidLidNonSendableTo and PidLidNonSendableCc, this parameter SHOULD be omitted.
                icalOutput.parameter(Parameter.CUTYPE, CuType.INDIVIDUAL.getValue());
                if (partstat != null) {
                    icalOutput.parameter(Parameter.PARTSTAT, partstat.getValue());
                }
                if (senderMailto != null) {
                    icalOutput.parameter(Parameter.SENT_BY, senderMailto);
                }
                icalOutput.endProperty(Property.ATTENDEE);
            }
        } else {
            // ORGANIZER to ATTENDEEs - REQUEST or CANCEL
            InternetAddress organizerEmail = null;
            if (firstFromEmailAddr != null) {
                SentBy sentBy = null;
                Cn cn = null;
                if (senderMailto != null) {
                    sentBy = new SentBy(senderMailto);
                }
                organizerEmail = firstFromIA;
                String displayName = firstFromIA.getPersonal();
                if ((displayName != null)&& (!displayName.equals(firstFromEmailAddr))) {
                    cn = new Cn(displayName);
                }
                Organizer organizer = new Organizer();
                organizer.setValue("Mailto:" + firstFromEmailAddr);
                if (cn != null) {
                    organizer.getParameters().add(cn);
                }
                if (sentBy != null) {
                    organizer.getParameters().add(sentBy);
                }
                IcalUtil.addProperty(icalOutput, organizer);
                // Assumption - ORGANIZER is an attendee and is attending.
                Attendee attendee = new Attendee("Mailto:" + firstFromEmailAddr);
                if (cn != null) {
                    attendee.getParameters().add(cn);
                }
                attendee.getParameters().add(CuType.INDIVIDUAL);
                PartStat orgPartstat = PartStat.ACCEPTED;
                if (ccRecips != null) {
                    for (Address a : ccRecips) {
                        InternetAddress ia = (InternetAddress) a;
                        if ( organizerEmail.equals(ia) ) {
                            orgPartstat = PartStat.TENTATIVE;
                            break;
                        }
                    }
                }
                attendee.getParameters().add(Role.REQ_PARTICIPANT);
                attendee.getParameters().add(orgPartstat);
                // Was including SENT-BY but probably not appropriate
                // for a request
                IcalUtil.addProperty(icalOutput, attendee);
            }
            if (toRecips != null) {
                for (Address a : toRecips) {
                    InternetAddress ia = (InternetAddress) a;
                    if ( (organizerEmail != null) && organizerEmail.equals(ia) ) {
                        continue;  // No need to add the information twice
                    }
                    addAttendee(icalOutput, ia, Role.REQ_PARTICIPANT,
                                CuType.INDIVIDUAL, partstat, replyWanted);
                }
            }
            if (ccRecips != null) {
                for (Address a : ccRecips) {
                    InternetAddress ia = (InternetAddress) a;
                    if ( (organizerEmail != null) && organizerEmail.equals(ia) ) {
                        continue;  // No need to add the information twice
                    }
                    addAttendee(icalOutput, ia, Role.OPT_PARTICIPANT,
                                CuType.INDIVIDUAL, partstat, replyWanted);
                }
            }
            if (bccRecips != null) {
                for (Address a : bccRecips) {
                    InternetAddress ia = (InternetAddress) a;
                    addAttendee(icalOutput, ia, Role.NON_PARTICIPANT,
                                CuType.RESOURCE, partstat, replyWanted);
                }
            }
        }
        if (senderMailto != null) {
            XProperty msOlkSender = new XProperty("X-MS-OLK-SENDER", senderMailto);
            if (senderCn != null) {
                Cn cn = new Cn(senderCn);
                msOlkSender.getParameters().add(cn);
            }
            IcalUtil.addProperty(icalOutput, msOlkSender);
        }
    }

    /**
     * 
     * @param icalOutput
     * @param ia
     * @param role
     * @param cuType
     * @param partstat
     * @param rsvp
     * @throws URISyntaxException
     * @throws ParserException
     * @throws IOException
     * @throws ParseException
     */
private void addAttendee(ContentHandler icalOutput, InternetAddress ia,
            Role role, CuType cuType, PartStat partstat, boolean rsvp)
            throws URISyntaxException, ParserException, IOException, ParseException {

        String email = ia.getAddress();
        String displayName = ia.getPersonal();
        Attendee attendee = new Attendee("Mailto:" + email);
        if ((displayName != null)&& (!displayName.equals(email))) {
            Cn cn = new Cn(displayName);
            attendee.getParameters().add(cn);
        }
        if (rsvp) {
            attendee.getParameters().add(Rsvp.TRUE);
        }
        attendee.getParameters().add(role);
        attendee.getParameters().add(cuType);
        if (partstat != null) {
            attendee.getParameters().add(partstat);
        }
        IcalUtil.addProperty(icalOutput, attendee);
    }

    /**
     * 
     * @param icalOutput
     * @param reminderDelta number of minutes before Start
     * @throws ParserException
     * @throws URISyntaxException
     * @throws IOException
     * @throws ParseException
     */
    private void addAlarmComponent(ContentHandler icalOutput, Integer reminderDelta)
                throws ParserException, URISyntaxException, IOException, ParseException {
        if (reminderDelta == null) {
            return;
        }

        icalOutput.startComponent(Component.VALARM);
        IcalUtil.addProperty(icalOutput, Action.DISPLAY);
        IcalUtil.addProperty(icalOutput, Property.DESCRIPTION, "Reminder", false);
        String trigStr;
        if (reminderDelta % 60 == 0) {
            reminderDelta = reminderDelta / 60;
            if (reminderDelta % 24 == 0) {
                reminderDelta = reminderDelta / 24;
                trigStr = String.format("-PT%dD", reminderDelta);
            } else {
                trigStr = String.format("-PT%dH", reminderDelta);
            }
        } else {
            trigStr = String.format("-PT%dM", reminderDelta);
        }
        ParameterList trigParams = new ParameterList();
        trigParams.add(Related.START);
        Trigger trigger = new Trigger(trigParams, trigStr);
        IcalUtil.addProperty(icalOutput, trigger);
        icalOutput.endComponent(Component.VALARM);
    }

    /**
     * @return the recurDef
     */
    public RecurrenceDefinition getRecurDef() {
        return recurDef;
    }
}
