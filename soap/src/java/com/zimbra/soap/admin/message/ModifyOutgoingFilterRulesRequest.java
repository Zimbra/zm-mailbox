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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.CosSelector;
import com.zimbra.soap.admin.type.DomainSelector;
import com.zimbra.soap.admin.type.ServerSelector;
import com.zimbra.soap.mail.type.FilterRule;
import com.zimbra.soap.type.AccountSelector;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Modify Filter rules
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_MODIFY_OUTGOING_FILTER_RULES_REQUEST)
public final class ModifyOutgoingFilterRulesRequest extends ModifyFilterRulesRequest {
    public ModifyOutgoingFilterRulesRequest() {
        super();
    }

    public ModifyOutgoingFilterRulesRequest(AccountSelector account, List<FilterRule> filterRules, String type) {
        super(account, filterRules, type);
    }

    public ModifyOutgoingFilterRulesRequest(DomainSelector domain, List<FilterRule> filterRules, String type) {
        super(domain, filterRules, type);
    }

    public ModifyOutgoingFilterRulesRequest(CosSelector cos, List<FilterRule> filterRules, String type) {
        super(cos, filterRules, type);
    }

    public ModifyOutgoingFilterRulesRequest(ServerSelector server, List<FilterRule> filterRules, String type) {
        super(server, filterRules, type);
    }
}
