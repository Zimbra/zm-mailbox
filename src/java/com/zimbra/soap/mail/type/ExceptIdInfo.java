/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012 Zimbra, Inc.
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
public class ExceptIdInfo {

    /**
     * @zm-api-field-tag recurrence_id_of_exception
     * @zm-api-field-description Recurrence ID of exception
     */
    @XmlAttribute(name=MailConstants.A_CAL_RECURRENCE_ID /* recurId */, required=true)
    private final String recurrenceId;

    /**
     * @zm-api-field-tag invite-id-of-exception
     * @zm-api-field-description Invite ID of exception
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private final String id;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ExceptIdInfo() {
        this((String) null, (String) null);
    }

    public ExceptIdInfo(String recurrenceId, String id) {
        this.recurrenceId = recurrenceId;
        this.id = id;
    }

    public String getRecurrenceId() { return recurrenceId; }
    public String getId() { return id; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("recurrenceId", recurrenceId)
            .add("id", id);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
