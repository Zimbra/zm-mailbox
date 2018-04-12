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

import java.util.List;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.WkDayInterface;

@XmlAccessorType(XmlAccessType.NONE)
public class WkDay implements WkDayInterface {

    /**
     * @zm-api-field-tag weekday
     * @zm-api-field-description Weekday -  <b>SU|MO|TU|WE|TH|FR|SA</b>
     */
    @XmlAttribute(name=MailConstants.A_CAL_RULE_DAY /* day */, required=true)
    private final String day;

    /**
     * @zm-api-field-tag ord-wk-[[+]|-]num
     * @zm-api-field-description Week number.  <b>[[+]|-]num</b> num: 1 to 53
     */
    @XmlAttribute(name=MailConstants.A_CAL_RULE_BYDAY_WKDAY_ORDWK /* ordwk */, required=false)
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

    @Override
    public WkDayInterface create(String day) {
        return new WkDay(day);
    }

    @Override
    public void setOrdWk(Integer ordWk) { this.ordWk = ordWk; }
    @Override
    public String getDay() { return day; }
    @Override
    public Integer getOrdWk() { return ordWk; }

    public static Iterable <WkDay> fromInterfaces(Iterable <WkDayInterface> params) {
        if (params == null)
            return null;
        List <WkDay> newList = Lists.newArrayList();
        for (WkDayInterface param : params) {
            newList.add((WkDay) param);
        }
        return newList;
    }

    public static List <WkDayInterface> toInterfaces(Iterable <WkDay> params) {
        if (params == null)
            return null;
        List <WkDayInterface> newList = Lists.newArrayList();
        Iterables.addAll(newList, params);
        return newList;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("day", day)
            .add("ordWk", ordWk)
            .toString();
    }
}
