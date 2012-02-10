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

package com.zimbra.soap.admin.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.ByMonthDayRuleInterface;

@XmlAccessorType(XmlAccessType.NONE)
public class ByMonthDayRule implements ByMonthDayRuleInterface {

    /**
     * @zm-api-field-tag modaylist
     * @zm-api-field-description Comma separated list of day numbers from either the start (positive) or the
     * end (negative) of the month - format : <b>[[+]|-]num[,...]</b>   where num between 1 to 31
     * <br />
     * e.g. <b>modaylist="1,+2,-7"</b>
     * <br />
     * means first day of the month, plus the 2nd day of the month, plus the 7th from last day of the month.
     */
    @XmlAttribute(name=MailConstants.A_CAL_RULE_BYMONTHDAY_MODAYLIST /* modaylist */, required=true)
    private final String list;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ByMonthDayRule() {
        this((String) null);
    }

    public ByMonthDayRule(String list) {
        this.list = list;
    }

    @Override
    public ByMonthDayRuleInterface create(String list) {
        return new ByMonthDayRule(list);
    }

    @Override
    public String getList() { return list; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("list", list)
            .toString();
    }
}
