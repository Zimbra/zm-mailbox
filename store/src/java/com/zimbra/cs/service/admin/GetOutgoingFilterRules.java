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
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.filter.RuleManager.FilterType;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.admin.message.GetOutgoingFilterRulesRequest;
import com.zimbra.soap.admin.message.GetOutgoingFilterRulesResponse;

public final class GetOutgoingFilterRules extends GetFilterRules {

    @Override
    public Element handle(Element req, Map<String, Object> context) throws ServiceException {
        zsc = getZimbraSoapContext(context);
        GetOutgoingFilterRulesRequest request = JaxbUtil.elementToJaxb(req);

        setAFTypeSelectorsAndEntry(request, zsc);

        if(acctSel != null) {
            entry = verifyAccountHarvestingAndPerms(acctSel, zsc);
            resp = new GetOutgoingFilterRulesResponse(afType.getType(), acctSel);
        } else if(domainSelector != null) {
            entry = verifyDomainPerms(domainSelector, zsc);
            resp = new GetOutgoingFilterRulesResponse(afType.getType(), domainSelector);
        } else if(cosSelector != null) {
            entry = verifyCosPerms(cosSelector, zsc);
            resp = new GetOutgoingFilterRulesResponse(afType.getType(), cosSelector);
        } else if(serverSelector != null) {
            entry = verifyServerPerms(serverSelector, zsc);
            resp = new GetOutgoingFilterRulesResponse(afType.getType(), serverSelector);
        } else {
            // one of the selector must be present
            throw ServiceException.INVALID_REQUEST("Selector not provided.", null);
        }

        rules = RuleManager.getAdminRulesAsXML(entry, FilterType.OUTGOING, afType);
        resp.addFilterRules(rules);
        return zsc.jaxbToElement(resp);
    }

}
