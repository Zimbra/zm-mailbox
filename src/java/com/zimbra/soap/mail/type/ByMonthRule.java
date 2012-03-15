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
import com.zimbra.soap.base.ByMonthRuleInterface;

@XmlAccessorType(XmlAccessType.NONE)
public class ByMonthRule implements ByMonthRuleInterface {

    /**
     * @zm-api-field-tag month-list
     * @zm-api-field-description Comma separated list of months where month is a number between 1 and 12
     */
    @XmlAttribute(name=MailConstants.A_CAL_RULE_BYMONTH_MOLIST /* molist */, required=true)
    private final String list;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ByMonthRule() {
        this((String) null);
    }

    public ByMonthRule(String list) {
        this.list = list;
    }

    @Override
    public ByMonthRuleInterface create(String list) {
        return new ByMonthRule(list);
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
