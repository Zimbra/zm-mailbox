package com.zimbra.cs.zclient;

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.calendar.ParsedDuration;
import com.zimbra.cs.mailbox.calendar.ParsedDateTime;
import com.zimbra.cs.mailbox.calendar.Attach;
import com.zimbra.cs.zclient.ZInvite.ZAttendee;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.text.ParseException;

/**
 * Created by IntelliJ IDEA.
 * User: akanjila
 * Date: Feb 6, 2008
 * Time: 12:50:55 PM
  */
public class ZAlarm {

    public enum ZAction {

        DISPLAY("DISPLAY"),
        AUDIO("AUDIO"),
        EMAIL("EMAIL"),
        X_YAHOO_CALENDAR_ACTION_IM("X-YAHOO-CALENDAR-ACTION-IM"),
        X_YAHOO_CALENDAR_ACTION_MOBILE("X-YAHOO-CALENDAR-ACTION-MOBILE"),
        PROCEDURE("PROCEDURE");

        private String mValue;

        ZAction(String value){
            this.mValue = value;
        }

        public String toString(){
            return this.mValue;
        }

        public static ZAction lookup(String str) {
            if (str != null) {
                try {
                    str = str.replace('-', '_').toUpperCase();
                    return ZAction.valueOf(str);
                } catch (IllegalArgumentException e) {}
            }
            return null;
        }
    }

    public enum ZRelated {
        START, END;
        public static ZRelated lookup(String str) {
            if (str != null) {
                try {
                    str = str.replace('-', '_').toUpperCase();
                    return ZRelated.valueOf(str);
                } catch (IllegalArgumentException e) {}
            }
            return null;
        }

    }
    
    public enum ZTriggerType {
        ABSOLUTE, RELATIVE;
        public static ZTriggerType lookup(String str) {
            if (str != null) {
                try {
                    str = str.replace('-', '_').toUpperCase();
                    return ZTriggerType.valueOf(str);
                } catch (IllegalArgumentException e) {}
            }
            return null;
        }

    }

    private ZAction mAction;
    private ZTriggerType mTriggerType = ZTriggerType.RELATIVE;
    private ZRelated mTriggerRelated;
    private ParsedDuration mTriggerRelative;
    private ParsedDateTime mTriggerAbsolute;

    private ParsedDuration mRepeatDuration;
    private int mRepeatCount;
    // SUMMARY (email subject when mAction=EMAIL)
    private String mSummary;
    private Attach mAttach;
    private List<ZAttendee> mAttendees;

    private String mDescription;

    public ZAlarm(){
    }

    public ZAlarm(Element alarmElem) throws ServiceException{
        ZAction action = ZAction.DISPLAY;
        ZTriggerType triggerType = ZTriggerType.RELATIVE;
        ZRelated triggerRelated = null;
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
        action = ZAction.lookup(val);
        if (action == null)
            throw ServiceException.INVALID_REQUEST(
                    "Invalid " + MailConstants.A_CAL_ALARM_ACTION + " value " + val, null);

        Element triggerElem = alarmElem.getElement(MailConstants.E_CAL_ALARM_TRIGGER);
        Element triggerRelativeElem = triggerElem.getOptionalElement(MailConstants.E_CAL_ALARM_RELATIVE);
        if (triggerRelativeElem != null) {
            triggerType = ZTriggerType.RELATIVE;
            String related = triggerRelativeElem.getAttribute(MailConstants.A_CAL_ALARM_RELATED, null);
            if (related != null) {
                triggerRelated = ZRelated.lookup(related);
                if (triggerRelated == null)
                    throw ServiceException.INVALID_REQUEST(
                            "Invalid " + MailConstants.A_CAL_ALARM_RELATED + " value " + val, null);
            }
            triggerRelative = ParsedDuration.parse(triggerRelativeElem);
        } else {
            triggerType = ZTriggerType.ABSOLUTE;
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
            ZAttendee at = new ZAttendee(attendeesIter.next());
            if (this.mAttendees == null)
                this.mAttendees = new ArrayList<ZAttendee>();
            this.mAttendees.add(at);
        }
        setAction(action);
        setDescription(description);
        setSummary(summary);
        setTriggerRelative(triggerRelative);

    }

