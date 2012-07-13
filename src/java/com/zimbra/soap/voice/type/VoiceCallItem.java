/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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

package com.zimbra.soap.voice.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.VoiceConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class VoiceCallItem {

    /**
     * @zm-api-field-tag phone
     * @zm-api-field-description Phone to which the message is for
     */
    @XmlAttribute(name=VoiceConstants.A_PHONE /* phone */, required=true)
    private String phone;

    /**
     * @zm-api-field-tag folder-id
     * @zm-api-field-description Folder id of the folder in which the message resides
     */
    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=true)
    private String folderId;

    /**
     * @zm-api-field-tag sort-field-value
     * @zm-api-field-description Value of the field this search is based on
     */
    @XmlAttribute(name=MailConstants.A_SORT_FIELD /* sf */, required=true)
    private String sortFieldValue;

    /**
     * @zm-api-field-tag msg-duration-secs
     * @zm-api-field-description Message duration in seconds
     */
    @XmlAttribute(name=VoiceConstants.A_VMSG_DURATION /* du */, required=true)
    private int durationInSecs;

    /**
     * @zm-api-field-tag date
     * @zm-api-field-description Timestamp when the message was deposited
     */
    @XmlAttribute(name=MailConstants.A_DATE /* d */, required=true)
    private long date;

    public VoiceCallItem() {
    }

    public void setPhone(String phone) { this.phone = phone; }
    public void setFolderId(String folderId) { this.folderId = folderId; }
    public void setSortFieldValue(String sortFieldValue) { this.sortFieldValue = sortFieldValue; }
    public void setDurationInSecs(int durationInSecs) { this.durationInSecs = durationInSecs; }
    public void setDate(long date) { this.date = date; }
    public String getPhone() { return phone; }
    public String getFolderId() { return folderId; }
    public String getSortFieldValue() { return sortFieldValue; }
    public int getDurationInSecs() { return durationInSecs; }
    public long getDate() { return date; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("phone", phone)
            .add("folderId", folderId)
            .add("sortFieldValue", sortFieldValue)
            .add("durationInSecs", durationInSecs)
            .add("date", date);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
