/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.mail.message;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.OctopusXmlConstants;
import com.zimbra.soap.mail.type.ActivityFilter;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("queryOffset", queryOffset)
            .add("queryLimit", queryLimit);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
