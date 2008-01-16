/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox.calendar;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZComponent;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZParameter;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZProperty;
import com.zimbra.common.soap.Element;

/**
 * iCalendar VALARM component
 */
public class Alarm {

    public static enum Action {
        DISPLAY, AUDIO, EMAIL, PROCEDURE,
        // Yahoo calendar reminder custom actions
        X_YAHOO_CALENDAR_ACTION_IM, X_YAHOO_CALENDAR_ACTION_MOBILE;

        public static Action lookup(String str) {
            if (str != null) {
                try {
                    str = str.replace('-', '_').toUpperCase();
                    return Action.valueOf(str);
                } catch (IllegalArgumentException e) {}
            }
            return null;
        }
    };
    public static enum TriggerType {
        RELATIVE, ABSOLUTE;

        public static TriggerType lookup(String str) {
            if (str != null) {
                try {
                    str = str.replace('-', '_').toUpperCase();
                    return TriggerType.valueOf(str);
                } catch (IllegalArgumentException e) {}
            }
            return null;
        }
    };

    public static enum TriggerRelated {
        START, END;
    
        public static TriggerRelated lookup(String str) {
            if (str != null) {
                try {
                    str = str.replace('-', '_').toUpperCase();
                    return TriggerRelated.valueOf(str);
                } catch (IllegalArgumentException e) {}
            }
            return null;
        }
    };

    // ACTION
    private Action mAction;

    // TRIGGER
    private TriggerType mTriggerType;
    private TriggerRelated mTriggerRelated;  // default is START
    private ParsedDuration mTriggerRelative;
    private ParsedDateTime mTriggerAbsolute;

    // REPEAT
    private ParsedDuration mRepeatDuration;
    private int mRepeatCount;

    // DESCRIPTION
    private String mDescription;

    // SUMMARY (email subject when mAction=EMAIL)
    private String mSummary;

    // ATTACH
    private Attach mAttach;

    // ATTENDEEs
    private List<ZAttendee> mAttendees;


    public String getDescription() { return mDescription; }
    public int getRepeatCount() { return mRepeatCount; }
    public Action getAction() { return mAction; }
    public List<ZAttendee> getAttendees() { return mAttendees; }
    
    public long getRepeatMillis() {
        if (mRepeatDuration == null)
            return mRepeatDuration.getDurationAsMsecs(new Date());
        else
            return 0;
    }

    public Alarm(Action action,
                  TriggerType triggerType, TriggerRelated related,
                  ParsedDuration triggerRelative, ParsedDateTime triggerAbsolute,
                  ParsedDuration repeatDuration, int repeatCount,
                  String description, String summary,
                  Attach attach,
                  List<ZAttendee> attendees)
    throws ServiceException {
        if (action == null)
            throw ServiceException.INVALID_REQUEST("Missing ACTION in VALARM", null);
        mAction = action;
        mTriggerType = triggerType;
        if (TriggerType.ABSOLUTE.equals(triggerType)) {
            if (triggerAbsolute == null)
                throw ServiceException.INVALID_REQUEST("Missing absolute TRIGGER in VALARM", null);
            mTriggerAbsolute = triggerAbsolute;
        } else {
            if (triggerRelative == null)
                throw ServiceException.INVALID_REQUEST("Missing relative TRIGGER in VALARM", null);
            mTriggerRelated = related;
            mTriggerRelative = triggerRelative;
        }
        if (repeatDuration != null) {
            mRepeatDuration = repeatDuration;
            mRepeatCount = repeatCount;
        }
        mDescription = description;
        mSummary = summary;
        mAttach = attach;
        mAttendees = attendees;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("action=").append(mAction.toString());
        sb.append(", triggerType=").append(mTriggerType.toString());
        if (TriggerType.ABSOLUTE.equals(mTriggerType)) {
            sb.append(", triggerAbsolute=").append(
                    mTriggerAbsolute != null ? mTriggerAbsolute.toString() : "<none>");
        } else {
            sb.append(", triggerRelated").append(
                    mTriggerRelated != null ? mTriggerRelated.toString() : "<default>");
            sb.append(", triggerRelative=").append(
                    mTriggerRelative != null ? mTriggerRelative.toString() : "<none>");
        }
        if (mRepeatDuration != null) {
            sb.append(", repeatDuration=").append(
                    mRepeatDuration != null ? mRepeatDuration.toString() : "<none>");
            sb.append(", repeatCount=").append(mRepeatCount);
        } else {
            sb.append(", repeat=<none>");
        }
        sb.append(", summary=\"").append(mSummary).append("\"");
        sb.append(", desc=\"").append(mDescription).append("\"");
        if (mAttach != null)
            sb.append(", attach=").append(mAttach.toString());
        if (mAttendees != null) {
            sb.append(", attendees=[");
            boolean first = true;
            for (ZAttendee attendee : mAttendees) {
                if (!first)
                    sb.append(", ");
                else
                    first = false;
                sb.append("[").append(attendee.toString()).append("]");
            }
            sb.append("]");
        }
        return sb.toString();
    }

