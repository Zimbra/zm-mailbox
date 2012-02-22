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
public class LegacyInstanceDataAttrs
extends CommonInstanceDataAttrs {

    // MailConstants.A_CAL_DURATION == "d" == MailConstants.A_DATE.
    // This eclipses the date setting - hence the existence of
    // InstanceDataAttrs which uses A_CAL_NEW_DURATION
    /**
     * @zm-api-field-tag duration
     * @zm-api-field-description Duration
     */
    @XmlAttribute(name=MailConstants.A_CAL_DURATION /* "d" */, required=false)
    private Long duration;

    public LegacyInstanceDataAttrs() {
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }
    public Long getDuration() { return duration; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("duration", duration);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
