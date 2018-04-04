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

package com.zimbra.soap.mail.message;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.CalendarItemRecur;
import com.zimbra.soap.mail.type.CalTZInfo;
import com.zimbra.soap.mail.type.CancelItemRecur;
import com.zimbra.soap.mail.type.ExceptionItemRecur;
import com.zimbra.soap.mail.type.InviteItemRecur;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="GetRecurResponse")
@XmlType(propOrder = {"timezone", "components"})
public class GetRecurResponse {

    /**
     * @zm-api-field-description Timezone
     */
    @XmlElement(name=MailConstants.E_CAL_TZ /* tz */, required=false)
    private CalTZInfo timezone;

    /**
     * @zm-api-field-description Recurrence components
     */
    @XmlElements({
        @XmlElement(name=MailConstants.E_CAL_CANCEL /* cancel */, type=CancelItemRecur.class),
        @XmlElement(name=MailConstants.E_CAL_EXCEPT /* except */, type=ExceptionItemRecur.class),
        @XmlElement(name=MailConstants.E_INVITE_COMPONENT /* comp */, type=InviteItemRecur.class)
    })
    private List<CalendarItemRecur> components = Lists.newArrayList();

    public GetRecurResponse() {
    }

    public void setTimezone(CalTZInfo timezone) { this.timezone = timezone; }
    public void setComponents(Iterable <CalendarItemRecur> components) {
        this.components.clear();
        if (components != null) {
            Iterables.addAll(this.components,components);
        }
    }

    public GetRecurResponse addComponent(CalendarItemRecur component) {
        this.components.add(component);
        return this;
    }

    public CalTZInfo getTimezone() { return timezone; }
    public List<CalendarItemRecur> getComponents() {
        return Collections.unmodifiableList(components);
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("timezone", timezone)
            .add("components", components);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
