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
import com.zimbra.soap.base.DateTimeStringAttrInterface;

@XmlAccessorType(XmlAccessType.NONE)
public class DateTimeStringAttr
implements DateTimeStringAttrInterface {

    /**
     * @zm-api-field-tag YYYYMMDD[ThhmmssZ]
     * @zm-api-field-description Date in format : YYYYMMDD[ThhmmssZ]
     */
    @XmlAttribute(name=MailConstants.A_CAL_DATETIME, required=true)
    private final String dateTime;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private DateTimeStringAttr() {
        this((String) null);
    }

    public DateTimeStringAttr(String dateTime) {
        this.dateTime = dateTime;
    }

    @Override
    public DateTimeStringAttrInterface create(String dateTime) {
        return new DateTimeStringAttr(dateTime);
    }

    @Override
    public String getDateTime() { return dateTime; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("dateTime", dateTime)
            .toString();
    }
}
