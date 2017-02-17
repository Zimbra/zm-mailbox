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

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.fb.ExchangeEWSFreeBusyProvider;
import com.zimbra.cs.fb.FreeBusyProvider;
import com.zimbra.soap.ZimbraSoapContext;

public class GetAllFreeBusyProviders extends AdminDocumentHandler {
	public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        
        // allow only system admin for now
        checkRight(zsc, context, null, Admin.R_getAllFreeBusyProviders);
        
        Element response = zsc.createElement(AdminConstants.GET_ALL_FREE_BUSY_PROVIDERS_RESPONSE);
        
        for (FreeBusyProvider prov : FreeBusyProvider.getProviders()) {
            if (!(prov instanceof ExchangeEWSFreeBusyProvider )) {
                Element provElem = response.addElement(AdminConstants.E_PROVIDER);
                provElem.addAttribute(AdminConstants.A_NAME, prov.getName());
                provElem.addAttribute(AdminConstants.A_PROPAGATE, prov.registerForMailboxChanges());
                provElem.addAttribute(AdminConstants.A_START, prov.cachedFreeBusyStartTime());
                provElem.addAttribute(AdminConstants.A_END, prov.cachedFreeBusyEndTime());
                provElem.addAttribute(AdminConstants.A_QUEUE, prov.getQueueFilename());
                provElem.addAttribute(AdminConstants.A_PREFIX, prov.foreignPrincipalPrefix());
            }
        }
	    return response;
	}
	
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_getAllFreeBusyProviders);
    }
}
