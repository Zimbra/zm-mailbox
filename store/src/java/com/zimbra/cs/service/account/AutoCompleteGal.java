/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016, 2020 Synacor, Inc.
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
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.gal.GalSearchControl;
import com.zimbra.cs.gal.GalSearchParams;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.type.GalSearchType;

/**
 * @since May 26, 2004
 * @author schemers
 */
public class AutoCompleteGal extends GalDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(getZimbraSoapContext(context));

        if (!canAccessAccount(zsc, account))
            throw ServiceException.PERM_DENIED("can not access account");
        
        String name = request.getAttribute(AccountConstants.E_NAME);
        String typeStr = request.getAttribute(AccountConstants.A_TYPE, "account");
        GalSearchType type = GalSearchType.fromString(typeStr);

        boolean needCanExpand = request.getAttributeBool(AccountConstants.A_NEED_EXP, false);

        String galAcctId = request.getAttribute(AccountConstants.A_GAL_ACCOUNT_ID, null);
        
        GalSearchParams params = new GalSearchParams(account, zsc);
        params.setType(type);
        params.setRequest(request);
        params.setQuery(name);
        params.setLimit(account.getContactAutoCompleteMaxResults());
        params.setNeedCanExpand(needCanExpand);
        params.setResponseName(AccountConstants.AUTO_COMPLETE_GAL_RESPONSE);
        if (galAcctId != null) {
            Account galAccount = Provisioning.getInstance().getAccountById(galAcctId);
            if (galAccount != null && (!account.getDomainId().equals(galAccount.getDomainId()))) {
                throw ServiceException
                    .PERM_DENIED("can not access galsync account of different domain");
            }
            params.setGalSyncAccount(galAccount);
        }
        GalSearchControl gal = new GalSearchControl(params);
        gal.autocomplete();
        return params.getResultCallback().getResponse();
    }

    @Override
    public boolean needsAuth(Map<String, Object> context) {
        return true;
    }
}
