/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.account;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.cs.zimlet.ZimletUserProperties;

/**
 * @author jylee
 */
public class ModifyProperties extends AccountDocumentHandler {

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
		ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);
        if (!canModifyOptions(zsc, account)) {
            throw ServiceException.PERM_DENIED("can not modify options");
        }
        ZimletUserProperties props = ZimletUserProperties.getProperties(account);
        int numUserproperties = request.listElements(AccountConstants.E_PROPERTY).size();
        if (numUserproperties > account.getLongAttr(Provisioning.A_zimbraZimletUserPropertiesMaxNumEntries, 150)) {
            throw AccountServiceException.TOO_MANY_ZIMLETUSERPROPERTIES();
        }
        for (Element e : request.listElements(AccountConstants.E_PROPERTY)) {
            props.setProperty(e.getAttribute(AccountConstants.A_ZIMLET),
                    e.getAttribute(AccountConstants.A_NAME),
                    e.getText());
            }
        props.saveProperties(account);
        Element response = zsc.createElement(AccountConstants.MODIFY_PROPERTIES_RESPONSE);
        return response;
	}
}
