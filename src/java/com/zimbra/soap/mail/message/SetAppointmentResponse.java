/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

package com.zimbra.soap.mail.message;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.ExceptIdInfo;
import com.zimbra.soap.type.Id;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="SetAppointmentResponse")
@XmlType(propOrder = {"defaultId", "exceptions"})
public class SetAppointmentResponse {

    /**
     * @zm-api-field-tag appointment-id
     * @zm-api-field-description Appointment ID
     */
    @XmlAttribute(name=MailConstants.A_CAL_ID /* calItemId */, required=false)
    private String calItemId;

    // For backwards compat
    /**
     * @zm-api-field-tag appt-id
     * @zm-api-field-description Deprecated - appointment ID
     */
    @XmlAttribute(name=MailConstants.A_APPT_ID_DEPRECATE_ME /* apptId */, required=false)
    private String deprecatedApptId;

    /**
     * @zm-api-field-description Information about default invite
     */
    @XmlElement(name=MailConstants.A_DEFAULT /* default */, required=false)
    private Id defaultId;

    /**
     * @zm-api-field-description Information about exceptions
     */
    @XmlElement(name=MailConstants.E_CAL_EXCEPT /* except */, required=false)
    private List<ExceptIdInfo> exceptions = Lists.newArrayList();

    public SetAppointmentResponse() {
    }

    public void setCalItemId(String calItemId) { this.calItemId = calItemId; }
    public void setDeprecatedApptId(String deprecatedApptId) {
        this.deprecatedApptId = deprecatedApptId;
    }
    public void setDefaultId(Id defaultId) { this.defaultId = defaultId; }
    public void setExceptions(Iterable <ExceptIdInfo> exceptions) {
        this.exceptions.clear();
        if (exceptions != null) {
            Iterables.addAll(this.exceptions,exceptions);
        }
    }

    public void addException(ExceptIdInfo exception) {
        this.exceptions.add(exception);
    }

    public String getCalItemId() { return calItemId; }
    public String getDeprecatedApptId() { return deprecatedApptId; }
    public Id getDefaultId() { return defaultId; }
    public List<ExceptIdInfo> getExceptions() {
        return Collections.unmodifiableList(exceptions);
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("calItemId", calItemId)
            .add("deprecatedApptId", deprecatedApptId)
            .add("defaultId", defaultId)
            .add("exceptions", exceptions);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
