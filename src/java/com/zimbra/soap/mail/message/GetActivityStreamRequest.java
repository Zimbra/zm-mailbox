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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.OctopusXmlConstants;
import com.zimbra.soap.mail.type.ActivityFilter;

/**
 * @zm-api-command-description Get activity stream
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=OctopusXmlConstants.E_GET_ACTIVITY_STREAM_REQUEST)
public class GetActivityStreamRequest {

    /**
     * @zm-api-field-tag item-id
     * @zm-api-field-description Item ID.  If the id is for a Document, the response will include the activities for
     * the requested Document.  if it is for a Folder, the response will include the activities for all the Documents
     * in the folder and subfolders.
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private final String id;

    /**
     * @zm-api-field-tag offset
     * @zm-api-field-description Offset - for getting the next page worth of activities
     */
    @XmlAttribute(name=MailConstants.A_QUERY_OFFSET /* offset */, required=false)
    private Integer queryOffset;

    /**
     * @zm-api-field-tag limit
     * @zm-api-field-description Limit - maximum number of activities to be returned
     */
    @XmlAttribute(name=MailConstants.A_QUERY_LIMIT /* limit */, required=false)
    private Integer queryLimit;

    /**
     * @zm-api-field-description Optionally <b>&lt;filter></b> can be used to filter the response based on the user
     * that performed the activity, operation, or both.  the server will cache previously established filter search
     * results, and return the identifier in session attribute.  The client is expected to reuse the session
     * identifier in the subsequent filter search to improve the performance.
     */
    @XmlElement(name=MailConstants.E_FILTER /* filter */, required=false)
    private ActivityFilter filter;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetActivityStreamRequest() {
        this((String) null);
    }

    public GetActivityStreamRequest(String id) {
        this.id = id;
    }

    public void setQueryOffset(Integer queryOffset) { this.queryOffset = queryOffset; }
    public void setQueryLimit(Integer queryLimit) { this.queryLimit = queryLimit; }
    public void setFilter(ActivityFilter filter) { this.filter = filter; }

    public String getId() { return id; }
    public Integer getQueryOffset() { return queryOffset; }
    public Integer getQueryLimit() { return queryLimit; }
    public ActivityFilter getFilter() { return filter; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("queryOffset", queryOffset)
            .add("queryLimit", queryLimit);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