    public Element toXml(Element parent) {
        Element alarm = parent.addElement(MailConstants.E_CAL_ALARM);
        alarm.addAttribute(MailConstants.A_CAL_ALARM_ACTION, mAction.toString());
        Element trigger = alarm.addElement(MailConstants.E_CAL_ALARM_TRIGGER);
        if (TriggerType.ABSOLUTE.equals(mTriggerType)) {
            Element absolute = trigger.addElement(MailConstants.E_CAL_ALARM_ABSOLUTE);
            absolute.addAttribute(MailConstants.A_DATE, mTriggerAbsolute.getDateTimePartString(false));
        } else {
            Element relative = mTriggerRelative.toXml(trigger, MailConstants.E_CAL_ALARM_RELATIVE);
            if (mTriggerRelated != null)
                relative.addAttribute(MailConstants.A_CAL_ALARM_RELATED, mTriggerRelated.toString());
        }
        if (mRepeatDuration != null) {
            Element repeat = mRepeatDuration.toXml(alarm, MailConstants.E_CAL_ALARM_REPEAT);
            repeat.addAttribute(MailConstants.A_CAL_ALARM_COUNT, mRepeatCount);
        }
        if (!Action.AUDIO.equals(mAction)) {
            Element desc = alarm.addElement(MailConstants.E_CAL_ALARM_DESCRIPTION);
            if (mDescription != null)
                desc.setText(mDescription);
        }
        if (mAttach != null)
            mAttach.toXml(alarm);
        if (Action.EMAIL.equals(mAction) ||
            Action.X_YAHOO_ACTION_IM.equals(mAction) ||
            Action.X_YAHOO_ACTION_MOBILE.equals(mAction)) {
            Element summary = alarm.addElement(MailConstants.E_CAL_ALARM_SUMMARY);
            if (mSummary != null)
                summary.setText(mSummary);
            if (mAttendees != null) {
                for (ZAttendee attendee : mAttendees) {
                    attendee.toXml(alarm);
                }
            }
        }
        return alarm;
    }

    public static boolean actionAllowed(Action action) {
        if (!DebugConfig.calendarAllowNonDisplayAlarms) {
            if (Action.DISPLAY.equals(action))
                return true;
            ZimbraLog.calendar.warn(
                    "Action " + (action != null ? action.toString() : "null") +
                    " is not allowed; ignoring alarm");
            return false;
        } else
            return true;
    }

