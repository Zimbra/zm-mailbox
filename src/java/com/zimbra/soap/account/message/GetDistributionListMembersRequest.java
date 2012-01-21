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

package com.zimbra.soap.account.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;

/**
 * @zm-api-command-description Get the list of members of a distribution list.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_GET_DISTRIBUTION_LIST_MEMBERS_REQUEST)
public class GetDistributionListMembersRequest {

    /**
     * @zm-api-field-description The number of members to return (0 is default and means all)
     */
    @XmlAttribute(name=AdminConstants.A_LIMIT, required=false)
    private final Integer limit;

    /**
     * @zm-api-field-description The starting offset (0, 25, etc)
     */
    @XmlAttribute(name=AdminConstants.A_OFFSET, required=false)
    private final Integer offset;

    /**
     * @zm-api-field-tag dl-name
     * @zm-api-field-description The name of the distribution list
     */
    @XmlElement(name=AdminConstants.E_DL, required=true)
    private final String dl;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetDistributionListMembersRequest() {
        this((Integer) null, (Integer) null, (String) null);
    }

    public GetDistributionListMembersRequest(Integer limit, Integer offset,
                            String dl) {
        this.limit = limit;
        this.offset = offset;
        this.dl = dl;
    }

    public Integer getLimit() { return limit; }
    public Integer getOffset() { return offset; }
    public String getDl() { return dl; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("limit", limit)
            .add("offset", offset)
            .add("dl", dl)
            .toString();
    }
}
