/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.type;

import com.google.common.base.MoreObjects;
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
        return MoreObjects.toStringHelper(this)
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
