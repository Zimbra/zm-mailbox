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
public class FilterTestHeader extends FilterTestInfo {

    // Comma separated list
    @XmlAttribute(name=MailConstants.A_HEADER, required=false)
    private String headers;

    @XmlAttribute(name=MailConstants.A_STRING_COMPARISON, required=false)
    private String stringComparison;

    @XmlAttribute(name=MailConstants.A_VALUE, required=false)
    private String value;

    @XmlAttribute(name=MailConstants.A_CASE_SENSITIVE, required=false)
    private Boolean caseSensitive;

    public FilterTestHeader() {
    }

    public void setHeaders(String headers) { this.headers = headers; }
    public void setStringComparison(String stringComparison) {
        this.stringComparison = stringComparison;
    }
    public void setValue(String value) { this.value = value; }
    public void setCaseSensitive(Boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }
    public String getHeaders() { return headers; }
    public String getStringComparison() { return stringComparison; }
    public String getValue() { return value; }
    public Boolean getCaseSensitive() { return caseSensitive; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("headers", headers)
            .add("stringComparison", stringComparison)
            .add("value", value)
            .add("caseSensitive", caseSensitive)
            .toString();
    }
}