    public ZAction getAction() {
        return mAction;
    }

    public void setAction(ZAction mAction) {
        this.mAction = mAction;
    }

    public ZTriggerType getTriggerType() {
        return mTriggerType;
    }

    public void setTriggerType(ZTriggerType triggerType) {
        this.mTriggerType = triggerType;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String mDescription) {
        this.mDescription = mDescription;
    }

    public int getRepeatCount() {
        return mRepeatCount;
    }

    public void setRepeatCount(int mRepeatCount) {
        this.mRepeatCount = mRepeatCount;
    }

    public void setTriggerRelative(ParsedDuration dur){
        this.mTriggerRelative = dur;
    }

    public ParsedDuration getTriggerRelated(){
        return this.mTriggerRelative;
    }

    public void setTriggerAbsolute(ParsedDateTime abs){
        this.mTriggerAbsolute = abs;
    }

    public ParsedDateTime getTriggerAbsolute(){
        return this.mTriggerAbsolute;
    }

    public void setSummary(String summary){
        this.mSummary = summary;
    }

    public String getSummary(){
        return this.mSummary;                                        
    }

    public List<ZAttendee> getAttendees(){
        return this.mAttendees;
    }

    public void setAttendees(List<ZAttendee> attendees){
        this.mAttendees = attendees;
    }

    public void addAttendee(ZAttendee attendee){
        if (this.mAttendees == null){
            this.mAttendees = new ArrayList<ZAttendee>();
        }
        this.mAttendees.add(attendee);
    }


    public Element toElement(Element parent) {
        Element alarm = parent.addElement(MailConstants.E_CAL_ALARM);
        alarm.addAttribute(MailConstants.A_CAL_ALARM_ACTION, mAction.toString());
        Element trigger = alarm.addElement(MailConstants.E_CAL_ALARM_TRIGGER);
        if (ZTriggerType.ABSOLUTE.equals(mTriggerType)) {
            Element absolute = trigger.addElement(MailConstants.E_CAL_ALARM_ABSOLUTE);
            absolute.addAttribute(MailConstants.A_DATE, mTriggerAbsolute.toString());
        } else {
            Element relative = mTriggerRelative.toXml(trigger, MailConstants.E_CAL_ALARM_RELATIVE);
            if (mTriggerRelated != null)
                relative.addAttribute(MailConstants.A_CAL_ALARM_RELATED, mTriggerRelated.toString());
        }
        if (mRepeatDuration != null) {
            Element repeat = mRepeatDuration.toXml(alarm, MailConstants.E_CAL_ALARM_REPEAT);
            repeat.addAttribute(MailConstants.A_CAL_ALARM_COUNT, mRepeatCount);
        }
        if (!ZAction.AUDIO.equals(mAction)) {
            Element desc = alarm.addElement(MailConstants.E_CAL_ALARM_DESCRIPTION);
            if (mDescription != null)
                desc.setText(mDescription);
        }
        if (mAttach != null)
            mAttach.toXml(alarm);
        if (ZAction.EMAIL.equals(mAction) ||
            ZAction.X_YAHOO_CALENDAR_ACTION_IM.equals(mAction) ||
            ZAction.X_YAHOO_CALENDAR_ACTION_MOBILE.equals(mAction)) {
            Element summary = alarm.addElement(MailConstants.E_CAL_ALARM_SUMMARY);
            if (mSummary != null) {
                summary.setText(mSummary);
            }
            if (mAttendees != null) {
                for (ZAttendee attendee : mAttendees) {
                    attendee.toElement(alarm);
                }
            }
        }
        return alarm;
    }
}
