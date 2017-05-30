/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.CosSelector;
import com.zimbra.soap.admin.type.DomainSelector;
import com.zimbra.soap.admin.type.ServerSelector;
import com.zimbra.soap.type.AccountSelector;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Get filter rules
 */
@XmlRootElement(name=AdminConstants.E_GET_OUTGOING_FILTER_RULES_REQUEST)
public class GetOutgoingFilterRulesRequest extends GetFilterRulesRequest{
    public GetOutgoingFilterRulesRequest() {
        super();
    }

    public GetOutgoingFilterRulesRequest(AccountSelector accountSelector, String type) {
        super(accountSelector, type);
    }

    public GetOutgoingFilterRulesRequest(DomainSelector domainSelector, String type) {
        super(domainSelector, type);
    }

    public GetOutgoingFilterRulesRequest(CosSelector cosSelector, String type) {
        super(cosSelector, type);
    }

    public GetOutgoingFilterRulesRequest(ServerSelector serverSelector, String type) {
        super(serverSelector, type);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("GetOutgoingFilterRulesRequest ");
        sb.append(super.getToStringData());
        return sb.toString();
    }
}