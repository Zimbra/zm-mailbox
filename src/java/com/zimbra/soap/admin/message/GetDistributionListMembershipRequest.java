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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.DistributionListSelector;

/**
 * @zm-api-command-description Request a list of DLs that a particular DL is a member of
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_DISTRIBUTION_LIST_MEMBERSHIP_REQUEST)
public class GetDistributionListMembershipRequest {

    /**
     * @zm-api-field-tag limit
     * @zm-api-field-description The maximum number of DLs to return (0 is default and means all)
     */
    @XmlAttribute(name=AdminConstants.A_LIMIT, required=false)
    private final Integer limit;

    /**
     * @zm-api-field-tag starting-offset
     * @zm-api-field-description The starting offset (0, 25 etc)
     */
    @XmlAttribute(name=AdminConstants.A_OFFSET, required=false)
    private final Integer offset;

    /**
     * @zm-api-field-description Distribution List
     */
    @XmlElement(name=AdminConstants.E_DL, required=false)
    private final DistributionListSelector dl;

    /**
     * no-argument constructor wanted by JAXB
     */
     @SuppressWarnings("unused")
    private GetDistributionListMembershipRequest() {
        this((DistributionListSelector) null,
            (Integer) null, (Integer) null);
    }

    public GetDistributionListMembershipRequest(DistributionListSelector dl,
            Integer limit, Integer offset) {
        this.dl = dl;
        this.limit = limit;
        this.offset = offset;
    }

    public DistributionListSelector getDl() { return dl; }
    public Integer getLimit() { return limit; }
    public Integer getOffset() { return offset; }
}
