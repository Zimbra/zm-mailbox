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

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import net.fortuna.ical4j.data.ContentHandler;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.parameter.CuType;
import net.fortuna.ical4j.model.parameter.PartStat;
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
import com.zimbra.cs.util.tnef.mapi.AppointmentStateFlags;
import com.zimbra.cs.util.tnef.mapi.BusyStatus;
import com.zimbra.cs.util.tnef.mapi.RecurrenceDefinition;
import com.zimbra.cs.util.tnef.mapi.TZRule;
import com.zimbra.cs.util.tnef.mapi.TimeZoneDefinition;
import java.util.EnumSet;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.SentBy;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Organizer;
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
    private boolean debugEnabled;

    public DefaultTnefToICalendar() {
        debugEnabled = false;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.util.tnef.TnefToICalendar#convert(java.io.InputStream, net.fortuna.ical4j.data.ContentHandler)
     */
    public boolean convert(MimeMessage mimeMsg, InputStream tnefInput, ContentHandler icalOutput)
            throws ServiceException {

        boolean conversionSuccessful = false;
        TNEFInputStream tnefStream = null;
        SchedulingViewOfTnef schedView = null;

        try {
            tnefStream = new TNEFInputStream(tnefInput);
	        schedView = new SchedulingViewOfTnef(tnefStream);
            if (debugEnabled) {
                System.out.println(schedView.toXmlStringBuffer());
            }

            // String oemCodePage = schedView.getOEMCodePage();
            String msgClass = schedView.getMessageClass();
            String uid = schedView.getIcalUID();
            Integer sequenceNum = schedView.getSequenceNumber();
            Boolean replyWanted = schedView.getResponseRequested();
            String location = schedView.getLocation();
            Boolean isAllDayEvent = schedView.isAllDayEvent();
            Boolean isCounterProposal = schedView.isCounterProposal();
            Integer importance = schedView.getMapiImportance();
            Clazz  icalClass = schedView.getIcalClass();
            BusyStatus busyStatus = schedView.getBusyStatus();
            BusyStatus intendedBusyStatus = schedView.getIntendedBusyStatus();
            // TODO: MS-OXCICAL says attendee properties SHOULD NOT be exported if asfMeeting not set.
            EnumSet <AppointmentStateFlags> appointmentStateFlags = schedView.getAppointmentStateFlags();
            // MS-OXCICAL says attendee properties SHOULD NOT be exported if asfMeeting not set.
            // However - if you do this, an acceptance seems to end up with
            // no ATTENDEE, which seems wrong.
            // boolean addAttendees = (appointmentStateFlags == null) ||
            //              appointmentStateFlags.contains(AppointmentStateFlags.ASF_MEETING);
            boolean addAttendees = true;

            TimeZoneDefinition startTimeTZinfo = schedView.getStartDateTimezoneInfo();
            TimeZoneDefinition endTimeTZinfo = schedView.getEndDateTimezoneInfo();
            TimeZoneDefinition recurrenceTZinfo = schedView.getRecurrenceTimezoneInfo();

            String tzDesc = schedView.getTimeZoneDescription();
            if (null != tzDesc) {
                TimeZoneDefinition tzStructInfo =
                        schedView.getTimeZoneStructInfo(tzDesc);
                if (null != tzStructInfo) {
                    // if the rules differ, tzStructInfo should win
                    if (recurrenceTZinfo != null) {
                        TZRule tzsRule = tzStructInfo.getEffectiveRule();
                        if (tzsRule != null) {
                            if (!tzsRule.equivalentRule(
                                    recurrenceTZinfo.getEffectiveRule())) {
                                sLog.debug(
    "PidLidAppointmentTimeZoneDefinitionRecur effective rule differs from PidLidTimeZoneStruct rule - ignored");
                                recurrenceTZinfo = tzStructInfo;
                            }
                        }
                    }
                    if (startTimeTZinfo == null) {
                        startTimeTZinfo = tzStructInfo;
                    }
                }
            }

            if (recurrenceTZinfo == null) {
                recurrenceTZinfo = startTimeTZinfo;
            }
            if (endTimeTZinfo == null) {
                endTimeTZinfo = startTimeTZinfo;
            }

            RecurrenceDefinition recurDef = schedView.getRecurrenceDefinition(recurrenceTZinfo);

            DateTime icalStartDate = schedView.getStartTime();
            DateTime icalEndDate = schedView.getEndTime();
            DateTime icalCreateDate = schedView.getUtcDateTime(null, MAPIProp.PR_CREATION_TIME);
            DateTime icalLastModDate = schedView.getUtcDateTime(null, MAPIProp.PR_LAST_MODIFICATION_TIME);
            // TODO: Only do these gets if is a COUNTER proposal
            DateTime proposedStartDate = schedView.getProposedStartTime();
            DateTime proposedEndDate = schedView.getProposedEndTime();
            DateTime recurrenceIdDateTime = schedView.getRecurrenceIdTime();
            DateTime attendeeCriticalChange = schedView.getAttendeeCriticalChange();
            DateTime ownerCriticalChange = schedView.getOwnerCriticalChange();
            String[] categories = schedView.getCategories();
            Method method = null;
            PartStat partstat = null;
            String descriptionText = null;
            String summary = null;
            javax.mail.Address[] toRecips = null;
            javax.mail.Address[] ccRecips = null;
            javax.mail.Address[] bccRecips = null;
            javax.mail.Address[] msgFroms = null;
            javax.mail.Address msgSender = null;
            if (mimeMsg != null) {
                summary = mimeMsg.getSubject();
                toRecips = mimeMsg.getRecipients(javax.mail.Message.RecipientType.TO);
                ccRecips = mimeMsg.getRecipients(javax.mail.Message.RecipientType.CC);
                bccRecips = mimeMsg.getRecipients(javax.mail.Message.RecipientType.BCC);
                msgFroms = mimeMsg.getFrom();
                msgSender = mimeMsg.getSender();
                PlainTextFinder finder = new PlainTextFinder();
                finder.accept(mimeMsg);
                descriptionText = finder.getPlainText();
            }
            if (msgClass == null) {
                sLog.debug("Unable to determine Class of TNEF - cannot generate ICALENDER equivalent");
                // throw TNEFtoIcalendarServiceException.NON_CALENDARING_CLASS(msgClass);
                return false;
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
                IcalUtil.addProperty(icalOutput, Property.UID, "No-orignal-valid-UID");
            } else {
                IcalUtil.addProperty(icalOutput, Property.UID, uid);
            }
            if ( (attendeeCriticalChange != null) &&
                    ( method.equals(Method.REPLY) ||
                      method.equals(Method.COUNTER) ) ) {
                IcalUtil.addProperty(icalOutput, Property.DTSTAMP, attendeeCriticalChange, false);
            } else if (ownerCriticalChange != null) {
                IcalUtil.addProperty(icalOutput, Property.DTSTAMP, ownerCriticalChange, false);
            } else {
                IcalUtil.addProperty(icalOutput, Property.DTSTAMP, "20000101T000000Z");
            }
            IcalUtil.addProperty(icalOutput, Property.CREATED, icalCreateDate, false);
            IcalUtil.addProperty(icalOutput, Property.LAST_MODIFIED, icalLastModDate, false);
            IcalUtil.addProperty(icalOutput, Property.SEQUENCE, sequenceNum, false);
            IcalUtil.addProperty(icalOutput, Property.SUMMARY, summary, false);
            IcalUtil.addProperty(icalOutput, Property.LOCATION, location, false);
            IcalUtil.addProperty(icalOutput, Property.DESCRIPTION, descriptionText, false);
            if ( method.equals(Method.COUNTER) ) {
                IcalUtil.addPropertyFromUtcTimeAndZone(icalOutput, Property.DTSTART,
                        proposedStartDate, startTimeTZinfo, isAllDayEvent);
                IcalUtil.addPropertyFromUtcTimeAndZone(icalOutput, Property.DTEND,
                        proposedEndDate, endTimeTZinfo, isAllDayEvent);
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
            IcalUtil.addPropertyFromUtcTimeAndZone(icalOutput, Property.RECURRENCE_ID,
                    recurrenceIdDateTime, startTimeTZinfo, isAllDayEvent);
            if (recurDef != null) {
                Property recurrenceProp =
                    recurDef.icalRecurrenceProperty(isAllDayEvent, false);
                IcalUtil.addProperty(icalOutput, recurrenceProp);
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
            }

            // ATTENDEEs
            InternetAddress firstFromIA = null;
            String firstFromEmailAddr = null;
            String senderMailto = null;  // Use for SENT-BY if applicable
            String senderCn = null;
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
                if ( addAttendees && (firstFromEmailAddr != null)) {
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
                    if (addAttendees) {
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
                }
                if (addAttendees && (toRecips != null)) {
                    for (Address a : toRecips) {
                        InternetAddress ia = (InternetAddress) a;
                        if ( (organizerEmail != null) && organizerEmail.equals(ia) ) {
                            continue;  // No need to add the information twice
                        }
                        addAttendee(icalOutput, ia, Role.REQ_PARTICIPANT,
                                    CuType.INDIVIDUAL, partstat,
                                    ((replyWanted != null) && (replyWanted)));
                    }
                }
                if (addAttendees && (ccRecips != null)) {
                    for (Address a : ccRecips) {
                        InternetAddress ia = (InternetAddress) a;
                        if ( (organizerEmail != null) && organizerEmail.equals(ia) ) {
                            continue;  // No need to add the information twice
                        }
                        addAttendee(icalOutput, ia, Role.OPT_PARTICIPANT,
                                    CuType.INDIVIDUAL, partstat,
                                    ((replyWanted != null) && (replyWanted)));
                    }
                }
                if (addAttendees && (bccRecips != null)) {
                    for (Address a : bccRecips) {
                        InternetAddress ia = (InternetAddress) a;
                        addAttendee(icalOutput, ia, Role.NON_PARTICIPANT,
                                    CuType.RESOURCE, partstat,
                                    ((replyWanted != null) && (replyWanted)));
                    }
                }
            }
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
                    schedView.getOwnerAppointmentId(), false);
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
            if (addAttendees && (senderMailto != null)) {
                XProperty msOlkSender = new XProperty("X-MS-OLK-SENDER", senderMailto);
                if (senderCn != null) {
                    Cn cn = new Cn(senderCn);
                    msOlkSender.getParameters().add(cn);
                }
                IcalUtil.addProperty(icalOutput, msOlkSender);
            }
            icalOutput.endComponent(Component.VEVENT);
            // TODO:  Want VALARM too "for completeness"
            icalOutput.endCalendar();
            conversionSuccessful = true;
        } catch (ParserException e) {
            sLog.error( "Unexpected ParserException thrown" , e);
        } catch (URISyntaxException e) {
            sLog.error( "Unexpected URISyntaxException thrown" , e);
        } catch (ParseException e) {
            sLog.error( "Unexpected ParseException thrown" , e);
        } catch (MessagingException e) {
            sLog.error( "Unexpected MessagingException thrown" , e);
        } catch (IOException e) {
            sLog.error( "Unexpected IOException thrown" , e);
        } catch (UnsupportedTnefCalendaringMsgException e) {
            // TODO: debug level is probably more appropriate
            sLog.warn("Unable to map this message to ICALENDAR", e);
        } catch (TNEFtoIcalendarServiceException e) {
            sLog.warn("Problem encountered mapping this message to ICALENDAR", e);
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

	public void setDebugEnabled(boolean dbgEnabled) {
		debugEnabled = dbgEnabled;
	}

	public boolean isDebugEnabled() {
		return debugEnabled;
	}

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
}
