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

import com.zimbra.common.soap.AdminConstants;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @zm-api-command-description Computes the aggregate quota usage for all domains in the system.
 * <br />
 * The request handler issues <b>GetAggregateQuotaUsageOnServerRequest</b> to all mailbox servers and computes the
 * aggregate quota used by each domain.
 * <br />
 * The request handler updates the <b>zimbraAggregateQuotaLastUsage</b> domain attribute and sends out warning
 * messages for each domain having quota usage greater than a defined percentage threshold.
 */
@XmlRootElement(name=AdminConstants.E_COMPUTE_AGGR_QUOTA_USAGE_REQUEST)
public class ComputeAggregateQuotaUsageRequest {

    public ComputeAggregateQuotaUsageRequest() {
    }
}
