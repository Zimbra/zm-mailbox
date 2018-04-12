/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.voice.type;

import com.google.common.base.MoreObjects;
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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("phone", phone)
            .add("folderId", folderId)
            .add("sortFieldValue", sortFieldValue)
            .add("durationInSecs", durationInSecs)
            .add("date", date);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
