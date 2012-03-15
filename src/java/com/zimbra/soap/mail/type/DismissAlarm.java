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
public class DismissAlarm {

    /**
     * @zm-api-field-tag cal-item-id
     * @zm-api-field-description Calendar item ID
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private final String id;

    /**
     * @zm-api-field-tag dismissed-at-millis
     * @zm-api-field-description Time alarm was dismissed, in millis
     */
    @XmlAttribute(name=MailConstants.A_CAL_ALARM_DISMISSED_AT /* dismissedAt */, required=true)
    private final long dismissedAt;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    protected DismissAlarm() {
        this((String) null, -1L);
    }

    public DismissAlarm(String id, long dismissedAt) {
        this.id = id;
        this.dismissedAt = dismissedAt;
    }

    public String getId() { return id; }
    public long getDismissedAt() { return dismissedAt; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("dismissedAt", dismissedAt);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