    /**
     * Create an Alarm from SOAP.  Return value may be null.
     * @param alarmElem
     * @return
     * @throws ServiceException
     */
    public static Alarm parse(Element alarmElem) throws ServiceException {
        Action action = Action.DISPLAY;
        TriggerType triggerType = TriggerType.RELATIVE;
        TriggerRelated triggerRelated = null;
        ParsedDuration triggerRelative = null;
        ParsedDateTime triggerAbsolute = null;
        ParsedDuration repeatDuration = null;
        int repeatCount = 0;
        String description = null;
        String summary = null;
        Attach attach = null;
        List<ZAttendee> attendees = null;

        String val;
        val = alarmElem.getAttribute(MailConstants.A_CAL_ALARM_ACTION);
        action = Action.lookup(val);
        if (action == null)
            throw ServiceException.INVALID_REQUEST(
                    "Invalid " + MailConstants.A_CAL_ALARM_ACTION + " value " + val, null);
        if (!actionAllowed(action))
            return null;

        Element triggerElem = alarmElem.getElement(MailConstants.E_CAL_ALARM_TRIGGER);
        Element triggerRelativeElem = triggerElem.getOptionalElement(MailConstants.E_CAL_ALARM_RELATIVE);
        if (triggerRelativeElem != null) {
            triggerType = TriggerType.RELATIVE;
            String related = triggerRelativeElem.getAttribute(MailConstants.A_CAL_ALARM_RELATED, null);
            if (related != null) {
                triggerRelated = TriggerRelated.lookup(related);
                if (triggerRelated == null)
                    throw ServiceException.INVALID_REQUEST(
                            "Invalid " + MailConstants.A_CAL_ALARM_RELATED + " value " + val, null);
            }
            triggerRelative = ParsedDuration.parse(triggerRelativeElem);
        } else {
            triggerType = TriggerType.ABSOLUTE;
            Element triggerAbsoluteElem = triggerElem.getOptionalElement(MailConstants.E_CAL_ALARM_ABSOLUTE);
            if (triggerAbsoluteElem == null)
                throw ServiceException.INVALID_REQUEST(
                        "<" + MailConstants.E_CAL_ALARM_TRIGGER + "> must have either <" +
                        MailConstants.E_CAL_ALARM_RELATIVE + "> or <" +
                        MailConstants.E_CAL_ALARM_ABSOLUTE + "> child element", null);
            String datetime = triggerAbsoluteElem.getAttribute(MailConstants.A_DATE);
            try {
                triggerAbsolute = ParsedDateTime.parseUtcOnly(datetime);
            } catch (ParseException e) {
                throw ServiceException.INVALID_REQUEST("Invalid absolute trigger value " + val, e);
            }
        }

        Element repeatElem = alarmElem.getOptionalElement(MailConstants.E_CAL_ALARM_REPEAT);
        if (repeatElem != null) {
            repeatDuration = ParsedDuration.parse(repeatElem);
            repeatCount = (int) repeatElem.getAttributeLong(MailConstants.A_CAL_ALARM_COUNT, 0);
        }

        Element descElem = alarmElem.getOptionalElement(MailConstants.E_CAL_ALARM_DESCRIPTION);
        if (descElem != null) {
            description = descElem.getTextTrim();
        }

        Element summaryElem = alarmElem.getOptionalElement(MailConstants.E_CAL_ALARM_SUMMARY);
        if (summaryElem != null) {
            summary = summaryElem.getTextTrim();
        }

        Element attachElem = alarmElem.getOptionalElement(MailConstants.E_CAL_ATTACH);
        if (attachElem != null)
            attach = Attach.parse(attachElem);

        Iterator<Element> attendeesIter = alarmElem.elementIterator(MailConstants.E_CAL_ATTENDEE);
        while (attendeesIter.hasNext()) {
            ZAttendee at = ZAttendee.parse(attendeesIter.next());
            if (attendees == null)
                attendees = new ArrayList<ZAttendee>();
            attendees.add(at);
        }

        Alarm alarm = new Alarm(
                action, triggerType, triggerRelated, triggerRelative, triggerAbsolute,
                repeatDuration, repeatCount, description, summary, attach, attendees);
        return alarm;
    }

