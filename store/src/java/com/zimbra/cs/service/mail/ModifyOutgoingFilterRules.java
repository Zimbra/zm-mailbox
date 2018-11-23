/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.ModifyOutgoingFilterRulesRequest;
import com.zimbra.soap.mail.message.ModifyOutgoingFilterRulesResponse;

public final class ModifyOutgoingFilterRules extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);

        if (!canModifyOptions(zsc, account)) {
            throw ServiceException.PERM_DENIED("cannot modify options");
        }

        ModifyOutgoingFilterRulesRequest req = zsc.elementToJaxb(request);
        RuleManager.setOutgoingXMLRules(account, req.getFilterRules());
        ZimbraLog.filter.debug("ModifyOutgoingFilterRules.handle going to execute clearConfigCacheOnAllServers with accountId: %s", account.getId());
        clearConfigCacheOnAllServers(account.getId());
        return zsc.jaxbToElement(new ModifyOutgoingFilterRulesResponse());
    }
}
