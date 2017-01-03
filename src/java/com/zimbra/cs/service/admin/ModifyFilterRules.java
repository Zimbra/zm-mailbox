/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.ModifyFilterRulesRequest;
import com.zimbra.soap.admin.message.ModifyFilterRulesResponse;
import com.zimbra.soap.type.AccountSelector;

public final class ModifyFilterRules extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        ModifyFilterRulesRequest req = JaxbUtil.elementToJaxb(request);

        AccountSelector acctSel = req.getAccount();
        if (acctSel == null) {
            ServiceException se = ServiceException.INVALID_REQUEST(String.format("missing <%s>", AdminConstants.E_ACCOUNT), null);
            ZimbraLog.filter.debug("AccountSelector not found", se);
            throw se;
        }
        String accountSelectorKey = acctSel.getKey();
        AccountBy by = acctSel.getBy().toKeyAccountBy();

        Account account = prov.get(by, accountSelectorKey, zsc.getAuthToken());
        verifyAccountHarvestingAndPerms(account, by, accountSelectorKey, zsc);

        ModifyFilterRulesRequest mfrReq = JaxbUtil.elementToJaxb(request);
        RuleManager.setIncomingXMLRules(account, mfrReq.getFilterRules());
        return zsc.jaxbToElement(new ModifyFilterRulesResponse());
    }

}
