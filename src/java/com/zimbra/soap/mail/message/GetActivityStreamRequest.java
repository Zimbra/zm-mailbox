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
import com.zimbra.common.soap.OctopusXmlConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=OctopusXmlConstants.E_GET_ACTIVITY_STREAM_REQUEST)
public class GetActivityStreamRequest {

    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    @XmlAttribute(name=MailConstants.A_QUERY_OFFSET /* offset */, required=false)
    private Integer queryOffset;

    @XmlAttribute(name=MailConstants.A_QUERY_LIMIT /* limit */, required=false)
    private Integer queryLimit;

    public GetActivityStreamRequest() {
    }

    public void setId(String id) { this.id = id; }
    public void setQueryOffset(Integer queryOffset) { this.queryOffset = queryOffset; }
    public void setQueryLimit(Integer queryLimit) { this.queryLimit = queryLimit; }
    public String getId() { return id; }
    public Integer getQueryOffset() { return queryOffset; }
    public Integer getQueryLimit() { return queryLimit; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("queryOffset", queryOffset)
            .add("queryLimit", queryLimit);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
