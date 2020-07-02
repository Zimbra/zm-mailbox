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

package com.zimbra.soap.mail.message;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.MiniCalError;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="GetMiniCalResponse")
@XmlType(propOrder = {"errors", "busyDates"})
public class GetMiniCalResponse {

    /**
     * @zm-api-field-tag busy-dates-yyyymmdd
     * @zm-api-field-description Matching busy dates in format : <b>yyyymmdd</b>
     */
    @XmlElement(name=MailConstants.E_CAL_MINICAL_DATE /* date */, required=false)
    private List<String> busyDates = Lists.newArrayList();

    /**
     * @zm-api-field-description Error for each calendar folder that couldn't be accessed
     */
    @XmlElement(name=MailConstants.E_ERROR /* error */, required=false)
    private List<MiniCalError> errors = Lists.newArrayList();

    public GetMiniCalResponse() {
    }

    public void setErrors(Iterable <MiniCalError> errors) {
        this.errors.clear();
        if (errors != null) {
            Iterables.addAll(this.errors,errors);
        }
    }

    public GetMiniCalResponse addError(MiniCalError error) {
        this.errors.add(error);
        return this;
    }

    public void setBusyDates(Iterable <String> busyDates) {
        this.busyDates.clear();
        if (busyDates != null) {
            Iterables.addAll(this.busyDates,busyDates);
        }
    }

    public GetMiniCalResponse addBusyDate(String busyDate) {
        this.busyDates.add(busyDate);
        return this;
    }

    public List<MiniCalError> getErrors() {
        return Collections.unmodifiableList(errors);
    }
    public List<String> getBusyDates() {
        return Collections.unmodifiableList(busyDates);
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("errors", errors)
            .add("busyDates", busyDates);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
