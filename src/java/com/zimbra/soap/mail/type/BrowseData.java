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
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class BrowseData {

    @XmlAttribute(name=MailConstants.A_BROWSE_DOMAIN_HEADER, required=false)
    private final String browseDomainHeader;

    @XmlAttribute(name=MailConstants.A_FREQUENCY, required=true)
    private final int frequency;

    @XmlValue
    private final String data;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private BrowseData() {
        this((String) null, -1, (String) null);
    }

    public BrowseData(String browseDomainHeader, int frequency, String data) {
        this.browseDomainHeader = browseDomainHeader;
        this.frequency = frequency;
        this.data = data;
    }

    public String getBrowseDomainHeader() { return browseDomainHeader; }
    public int getFrequency() { return frequency; }
    public String getData() { return data; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("browseDomainHeader", browseDomainHeader)
            .add("frequency", frequency)
            .add("data", data)
            .toString();
    }
}
