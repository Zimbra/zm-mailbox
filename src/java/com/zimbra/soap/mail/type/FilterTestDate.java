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

@XmlAccessorType(XmlAccessType.FIELD)
public class FilterTestDate extends FilterTestInfo {

    @XmlAttribute(name=MailConstants.A_DATE_COMPARISON, required=false)
    private String dateComparison;

    @XmlAttribute(name=MailConstants.A_DATE, required=false)
    private Long date;

    public FilterTestDate() {
    }

    public void setDateComparison(String dateComparison) {
        this.dateComparison = dateComparison;
    }
    public void setDate(Long date) { this.date = date; }
    public String getDateComparison() { return dateComparison; }
    public Long getDate() { return date; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("dateComparison", dateComparison)
            .add("date", date)
            .toString();
    }
}
