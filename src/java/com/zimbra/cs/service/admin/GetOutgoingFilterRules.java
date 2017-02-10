/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.filter.RuleManager.FilterType;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.GetOutgoingFilterRulesRequest;
import com.zimbra.soap.admin.message.GetOutgoingFilterRulesResponse;
import com.zimbra.soap.admin.type.CosSelector;
import com.zimbra.soap.admin.type.DomainSelector;
import com.zimbra.soap.admin.type.ServerSelector;
import com.zimbra.soap.mail.type.FilterRule;
import com.zimbra.soap.type.AccountSelector;

public final class GetOutgoingFilterRules extends AdminDocumentHandler {

    @Override
    public Element handle(Element req, Map<String, Object> context) throws ServiceException {
        Account account = null;
        Domain domain = null;
        Cos cos = null;
        Server server = null;
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        GetOutgoingFilterRulesRequest request = JaxbUtil.elementToJaxb(req);

        String type = request.getType();
        AccountSelector acctSel = request.getAccount();
        if(acctSel != null) {
            account = verifyAccountHarvestingAndPerms(acctSel, zsc);
        }
        DomainSelector domainSelector = request.getDomain();
        if(domainSelector != null) {
            domain = verifyDomainPerms(domainSelector, zsc);
        }
        CosSelector cosSelector = request.getCos();
        if(cosSelector != null) {
            cos = verifyCosPerms(cosSelector, zsc);
        }
        ServerSelector serverSelector = request.getServer();
        if(serverSelector != null) {
            server = verifyServerPerms(serverSelector, zsc);
        }

        GetOutgoingFilterRulesResponse resp = new GetOutgoingFilterRulesResponse(type);
        List<FilterRule> rules = null;
        if(account != null) {
            rules = RuleManager.getAccountAdminRulesAsXML(account, FilterType.OUTGOING, type);
        } else if(domain != null) {
            rules = RuleManager.getDomainAdminRulesAsXML(domain, FilterType.OUTGOING, type);
        } else if(cos != null) {
            rules = RuleManager.getCosAdminRulesAsXML(cos, FilterType.OUTGOING, type);
        } else if(server != null) {
            rules = RuleManager.getServerAdminRulesAsXML(server, FilterType.OUTGOING, type);
        }
        resp.addFilterRules(rules);
        return zsc.jaxbToElement(resp);
    }

}
