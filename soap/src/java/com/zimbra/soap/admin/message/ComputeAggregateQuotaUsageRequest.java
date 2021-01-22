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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
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

    /**
     * @zm-api-field-description If <b>1 (true)</b> then return single value containing quota usage across all domains.
     */
    @XmlAttribute(name=AdminConstants.A_SINGLE_RESULT /* dynamic */, required=false)
    private ZmBoolean singleResult;


    public ComputeAggregateQuotaUsageRequest() {
    }

}
