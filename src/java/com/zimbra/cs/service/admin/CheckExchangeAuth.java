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
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.ldap.Check;
import com.zimbra.cs.fb.ExchangeFreeBusyProvider;
import com.zimbra.cs.fb.ExchangeFreeBusyProvider.AuthScheme;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class CheckExchangeAuth extends AdminDocumentHandler {

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        Account authedAcct = getAuthenticatedAccount(zsc);
        Domain domain = Provisioning.getInstance().getDomain(authedAcct);
        
        checkRight(zsc, context, domain, Admin.R_checkExchangeAuthConfig);
        Element auth = request.getElement(AdminConstants.E_AUTH);
        ExchangeFreeBusyProvider.ServerInfo sinfo = new ExchangeFreeBusyProvider.ServerInfo();
        sinfo.url = auth.getAttribute(AdminConstants.A_URL);
        sinfo.authUsername = auth.getAttribute(AdminConstants.A_USER);
        sinfo.authPassword = auth.getAttribute(AdminConstants.A_PASS);
        String scheme = auth.getAttribute(AdminConstants.A_SCHEME);
        sinfo.scheme = AuthScheme.valueOf(scheme);
        String type = auth.getAttribute(AdminConstants.A_TYPE, ExchangeFreeBusyProvider.TYPE_WEBDAV);
        Provisioning.Result r;
        if (ExchangeFreeBusyProvider.TYPE_WEBDAV.equals(type)) {
            r = Check.checkExchangeAuth(sinfo, authedAcct);
        } else {
            r = Check.checkExchangeEWSAuth(sinfo, authedAcct);
        }

	    Element response = zsc.createElement(AdminConstants.CHECK_EXCHANGE_AUTH_RESPONSE);
        response.addElement(AdminConstants.E_CODE).addText(r.getCode());
        String message = r.getMessage();
        if (message != null)
            response.addElement(AdminConstants.E_MESSAGE).addText(message);
	    return response;
	}
	
	@Override
	public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_checkExchangeAuthConfig);
    }
}