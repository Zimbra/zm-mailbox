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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.ByDayRuleInterface;
import com.zimbra.soap.base.WkDayInterface;

@XmlAccessorType(XmlAccessType.NONE)
public class ByDayRule implements ByDayRuleInterface {

    /**
     * @zm-api-field-description By day weekday rule specification
     */
    @XmlElement(name=MailConstants.E_CAL_RULE_BYDAY_WKDAY /* wkday */, required=false)
    private List<WkDay> days = Lists.newArrayList();

    public ByDayRule() {
    }

    public void setDays(Iterable <WkDay> days) {
        this.days.clear();
        if (days != null) {
            Iterables.addAll(this.days,days);
        }
    }

    public ByDayRule addDay(WkDay day) {
        this.days.add(day);
        return this;
    }

    public List<WkDay> getDays() {
        return Collections.unmodifiableList(days);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("days", days)
            .toString();
    }

    @Override
    public void setDayInterfaces(Iterable<WkDayInterface> days) {
        setDays(WkDay.fromInterfaces(days));
    }

    @Override
    public void addDayInterface(WkDayInterface day) {
        addDay((WkDay) day);
    }

    @Override
    public List<WkDayInterface> getDayInterfaces() {
        return WkDay.toInterfaces(days);
    }
}
