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
import com.zimbra.soap.base.ByWeekNoRuleInterface;

@XmlAccessorType(XmlAccessType.NONE)
public class ByWeekNoRule implements ByWeekNoRuleInterface {

    /**
     * @zm-api-field-tag byweekno-wklist
     * @zm-api-field-description BYWEEKNO Week list.  Format : <b>[[+]|-]num[,...]</b> where num is between 1 and 53
     * <br />
     * e.g. <b>&lt;byweekno wklist="1,+2,-1"/></b> means first week, 2nd week, and last week of the year.
     */
    @XmlAttribute(name=MailConstants.A_CAL_RULE_BYWEEKNO_WKLIST /* wklist */, required=true)
    private final String list;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ByWeekNoRule() {
        this((String) null);
    }

    public ByWeekNoRule(String list) {
        this.list = list;
    }

    @Override
    public ByWeekNoRuleInterface create(String list) {
        return new ByWeekNoRule(list);
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