    public ZComponent toZComponent() throws ServiceException {
        ZComponent comp = new ZComponent(ICalTok.VALARM);

        ZProperty action = new ZProperty(ICalTok.ACTION, mAction.toString());
        comp.addProperty(action);

        ZProperty trigger = new ZProperty(ICalTok.TRIGGER);
        if (TriggerType.ABSOLUTE.equals(mTriggerType)) {
            ZParameter vt = new ZParameter(ICalTok.VALUE, ICalTok.DATE_TIME.toString());
            trigger.addParameter(vt);
            trigger.setValue(mTriggerAbsolute.getDateTimePartString(false));
        } else {
            if (mTriggerRelated != null) {
                ZParameter related = new ZParameter(ICalTok.RELATED, mTriggerRelated.toString());
                trigger.addParameter(related);
            }
            trigger.setValue(mTriggerRelative.toString());
        }
        comp.addProperty(trigger);

        if (mRepeatDuration != null) {
            ZProperty duration = new ZProperty(ICalTok.DURATION, mRepeatDuration.toString());
            comp.addProperty(duration);
            ZProperty repeat = new ZProperty(ICalTok.REPEAT, mRepeatCount);
            comp.addProperty(repeat);
        }

        if (!Action.AUDIO.equals(mAction)) {
            String d = mDescription;
            // DESCRIPTION is required in DISPLAY and EMAIL alarms.
            if (d == null && !Action.PROCEDURE.equals(mAction))
                d = "Reminder";
            ZProperty desc = new ZProperty(ICalTok.DESCRIPTION, d);
            comp.addProperty(desc);
        }

        if (mAttach != null)
            comp.addProperty(mAttach.toZProperty());

        if (Action.EMAIL.equals(mAction) ||
                Action.X_YAHOO_ACTION_IM.equals(mAction) ||
                Action.X_YAHOO_ACTION_MOBILE.equals(mAction)) {
            String s = mSummary;
            if (s == null)
                s = "Reminder";
            ZProperty summary = new ZProperty(ICalTok.SUMMARY, s);
            comp.addProperty(summary);
            // At least one ATTENDEE is required, but let's not throw any error
            // if somehow the object didn't have any attendee.
            if (mAttendees != null) {
                for (ZAttendee attendee : mAttendees) {
                    comp.addProperty(attendee.toProperty());
                }
            }
        }

        return comp;
    }

    /**
     * Create an Alarm from ZComponent.  Return value may be null.
     * @param comp
     * @return
     * @throws ServiceException
     */
    public static Alarm parse(ZComponent comp) throws ServiceException {
        Action action = Action.DISPLAY;
        TriggerType triggerType = TriggerType.RELATIVE;
        TriggerRelated triggerRelated = null;
        ParsedDuration triggerRelative = null;
        ParsedDateTime triggerAbsolute = null;
        ParsedDuration repeatDuration = null;
        int repeatCount = 0;
        String description = null;
        String summary = null;
        Attach attach = null;
        List<ZAttendee> attendees = null;

        Iterator<ZProperty> propIter = comp.getPropertyIterator();
        while (propIter.hasNext()) {
            ZProperty prop = propIter.next();
            ICalTok tok = prop.getToken();
            String val = prop.getValue();
            if (tok == null)
            	continue;
            switch (tok) {
            case ACTION:
                if (val != null) {
                    action = Action.lookup(val);
                    if (action == null)
                        throw ServiceException.INVALID_REQUEST("Invalid ACTION value " + val, null);
                    if (!actionAllowed(action))
                        return null;
                }
                break;
            case TRIGGER:
                ZParameter valueType = prop.getParameter(ICalTok.VALUE);
                if (valueType != null) {
                    String vt = valueType.getValue();
                    if (ICalTok.DATE_TIME.toString().equals(vt))
                        triggerType = TriggerType.ABSOLUTE;
                }
                if (TriggerType.RELATIVE.equals(triggerType)) {
                    ZParameter related = prop.getParameter(ICalTok.RELATED);
                    if (related != null) {
                        String rel = related.getValue();
                        if (rel != null) {
                            triggerRelated = TriggerRelated.lookup(rel);
                            if (triggerRelated == null)
                                throw ServiceException.INVALID_REQUEST("Invalid RELATED value " + rel, null);
                        }
                    }
                    triggerRelative = ParsedDuration.parse(val);
                } else {
                    try {
                        if (val != null)
                            triggerAbsolute = ParsedDateTime.parseUtcOnly(val);
                    } catch (ParseException e) {
                        throw ServiceException.INVALID_REQUEST("Invalid TRIGGER value " + val, e);
                    }
                }
                break;
            case DURATION:
                if (val != null)
                    repeatDuration = ParsedDuration.parse(val);
                break;
            case REPEAT:
                if (val != null) {
                    try {
                        repeatCount = Integer.parseInt(val);
                    } catch (NumberFormatException e) {
                        throw ServiceException.INVALID_REQUEST("Invalid REPEAT value " + val, e);
                    }
                }
                break;
            case DESCRIPTION:
                description = val;
                break;
            case SUMMARY:
                summary = val;
                break;
            case ATTACH:
                attach = Attach.parse(prop);
                break;
            case ATTENDEE:
                ZAttendee attendee = new ZAttendee(prop);
                if (attendees == null)
                    attendees = new ArrayList<ZAttendee>();
                attendees.add(attendee);
                break;
            }
        }

        Alarm alarm = new Alarm(
                action, triggerType, triggerRelated, triggerRelative, triggerAbsolute,
                repeatDuration, repeatCount, description, summary, attach, attendees);
        return alarm;
    }
    
