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
import com.zimbra.soap.base.DateAttrInterface;

@XmlAccessorType(XmlAccessType.NONE)
public class DateAttr implements DateAttrInterface {

    /**
     * @zm-api-field-tag YYYYMMDDThhmmssZ
     * @zm-api-field-description Date in format : <b>YYYYMMDDThhmmssZ</b>
     */
    @XmlAttribute(name=MailConstants.A_DATE, required=true)
    private final String date;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private DateAttr() {
        this((String) null);
    }

    public DateAttr(String date) {
        this.date = date;
    }

    @Override
    public DateAttrInterface create(String date) {
        return new DateAttr(date);
    }

    @Override
    public String getDate() { return date; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("date", date)
            .toString();
    }
}
