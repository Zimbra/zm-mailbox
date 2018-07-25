/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.type;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.AlarmInfoInterface;
import com.zimbra.soap.base.AlarmTriggerInfoInterface;
import com.zimbra.soap.base.CalendarAttachInterface;
import com.zimbra.soap.base.CalendarAttendeeInterface;
import com.zimbra.soap.base.DurationInfoInterface;
import com.zimbra.soap.base.XPropInterface;

@XmlAccessorType(XmlAccessType.NONE)
public class AlarmInfo implements AlarmInfoInterface {

    /**
     * @zm-api-field-tag alarm-action
     * @zm-api-field-description Alarm action
     * <br />
     * Possible values:
     * <br />
     * <b>DISPLAY|AUDIO|EMAIL|PROCEDURE|X_YAHOO_CALENDAR_ACTION_IM|X_YAHOO_CALENDAR_ACTION_MOBILE</b>
     */
    @XmlAttribute(name=MailConstants.A_CAL_ALARM_ACTION /* action */, required=true)
    private final String action;

    /**
     * @zm-api-field-description Alarm trigger information
     */
    @XmlElement(name=MailConstants.E_CAL_ALARM_TRIGGER /* trigger */, required=false)
    private AlarmTriggerInfo trigger;

    /**
     * @zm-api-field-description Alarm repeat information
     */
    @XmlElement(name=MailConstants.E_CAL_ALARM_REPEAT /* repeat */, required=false)
    private DurationInfo repeat;

    /**
     * @zm-api-field-tag alarm-reminder-text
     * @zm-api-field-description Alarm description
     * <table>
     * <tr> <td> <b>action=DISPLAY</b> </td> <td> Reminder text to display</td> </tr>
     * <tr> <td> <b>action=EMAIL|X_YAHOO_CALENDAR_ACTION_IM|X_YAHOO_CALENDAR_ACTION_MOBILE</b> </td>
     *      <td> EMail body </td> </tr>
     * <tr> <td> <b>action=PROCEDURE</b> </td> <td> Description text </td> </tr>
     * </table>
     */
    @XmlElement(name=MailConstants.E_CAL_ALARM_DESCRIPTION /* desc */, required=false)
    private String description;

    /**
     * @zm-api-field-description Information on attachment
     */
    @XmlElement(name=MailConstants.E_CAL_ATTACH /* attach */, required=false)
    private CalendarAttach attach;

    /**
     * @zm-api-field-tag alarm-summary
     * @zm-api-field-description Alarm summary
     */
    @XmlElement(name=MailConstants.E_CAL_ALARM_SUMMARY /* summary */, required=false)
    private String summary;

    /**
     * @zm-api-field-description Attendee information
     */
    @XmlElement(name=MailConstants.E_CAL_ATTENDEE /* at */, required=false)
    private List<CalendarAttendee> attendees = Lists.newArrayList();

    /**
     * @zm-api-field-description Non-standard properties (see RFC2445 section 4.8.8.1)
     * <br />
     * e.g.
     * <br />
     * iCalendar:
     * <pre>
     *     X-FOO-HELLO;X-FOO-WORLD=world:hello
     * </pre>
     * SOAP:
     * <pre>
     *     &lt;xprop name="X-FOO-HELLO" value="hello">
     *         &lt;xparam name="X-FOO-WORLD" value="world"/>
     *     &lt;/xprop>
     * </pre>
     */
    @XmlElement(name=MailConstants.E_CAL_XPROP /* xprop */, required=false)
    private List<XProp> xProps = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AlarmInfo() {
        this((String) null);
    }

    public AlarmInfo(String action) {
        this.action = action;
    }

    @Override
    public AlarmInfoInterface createFromAction(String action) {
        return new AlarmInfo(action);
    }

    public void setTrigger(AlarmTriggerInfo trigger) {
        this.trigger = trigger;
    }

    public void setRepeat(DurationInfo repeat) { this.repeat = repeat; }
    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    public void setAttach(CalendarAttach attach) { this.attach = attach; }
    @Override
    public void setSummary(String summary) { this.summary = summary; }
    public void setAttendees(Iterable <CalendarAttendee> attendees) {
        this.attendees.clear();
        if (attendees != null) {
            Iterables.addAll(this.attendees,attendees);
        }
    }

    public void addAttendee(CalendarAttendee attendee) {
        this.attendees.add(attendee);
    }

    public void setXProps(Iterable <XProp> xProps) {
        this.xProps.clear();
        if (xProps != null) {
            Iterables.addAll(this.xProps,xProps);
        }
    }

    public void addXProp(XProp xProp) {
        this.xProps.add(xProp);
    }

    @Override
    public String getAction() { return action; }
    public AlarmTriggerInfo getTrigger() { return trigger; }
    public DurationInfo getRepeat() { return repeat; }
    @Override
    public String getDescription() { return description; }
    public CalendarAttach getAttach() { return attach; }
    @Override
    public String getSummary() { return summary; }
    public List<CalendarAttendee> getAttendees() {
        return Collections.unmodifiableList(attendees);
    }
    public List<XProp> getXProps() {
        return Collections.unmodifiableList(xProps);
    }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("action", action)
            .add("trigger", trigger)
            .add("repeat", repeat)
            .add("description", description)
            .add("attach", attach)
            .add("summary", summary)
            .add("attendees", attendees)
            .add("xProps", xProps);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }

    @Override
    public void setTriggerInterface(AlarmTriggerInfoInterface trigger) {
        setTrigger((AlarmTriggerInfo) trigger);
    }

    @Override
    public void setRepeatInterface(DurationInfoInterface repeat) {
        setRepeat((DurationInfo) repeat);
    }

    @Override
    public void setAttachInterface(CalendarAttachInterface attach) {
        setAttach((CalendarAttach) attach);
    }

    @Override
    public void setAttendeeInterfaces(
            Iterable<CalendarAttendeeInterface> attendees) {
        setAttendees(CalendarAttendee.fromInterfaces(attendees));
    }

    @Override
    public void addAttendeeInterface(CalendarAttendeeInterface attendee) {
        addAttendee((CalendarAttendee) attendee);
    }

    @Override
    public void setXPropsInterface(Iterable<XPropInterface> xProps) {
        setXProps(XProp.fromInterfaces(xProps));
    }

    @Override
    public void addXPropInterface(XPropInterface xProp) {
        addXProp((XProp) xProp);
    }

    @Override
    public AlarmTriggerInfoInterface getTriggerInfo() {
        return trigger;
    }

    @Override
    public DurationInfoInterface getRepeatInfo() {
        return repeat;
    }

    @Override
    public CalendarAttachInterface getAttachInfo() {
        return attach;
    }

    @Override
    public List<CalendarAttendeeInterface> getAttendeeInterfaces() {
        return CalendarAttendee.toInterfaces(attendees);
    }

    @Override
    public List<XPropInterface> getXPropInterfaces() {
        return XProp.toInterfaces(xProps);
    }

    public static Iterable <AlarmInfo> fromInterfaces(Iterable <AlarmInfoInterface> params) {
        if (params == null)
            return null;
        List <AlarmInfo> newList = Lists.newArrayList();
        for (AlarmInfoInterface param : params) {
            newList.add((AlarmInfo) param);
        }
        return newList;
    }

    public static List <AlarmInfoInterface> toInterfaces(Iterable <AlarmInfo> params) {
        if (params == null)
            return null;
        List <AlarmInfoInterface> newList = Lists.newArrayList();
        Iterables.addAll(newList, params);
        return newList;
    }
}
