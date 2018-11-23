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
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.filter.RuleManager.FilterType;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.ModifyOutgoingFilterRulesRequest;
import com.zimbra.soap.admin.message.ModifyOutgoingFilterRulesResponse;

public final class ModifyOutgoingFilterRules extends ModifyFilterRules {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        ModifyOutgoingFilterRulesRequest req = JaxbUtil.elementToJaxb(request);

        setAFTypeAndSelectors(req);

        if(acctSel != null) {
            entry = verifyAccountHarvestingAndPerms(acctSel, zsc);
        } else if(domainSelector != null) {
            entry = verifyDomainPerms(domainSelector, zsc);
        } else if(cosSelector != null) {
            entry = verifyCosPerms(cosSelector, zsc);
        } else if(serverSelector != null) {
            entry = verifyServerPerms(serverSelector, zsc);
        } else {
            // one of the selector must be present
            throw ServiceException.INVALID_REQUEST("Selector not provided.", null);
        }

        RuleManager.setAdminRulesFromXML(entry, req.getFilterRules(), FilterType.OUTGOING, afType);
        if (entry instanceof Account) {
            ZimbraLog.filter.debug("ModifyOutgoingFilterRules(Admin).handle going to execute clearConfigCacheOnAllServers with accountId: %s", ((Account)entry).getId());
            clearConfigCacheOnAllServers(((Account)entry).getId());	
        }
        return zsc.jaxbToElement(new ModifyOutgoingFilterRulesResponse());
    }

}
