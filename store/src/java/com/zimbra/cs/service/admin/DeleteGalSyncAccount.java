/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.ZimbraSoapContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class DeleteGalSyncAccount extends AdminDocumentHandler {

    public boolean domainAuthSufficient(Map<String, Object> context) {
        return true;
    }

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
	    Provisioning prov = Provisioning.getInstance();

	    Element acctElem = request.getElement(AdminConstants.E_ACCOUNT);
	    String acctKey = acctElem.getAttribute(AdminConstants.A_BY);
        String acctValue = acctElem.getText();

        Account account = prov.get(AccountBy.fromString(acctKey), acctValue);
		if (account == null)
			throw AccountServiceException.NO_SUCH_ACCOUNT(acctValue);
		
		checkAccountRight(zsc, account, Admin.R_deleteAccount); 

		String id = account.getId();
		if (!Provisioning.onLocalServer(account)) {
		    String affinityIp = Provisioning.affinityServerForZimbraId(id);       
		    return proxyRequest(request, context, affinityIp, zsc);
		}
		
		HashSet<String> acctIds = new HashSet<String>();
		Domain domain = prov.getDomain(account);
		Collections.addAll(acctIds, domain.getGalAccountId());
		if (!acctIds.contains(id))
			throw AccountServiceException.NO_SUCH_ACCOUNT(id);
		acctIds.remove(id);
		HashMap<String,Object> attrs = new HashMap<String,Object>();
		attrs.put(Provisioning.A_zimbraGalAccountId, acctIds);
		prov.modifyAttrs(domain, attrs);
		prov.deleteAccount(id);
		
        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "DeleteGalSyncAccount", "id", id} ));         

	    Element response = zsc.createElement(AdminConstants.DELETE_GAL_SYNC_ACCOUNT_RESPONSE);
	    return response;
	}
	
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_deleteAccount);
    }
}