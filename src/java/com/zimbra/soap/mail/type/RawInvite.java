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
public class RawInvite {

    /**
     * @zm-api-field-tag UID
     * @zm-api-field-description UID
     */
    @XmlAttribute(name=MailConstants.A_UID /* uid */, required=false)
    private String uid;

    /**
     * @zm-api-field-tag summary
     * @zm-api-field-description summary
     */
    @XmlAttribute(name=MailConstants.A_SUMMARY /* summary */, required=false)
    private String summary;

    /**
     * @zm-api-field-tag raw-icalendar
     * @zm-api-field-description Raw iCalendar data
     */
    @XmlValue
    private String content;

    public RawInvite() {
    }

    public void setUid(String uid) { this.uid = uid; }
    public void setSummary(String summary) { this.summary = summary; }
    public void setContent(String content) { this.content = content; }
    public String getUid() { return uid; }
    public String getSummary() { return summary; }
    public String getContent() { return content; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("uid", uid)
            .add("summary", summary)
            .add("content", content);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
