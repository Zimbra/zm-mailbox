/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.admin;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.listeners.DomainListener;
import com.zimbra.soap.ZimbraSoapContext;

import java.util.List;
import java.util.Map;

/**
 * @author schemers
 */
public class DeleteDomain extends AdminDocumentHandler {

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        String id = request.getAttribute(AdminConstants.E_ID);

        Domain domain = prov.get(Key.DomainBy.id, id);
        if (domain == null)
            throw AccountServiceException.NO_SUCH_DOMAIN(id);
        
        if (domain.isShutdown())
            throw ServiceException.PERM_DENIED("can not access domain, domain is in " + domain.getDomainStatusAsString() + " status");
        
        checkRight(zsc, context, domain, Admin.R_deleteDomain);
        
        String name = domain.getName();
        
        prov.deleteDomain(id);

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(new String[] {"cmd", "DeleteDomain","name", name, "id", id }));

	    Element response = zsc.createElement(AdminConstants.DELETE_DOMAIN_RESPONSE);

        DomainListener.invokeOnDeleteDomain(domain);

	    return response;
	}
	
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_deleteDomain);
    }
}