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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.DtValInterface;
import com.zimbra.soap.base.SingleDatesInterface;

@XmlAccessorType(XmlAccessType.NONE)
public class SingleDates
implements RecurRuleBase, SingleDatesInterface {

    /**
     * @zm-api-field-tag TZID
     * @zm-api-field-description TZID
     */
    @XmlAttribute(name=MailConstants.A_CAL_TIMEZONE /* tz */, required=false)
    private String timezone;

    /**
     * @zm-api-field-description Information on start date/time and end date/time or duration
     */
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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("timezone", timezone)
            .add("dtVals", dtVals);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
