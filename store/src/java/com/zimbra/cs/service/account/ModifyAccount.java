/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.account;

import java.util.List;
import java.util.Map;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.CacheEntryBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.CacheEntry;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.session.AdminSession;
import com.zimbra.cs.session.Session;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.message.ModifyAccountRequest;
import com.zimbra.soap.admin.type.CacheEntryType;//TODO: ??

/**
 * @author schemers
 */
public class ModifyAccount extends AccountDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getAuthenticatedAccount(zsc);
        Provisioning prov = Provisioning.getInstance();
        ModifyAccountRequest req = zsc.elementToJaxb(request);
        AuthToken authToken = zsc.getAuthToken();

        ZimbraLog.account.info("The account is: " + account);
        ZimbraLog.account.info("The authToken is: " + authToken);

        Map<String, Object> attrs = req.getAttrsAsOldMultimap();

        // pass in true to checkImmutable
        prov.modifyAttrs(account, attrs, true, authToken);

        ZimbraLog.security
                .info(ZimbraLog.encodeAttrs(new String[] { "cmd", "ModifyAccount", "name", account.getName() }, attrs));

        Element response = zsc.createElement(AccountConstants.MODIFY_ACCOUNT_RESPONSE);
        ToXML.encodeAccount(response, account);
        return response;
    }

    public static String getStringAttrNewValue(String attrName, Map<String, Object> attrs) throws ServiceException {
        Object object = attrs.get(attrName);
        if (object == null) {
            object = attrs.get("+" + attrName);
        }
        if (object == null) {
            object = attrs.get("-" + attrName);
        }
        if (object == null) {
            return null;
        }

        if (!(object instanceof String)) {
            throw ServiceException.PERM_DENIED("can not modify " + attrName + "(single valued attribute)");
        }
        return (String) object;
    }
}
