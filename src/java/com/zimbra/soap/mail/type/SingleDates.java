/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012 Zimbra, Inc.
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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.DtValInterface;
import com.zimbra.soap.base.SingleDatesInterface;

@XmlAccessorType(XmlAccessType.NONE)
public class SingleDates
implements RecurRuleBase, SingleDatesInterface {

    @XmlAttribute(name=MailConstants.A_CAL_TIMEZONE /* tz */, required=false)
    private String timezone;

    @XmlElement(name=MailConstants.E_CAL_DATE_VAL /* dtval */, required=false)
    private List<DtVal> dtVals = Lists.newArrayList();

    public SingleDates() {
    }

    public void setTimezone(String timezone) { this.timezone = timezone; }
    public void setDtvals(Iterable <DtVal> dtVals) {
        this.dtVals.clear();
        if (dtVals != null) {
            Iterables.addAll(this.dtVals,dtVals);
        }
    }

    public void addDtval(DtVal dtVal) {
        this.dtVals.add(dtVal);
    }

    public String getTimezone() { return timezone; }
    public List<DtVal> getDtvals() {
        return dtVals;
    }

    @Override
    public void setDtValInterfaces(Iterable<DtValInterface> dtVals) {
        setDtvals(DtVal.fromInterfaces(dtVals));
    }

    @Override
    public void addDtValInterface(DtValInterface dtVal) {
        addDtval((DtVal) dtVal);
    }

    @Override
    public List<DtValInterface> getDtValInterfaces() {
        return DtVal.toInterfaces(dtVals);
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("timezone", timezone)
            .add("dtVals", dtVals);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
