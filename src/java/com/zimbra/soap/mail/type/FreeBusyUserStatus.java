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

package com.zimbra.soap.mail.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class FreeBusyUserStatus {

    /**
     * @zm-api-field-tag email
     * @zm-api-field-description Email address for a user who has a conflict with the instance
     */
    @XmlAttribute(name=MailConstants.A_NAME /* name */, required=true)
    private final String name;

    /**
     * @zm-api-field-tag freebusy-status-B|T|O
     * @zm-api-field-description Free/Busy status - <b>B|T|O</b> (Busy, Tentative or Out-of-office)
     */
    @XmlAttribute(name=MailConstants.A_APPT_FREEBUSY /* fb */, required=true)
    private final String freebusyStatus;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private FreeBusyUserStatus() {
        this((String) null, (String) null);
    }

    public FreeBusyUserStatus(String name, String freebusyStatus) {
        this.name = name;
        this.freebusyStatus = freebusyStatus;
    }

    public String getName() { return name; }
    public String getFreebusyStatus() { return freebusyStatus; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("freebusyStatus", freebusyStatus);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
