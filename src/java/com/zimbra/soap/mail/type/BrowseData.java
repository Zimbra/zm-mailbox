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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class BrowseData {

    /**
     * @zm-api-field-tag h-flags
     * @zm-api-field-description Set for domains.  Indicates whether or not the domain was from the "From", "To", or
     * "Cc" header.  Valid flags are always one of: "f", "t", "ft", "c", "fc", "tc", "ftc"
     */
    @XmlAttribute(name=MailConstants.A_BROWSE_DOMAIN_HEADER /* h */, required=false)
    private final String browseDomainHeader;

    /**
     * @zm-api-field-tag count
     * @zm-api-field-description Frequency count
     */
    @XmlAttribute(name=MailConstants.A_FREQUENCY /* freq */, required=true)
    private final int frequency;

    /**
     * @zm-api-field-tag browse-data
     * @zm-api-field-description Browse data.
     * <table>
     * <tr> <td> <b>for attachments</b> </td> <td> content type (e.g. application/msword) </td> </tr>
     * <tr> <td> <b>for objects</b> </td> <td> object type (url, etc) </td> </tr>
     * <tr> <td> <b>for domains</b> </td> <td> domains (e.g. stanford.edu) <b>{h-flags}</b> will also be set)</td> </tr>
     * </table>
      for attachments: content type (application/msword)

      for objects: object type (url, etc)

      for domains: domains (stanford.edu, etc)
     */
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

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("browseDomainHeader", browseDomainHeader)
            .add("frequency", frequency)
            .add("data", data);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
