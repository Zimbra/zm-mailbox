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

package com.zimbra.soap.mail.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;

/**
 * @zm-api-command-description Get Calendar item summaries
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_GET_CALITEM_SUMMARIES_REQUEST)
public class GetCalendarItemSummariesRequest {

    /**
     * @zm-api-field-tag range-start-millis-gmt
     * @zm-api-field-description Range start in milliseconds since the epoch GMT
     */
    @XmlAttribute(name=MailConstants.A_CAL_START_TIME /* s */, required=true)
    private final long startTime;

    /**
     * @zm-api-field-tag range-end-millis-gmt
     * @zm-api-field-description Range end in milliseconds since the epoch GMT
     */
    @XmlAttribute(name=MailConstants.A_CAL_END_TIME /* e */, required=true)
    private final long endTime;

    /**
     * @zm-api-field-tag folder-id
     * @zm-api-field-description Folder ID.
     */
    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private String folderId;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetCalendarItemSummariesRequest() {
        this(-1L, -1L);
    }

    public GetCalendarItemSummariesRequest(long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void setFolderId(String folderId) { this.folderId = folderId; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public String getFolderId() { return folderId; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("startTime", startTime)
            .add("endTime", endTime)
            .add("folderId", folderId);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
