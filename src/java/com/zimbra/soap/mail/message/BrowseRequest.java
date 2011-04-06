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

package com.zimbra.soap.mail.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=MailConstants.E_BROWSE_REQUEST)
public class BrowseRequest {

    // Valid values are case insensitive values from BrowseBy
    @XmlAttribute(name=MailConstants.A_BROWSE_BY, required=true)
    private final String browseBy;

    @XmlAttribute(name=MailConstants.A_REGEX, required=false)
    private final String regex;

    @XmlAttribute(name=MailConstants.A_MAX_TO_RETURN, required=false)
    private final Integer max;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private BrowseRequest() {
        this((String) null, (String) null, (Integer) null);
    }

    public BrowseRequest(String browseBy, String regex, Integer max) {
        this.browseBy = browseBy;
        this.regex = regex;
        this.max = max;
    }

    public String getBrowseBy() { return browseBy; }
    public String getRegex() { return regex; }
    public Integer getMax() { return max; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("browseBy", browseBy)
            .add("regex", regex)
            .add("max", max)
            .toString();
    }
}
