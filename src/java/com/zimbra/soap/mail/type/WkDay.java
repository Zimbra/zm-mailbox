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

@XmlAccessorType(XmlAccessType.FIELD)
public class WkDay {

    @XmlAttribute(name=MailConstants.A_CAL_RULE_DAY, required=true)
    private final String day;

    @XmlAttribute(name=MailConstants.A_CAL_RULE_BYDAY_WKDAY_ORDWK,
                    required=false)
    private Integer ordWk;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private WkDay() {
        this((String) null);
    }

    public WkDay(String day) {
        this.day = day;
    }

    public void setOrdWk(Integer ordWk) { this.ordWk = ordWk; }
    public String getDay() { return day; }
    public Integer getOrdWk() { return ordWk; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("day", day)
            .add("ordWk", ordWk)
            .toString();
    }
}
