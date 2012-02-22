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

import java.util.List;

import com.google.common.base.Objects;
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
        return Objects.toStringHelper(this)
            .add("day", day)
            .add("ordWk", ordWk)
            .toString();
    }
}
