/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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
package com.zimbra.soap.account.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.SmimeConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonAttribute;

@XmlAccessorType(XmlAccessType.NONE)
public class CertificateValidity {

    /**
     * @zm-api-field-tag startDate
     * @zm-api-field-description The date from which certificate validity starts.Value is number of milliseconds since epoch.
     */
    @ZimbraJsonAttribute
    @XmlElement(name=SmimeConstants.E_START_DATE, required=false)
    private long startDate;

    /**
     * @zm-api-field-tag endDate
     * @zm-api-field-description The expiration date.Value is number of milliseconds since epoch.
     */
    @ZimbraJsonAttribute
    @XmlElement(name=SmimeConstants.E_END_DATE, required=false)
    private long endDate;

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getEndDate() {
        return endDate;
    }

    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper.add("startDate", startDate)
            .add("endDate", endDate);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }

}
