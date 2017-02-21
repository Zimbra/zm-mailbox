/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.ACLUtil;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.ZimbraACE;
import com.zimbra.soap.ZimbraSoapContext;

public class GetRights extends AccountDocumentHandler {
    
    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);

        if (!canAccessAccount(zsc, account)) {
            throw ServiceException.PERM_DENIED("can not access account");
        }
        
        Set<Right> specificRights = null;
        for (Element eACE : request.listElements(AccountConstants.E_ACE)) {
            if (specificRights == null)
                specificRights = new HashSet<Right>();
            specificRights.add(RightManager.getInstance().getUserRight(eACE.getAttribute(AccountConstants.A_RIGHT)));
        }
        
        List<ZimbraACE> aces = (specificRights==null)?ACLUtil.getAllACEs(account) : ACLUtil.getACEs(account, specificRights);
        Element response = zsc.createElement(AccountConstants.GET_RIGHTS_RESPONSE);
        if (aces != null) {
            for (ZimbraACE ace : aces) {
                ToXML.encodeACE(response, ace);
            }
        }
        return response;
    }
}