    /**
     * Create an alarm object from a simple warning relative time.
     * 
     * @param reminder number of minutes before start of meeting
     * @return the Alarm object
     */
    public static Alarm fromSimpleReminder(int minBeforeStart) throws ServiceException {
    	return new Alarm(Action.DISPLAY, TriggerType.RELATIVE, TriggerRelated.START,
    			ParsedDuration.parse(true, 0, 0, 0, minBeforeStart, 0), null, null, 0, null, null, null, null);
    }

    private static final String FN_ACTION = "ac";
    private static final String FN_TRIGGER_TYPE = "tt";
    private static final String FN_TRIGGER_RELATED = "trd";
    private static final String FN_TRIGGER_RELATIVE = "tr";
    private static final String FN_TRIGGER_ABSOLUTE = "ta";
    private static final String FN_REPEAT_DURATION = "rd";
    private static final String FN_REPEAT_COUNT = "rc";
    private static final String FN_DESCRIPTION = "ds";
    private static final String FN_SUMMARY = "su";
    private static final String FN_NUM_ATTENDEES = "numAt";
    private static final String FN_ATTENDEE = "at";
    private static final String FN_ATTACH = "attach";

    private static String abbrevAction(Action action) {
        String str;
        switch (action) {
        case DISPLAY: str = "d"; break;
        case AUDIO: str = "a"; break;
        case EMAIL: str = "e"; break;
        case PROCEDURE: str = "p"; break;
        default: str = action.toString();
        }
        return str;
    }

    private static Action expandAction(String abbrev) {
        if (abbrev == null || abbrev.length() == 0)
            return Action.DISPLAY;
        Action action;
        char ch = abbrev.charAt(0);
        switch (ch) {
        case 'd': action = Action.DISPLAY; break;
        case 'a': action = Action.AUDIO; break;
        case 'e': action = Action.EMAIL; break;
        case 'p': action = Action.PROCEDURE; break;
        default:
            action = Action.lookup(abbrev);
            if (action == null)
                action = Action.DISPLAY;
        }
        return action;
    }

    private static String abbrevTriggerType(TriggerType tt) {
        if (tt == null || TriggerType.RELATIVE.equals(tt))
            return "r";
        else
            return "a";
    }

    private static TriggerType expandTriggerType(String abbrev) {
        if (abbrev == null || abbrev.length() == 0)
            return TriggerType.RELATIVE;
        char ch = abbrev.charAt(0);
        if (ch == 'a')
            return TriggerType.ABSOLUTE;
        else
            return TriggerType.RELATIVE;
    }

    private static String abbrevTriggerRelated(TriggerRelated tr) {
        if (tr == null)
            return null;
        else if (TriggerRelated.END.equals(tr))
            return "e";
        else
            return "s";
    }

    private static TriggerRelated expandTriggerRelated(String abbrev) {
        if (abbrev == null || abbrev.length() == 0)
            return null;
        char ch = abbrev.charAt(0);
        if (ch == 'e')
            return TriggerRelated.END;
        else
            return TriggerRelated.START;
    }

    public Metadata encodeMetadata() {
        Metadata meta = new Metadata();

        meta.put(FN_ACTION, abbrevAction(mAction));
        meta.put(FN_TRIGGER_TYPE, abbrevTriggerType(mTriggerType));
        if (TriggerType.RELATIVE.equals(mTriggerType)) {
            meta.put(FN_TRIGGER_RELATED, abbrevTriggerRelated(mTriggerRelated));
            meta.put(FN_TRIGGER_RELATIVE, mTriggerRelative.toString());
        } else {
            meta.put(FN_TRIGGER_ABSOLUTE, mTriggerAbsolute.getDateTimePartString(false));
        }
        if (mRepeatDuration != null) {
            meta.put(FN_REPEAT_DURATION, mRepeatDuration.toString());
            meta.put(FN_REPEAT_COUNT, mRepeatCount);
        }
        meta.put(FN_DESCRIPTION, mDescription);
        meta.put(FN_SUMMARY, mSummary);
        if (mAttach != null)
            meta.put(FN_ATTACH, mAttach.encodeMetadata());
        if (mAttendees != null) {
            meta.put(FN_NUM_ATTENDEES, mAttendees.size());
            int i = 0;
            for (Iterator<ZAttendee> iter = mAttendees.iterator(); iter.hasNext(); i++) {
                ZAttendee at = iter.next();
                meta.put(FN_ATTENDEE + i, at.encodeAsMetadata());
            }
        }

        return meta;
    }

