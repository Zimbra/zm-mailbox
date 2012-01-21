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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.account.type.DistributionListSubscribeStatus;

@XmlRootElement(name=AccountConstants.E_SUBSCRIBE_DISTRIBUTION_LIST_RESPONSE)
public class SubscribeDistributionListResponse {

    /**
     * @zm-api-field-description Status of subscription attempt
     */
    @XmlAttribute(name=AccountConstants.A_STATUS, required=true)
    private DistributionListSubscribeStatus status;

    public SubscribeDistributionListResponse() {
        this((DistributionListSubscribeStatus) null);
    }
    public SubscribeDistributionListResponse(DistributionListSubscribeStatus status) {
        this.status = status;
    }

    public DistributionListSubscribeStatus getStatus() { return status; }
}
