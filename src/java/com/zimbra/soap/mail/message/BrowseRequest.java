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

/**
 * @zm-api-command-description Browse
 * <br />
 * EXAMPLE:
 * <pre>
 *     &lt;mail:BrowseRequest xmlns:mail="urn:zimbraMail">
 *        &lt;query>from:roland&lt;/query>
 *        &lt;browseBy>attachments&lt;/browseBy>
 *     &lt;/mail:BrowseRequest>
 *
 *     &lt;mail:BrowseResponse xmlns:mail="urn:zimbraMail">
 *       &lt;bd freq="3">application/pdf&lt;/bd>
 *       &lt;bd freq="1">application/msword&lt;/bd>
 *       &lt;bd freq="1">application/vnd.ms-powerpoint&lt;/bd>
 *       &lt;bd freq="8">image/jpeg&lt;/bd>
 *       &lt;bd freq="11">application/octet-stream&lt;/bd>
 *       &lt;bd freq="23">text/plain&lt;/bd>
 *       &lt;bd freq="1">image/gif&lt;/bd>
 *       &lt;bd freq="12">message/rfc822&lt;/bd>
 *     &lt;/mail:BrowseResponse>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_BROWSE_REQUEST)
public class BrowseRequest {

    // Valid values are case insensitive values from BrowseBy
    /**
     * @zm-api-field-tag browse-by-domains|attachments|objects
     * @zm-api-field-description Browse by setting - <b>domains|attachments|objects</b>
     */
    @XmlAttribute(name=MailConstants.A_BROWSE_BY, required=true)
    private final String browseBy;

    /**
     * @zm-api-field-tag regex-string
     * @zm-api-field-description Regex string.  Return only those results which match the specified regular expression
     */
    @XmlAttribute(name=MailConstants.A_REGEX, required=false)
    private final String regex;

    /**
     * @zm-api-field-tag max-entries
     * @zm-api-field-description Return only a maximum number of entries as requested.  If more than
     * <b>{max-entries}</b> results exist, the server will return the first {max-entries}, sorted by frequency
     */
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
