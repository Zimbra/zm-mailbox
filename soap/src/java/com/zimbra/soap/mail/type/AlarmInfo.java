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

package com.zimbra.soap.mail.type;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.AlarmInfoInterface;
import com.zimbra.soap.base.AlarmTriggerInfoInterface;
import com.zimbra.soap.base.CalendarAttachInterface;
import com.zimbra.soap.base.CalendarAttendeeInterface;
import com.zimbra.soap.base.DurationInfoInterface;
import com.zimbra.soap.base.XPropInterface;

import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name=GqlConstants.CLASS_ALARM_INFORMATION, description="Alarm information")
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
    @GraphQLNonNull
    @GraphQLQuery(name=GqlConstants.ACTION, description="Alarm action\n "
        + "> Possible values:\n "
        + "* DISPLAY|AUDIO|EMAIL|PROCEDURE|X_YAHOO_CALENDAR_ACTION_IM|X_YAHOO_CALENDAR_ACTION_MOBILE")
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
    @GraphQLQuery(name=GqlConstants.ATTENDEES, description="Attendee information")
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
    @GraphQLQuery(name=GqlConstants.XPROPS, description="Non-standard properties")
    private List<XProp> xProps = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AlarmInfo() {
        this((String) null);
    }

    public AlarmInfo(
        @GraphQLNonNull @GraphQLInputField(name=GqlConstants.ACTION) String action) {
        this.action = action;
    }

    @Override
    public AlarmInfoInterface createFromAction(String action) {
        return new AlarmInfo(action);
    }

    @GraphQLInputField(name=GqlConstants.TRIGGER, description="Alarm trigger information")
    public void setTrigger(AlarmTriggerInfo trigger) {
        this.trigger = trigger;
    }

    @GraphQLInputField(name=GqlConstants.REPEAT, description="Alarm repeat information")
    public void setRepeat(DurationInfo repeat) { this.repeat = repeat; }
    @Override
    @GraphQLInputField(name=GqlConstants.DESCRIPTION, description="Alarm description\n "
        + "* action=DISPLAY: Reminder text to display\n "
        + "* action=EMAIL|X_YAHOO_CALENDAR_ACTION_IM|X_YAHOO_CALENDAR_ACTION_MOBILE: EMail body \n"
        + "* action=PROCEDURE: Description text")
    public void setDescription(String description) {
        this.description = description;
    }

    @GraphQLInputField(name=GqlConstants.ATTACHMENT, description="Information on attachment")
    public void setAttach(CalendarAttach attach) { this.attach = attach; }
    @Override
    @GraphQLInputField(name=GqlConstants.SUMMARY, description="Alarm summary")
    public void setSummary(String summary) { this.summary = summary; }
    @GraphQLInputField(name=GqlConstants.ATTENDEES, description="Attendee information")
    public void setAttendees(Iterable <CalendarAttendee> attendees) {
        this.attendees.clear();
        if (attendees != null) {
            Iterables.addAll(this.attendees,attendees);
        }
    }

    @GraphQLIgnore
    public void addAttendee(CalendarAttendee attendee) {
        this.attendees.add(attendee);
    }

    @GraphQLInputField(name=GqlConstants.XPROPS, description="Non-standard properties")
    public void setXProps(Iterable <XProp> xProps) {
        this.xProps.clear();
        if (xProps != null) {
            Iterables.addAll(this.xProps,xProps);
        }
    }

    @GraphQLIgnore
    public void addXProp(XProp xProp) {
        this.xProps.add(xProp);
    }

    @Override
    public String getAction() { return action; }
    @GraphQLQuery(name=GqlConstants.TRIGGER, description="Alarm trigger information")
    public AlarmTriggerInfo getTrigger() { return trigger; }
    @GraphQLQuery(name=GqlConstants.REPEAT, description="Alarm repeat information")
    public DurationInfo getRepeat() { return repeat; }
    @Override
    @GraphQLQuery(name=GqlConstants.DESCRIPTION, description="Alarm description\n "
        + "* action=DISPLAY: Reminder text to display\n "
        + "* action=EMAIL|X_YAHOO_CALENDAR_ACTION_IM|X_YAHOO_CALENDAR_ACTION_MOBILE: EMail body \n"
        + "* action=PROCEDURE: Description text")
    public String getDescription() { return description; }
    @GraphQLQuery(name=GqlConstants.ATTACHMENT, description="Information on attachment")
    public CalendarAttach getAttach() { return attach; }
    @Override
    @GraphQLQuery(name=GqlConstants.SUMMARY, description="Alarm summary")
    public String getSummary() { return summary; }
    @GraphQLQuery(name=GqlConstants.ATTENDEES, description="Attendee information")
    public List<CalendarAttendee> getAttendees() {
        return Collections.unmodifiableList(attendees);
    }
    @GraphQLQuery(name=GqlConstants.XPROPS, description="Non-standard properties")
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
    @GraphQLIgnore
    public void setTriggerInterface(AlarmTriggerInfoInterface trigger) {
        setTrigger((AlarmTriggerInfo) trigger);
    }

    @Override
    @GraphQLIgnore
    public void setRepeatInterface(DurationInfoInterface repeat) {
        setRepeat((DurationInfo) repeat);
    }

    @Override
    @GraphQLIgnore
    public void setAttachInterface(CalendarAttachInterface attach) {
        setAttach((CalendarAttach) attach);
    }

    @Override
    @GraphQLIgnore
    public void setAttendeeInterfaces(
            Iterable<CalendarAttendeeInterface> attendees) {
        setAttendees(CalendarAttendee.fromInterfaces(attendees));
    }

    @Override
    @GraphQLIgnore
    public void addAttendeeInterface(CalendarAttendeeInterface attendee) {
        addAttendee((CalendarAttendee) attendee);
    }

    @Override
    @GraphQLIgnore
    public void setXPropsInterface(Iterable<XPropInterface> xProps) {
        setXProps(XProp.fromInterfaces(xProps));
    }

    @Override
    @GraphQLIgnore
    public void addXPropInterface(XPropInterface xProp) {
        addXProp((XProp) xProp);
    }

    @Override
    @GraphQLIgnore
    public AlarmTriggerInfoInterface getTriggerInfo() {
        return trigger;
    }

    @Override
    @GraphQLIgnore
    public DurationInfoInterface getRepeatInfo() {
        return repeat;
    }

    @Override
    @GraphQLIgnore
    public CalendarAttachInterface getAttachInfo() {
        return attach;
    }

    @Override
    @GraphQLIgnore
    public List<CalendarAttendeeInterface> getAttendeeInterfaces() {
        return CalendarAttendee.toInterfaces(attendees);
    }

    @Override
    @GraphQLIgnore
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

    /**
     * Returns the alarm trigger time in millis.
     * Both start and end times of the appointment/task instance are required because the alarm
     * may be specified relative to either start or end time.
     * @param instStart start time of the appointment/task instance
     * @param instEnd end time of the appointment/task instance
     * @return
     */
    @GraphQLIgnore
    public long getTriggerTime(long instStart, long instEnd) {
        if (trigger == null) {
            return instStart;// start time is the trigger time, if trigger not found
        }
        if (trigger.getAbsolute() != null) {
            DateAttr da = trigger.getAbsolute();
            LocalDateTime ldt = LocalDateTime.parse(da.getDate(), DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
            return ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
        } else if (trigger.getRelative() != null) {
            DurationInfo di = trigger.getRelative();
            String rel = di.getRelated();
            if ("END".equals(rel)) {
                Instant ednInst = Instant.ofEpochMilli(instEnd);
                if (di.getDurationNegative() != null && di.getDurationNegative()) {
                    if (di.getWeeks() != null) {
                        return ednInst.minus(di.getWeeks().intValue(), ChronoUnit.WEEKS).toEpochMilli();
                    } else if (di.getDays() != null) {
                        return ednInst.minus(di.getDays().intValue(), ChronoUnit.DAYS).toEpochMilli();
                    } else if (di.getHours() != null) {
                        return ednInst.minus(di.getHours().intValue(), ChronoUnit.HOURS).toEpochMilli();
                    } else if (di.getMinutes() != null) {
                        return ednInst.minus(di.getMinutes().intValue(), ChronoUnit.MINUTES).toEpochMilli();
                    } else if (di.getSeconds() != null) {
                        return ednInst.minus(di.getSeconds().intValue(), ChronoUnit.SECONDS).toEpochMilli();
                    } else {
                        return instEnd;
                    }
                } else {
                    if (di.getWeeks() != null) {
                        return ednInst.plus(di.getWeeks().intValue(), ChronoUnit.WEEKS).toEpochMilli();
                    } else if (di.getDays() != null) {
                        return ednInst.plus(di.getDays().intValue(), ChronoUnit.DAYS).toEpochMilli();
                    } else if (di.getHours() != null) {
                        return ednInst.plus(di.getHours().intValue(), ChronoUnit.HOURS).toEpochMilli();
                    } else if (di.getMinutes() != null) {
                        return ednInst.plus(di.getMinutes().intValue(), ChronoUnit.MINUTES).toEpochMilli();
                    } else if (di.getSeconds() != null) {
                        return ednInst.plus(di.getSeconds().intValue(), ChronoUnit.SECONDS).toEpochMilli();
                    } else {
                        return instEnd;
                    }
                }
            } else {
                Instant startInst = Instant.ofEpochMilli(instStart);
                if (di.getDurationNegative() != null && di.getDurationNegative()) {
                    if (di.getWeeks() != null) {
                        return startInst.minus(di.getWeeks().intValue(), ChronoUnit.WEEKS).toEpochMilli();
                    } else if (di.getDays() != null) {
                        return startInst.minus(di.getDays().intValue(), ChronoUnit.DAYS).toEpochMilli();
                    } else if (di.getHours() != null) {
                        return startInst.minus(di.getHours().intValue(), ChronoUnit.HOURS).toEpochMilli();
                    } else if (di.getMinutes() != null) {
                        return startInst.minus(di.getMinutes().intValue(), ChronoUnit.MINUTES).toEpochMilli();
                    } else if (di.getSeconds() != null) {
                        return startInst.minus(di.getSeconds().intValue(), ChronoUnit.SECONDS).toEpochMilli();
                    } else {
                        return instStart;
                    }
                } else {
                    if (di.getWeeks() != null) {
                        return startInst.plus(di.getWeeks().intValue(), ChronoUnit.WEEKS).toEpochMilli();
                    } else if (di.getDays() != null) {
                        return startInst.plus(di.getDays().intValue(), ChronoUnit.DAYS).toEpochMilli();
                    } else if (di.getHours() != null) {
                        return startInst.plus(di.getHours().intValue(), ChronoUnit.HOURS).toEpochMilli();
                    } else if (di.getMinutes() != null) {
                        return startInst.plus(di.getMinutes().intValue(), ChronoUnit.MINUTES).toEpochMilli();
                    } else if (di.getSeconds() != null) {
                        return startInst.plus(di.getSeconds().intValue(), ChronoUnit.SECONDS).toEpochMilli();
                    } else {
                        return instStart;
                    }
                }
            }
        } else {
            return instStart;// start time is the trigger time, if trigger type is neither absolute nor relative
        }
    }
}
