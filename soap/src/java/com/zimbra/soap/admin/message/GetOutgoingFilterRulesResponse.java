/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Zimbra, Inc.
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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.CosSelector;
import com.zimbra.soap.admin.type.DomainSelector;
import com.zimbra.soap.admin.type.ServerSelector;
import com.zimbra.soap.type.AccountSelector;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_OUTGOING_FILTER_RULES_RESPONSE)
public final class GetOutgoingFilterRulesResponse extends GetFilterRulesResponse {
    public GetOutgoingFilterRulesResponse() {
        super();
    }

    public GetOutgoingFilterRulesResponse(String type) {
        super(type);
    }

    public GetOutgoingFilterRulesResponse(String type, AccountSelector accountSelector) {
        super(type, accountSelector);
    }

    public GetOutgoingFilterRulesResponse(String type, DomainSelector domainSelector) {
        super(type, domainSelector);
    }

    public GetOutgoingFilterRulesResponse(String type, CosSelector cosSelector) {
        super(type, cosSelector);
    }

    public GetOutgoingFilterRulesResponse(String type, ServerSelector serverSelector) {
        super(type, serverSelector);
    }
}