    /**
     * Create an Alarm from Metadata.  Return value may be null.
     * @param meta
     * @return
     * @throws ServiceException
     * @throws ParseException
     */
    public static Alarm decodeMetadata(Metadata meta) throws ServiceException {
        Action action = expandAction(meta.get(FN_ACTION));
        if (!actionAllowed(action))
            return null;

        TriggerType tt = expandTriggerType(meta.get(FN_TRIGGER_TYPE));
        TriggerRelated triggerRelated = null;
        ParsedDuration triggerRelative = null;
        ParsedDateTime triggerAbsolute = null;
        if (TriggerType.ABSOLUTE.equals(tt)) {
            try {
                triggerAbsolute = ParsedDateTime.parseUtcOnly(meta.get(FN_TRIGGER_ABSOLUTE));
            } catch (ParseException e) {
                throw ServiceException.FAILURE("Error parsing metadata for alarm", e);
            }
        } else {
            triggerRelative = ParsedDuration.parse(meta.get(FN_TRIGGER_RELATIVE));
            triggerRelated = expandTriggerRelated(meta.get(FN_TRIGGER_RELATED, null));
        }
        ParsedDuration repeatDuration = null;
        int repeatCount = 0;
        String val = meta.get(FN_REPEAT_DURATION, null);
        if (val != null) {
            repeatDuration = ParsedDuration.parse(val);
            repeatCount = (int) meta.getLong(FN_REPEAT_COUNT, 0);
        }
        String description = meta.get(FN_DESCRIPTION, null);
        String summary = meta.get(FN_SUMMARY, null);

        Attach attach = null;
        Metadata metaAttach = meta.getMap(FN_ATTACH, true);
        if (metaAttach != null)
            attach = Attach.decodeMetadata(metaAttach);

        int numAts = (int) meta.getLong(FN_NUM_ATTENDEES, 0);
        List<ZAttendee> attendees = new ArrayList<ZAttendee>(numAts);
        for (int i = 0; i < numAts; i++) {
            try {
                Metadata metaAttendee = meta.getMap(FN_ATTENDEE + i, true);
                if (metaAttendee != null)
                    attendees.add(new ZAttendee(metaAttendee));
            } catch (ServiceException e) {
                ZimbraLog.calendar.warn("Problem decoding attendee " + i + " in ALARM "); 
            }
        }

        Alarm alarm = new Alarm(
                action, tt, triggerRelated, triggerRelative, triggerAbsolute,
                repeatDuration, repeatCount, description, summary, attach, attendees);
        return alarm;
    }

    /**
     * Returns the alarm trigger time in millis.  Only display alarm will return a meaningful
     * trigger time.  Other alarm types will return Long.MAX_VALUE.
     * Both start and end times of the appointment/task instance is required because the alarm
     * may be specified relative to either start or end time.
     * @param instStart start time of the appointment/task instance
     * @param instEnd end time of the appointment/task instance
     * @return
     */
    public long getTriggerTime(long instStart, long instEnd) {
//        if (!Action.DISPLAY.equals(mAction)) return Long.MAX_VALUE;
        if (TriggerType.ABSOLUTE.equals(mTriggerType)) {
            assert(mTriggerAbsolute != null);
            return mTriggerAbsolute.getUtcTime();
        }
        if (TriggerRelated.END.equals(mTriggerRelated))
            return mTriggerRelative.addToTime(instEnd);
        else
            return mTriggerRelative.addToTime(instStart);
    }
    
    public ParsedDuration getTriggerRelative() {
    	return mTriggerRelative;
    }
    
    public ParsedDateTime getTriggerAbsolute() {
    	return mTriggerAbsolute;
    }
}
