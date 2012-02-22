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
import com.zimbra.soap.base.WkstRuleInterface;

@XmlAccessorType(XmlAccessType.NONE)
public class WkstRule
implements WkstRuleInterface {

    /**
     * @zm-api-field-tag weekday
     * @zm-api-field-description Weekday -  <b>SU|MO|TU|WE|TH|FR|SA</b>
     */
    @XmlAttribute(name=MailConstants.A_CAL_RULE_DAY, required=true)
    private final String day;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private WkstRule() {
        this((String) null);
    }

    public WkstRule(String day) {
        this.day = day;
    }

    @Override
    public WkstRuleInterface create(String day) {
        return new WkstRule(day);
    }

    @Override
    public String getDay() { return day; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("day", day).toString();
    }
}
