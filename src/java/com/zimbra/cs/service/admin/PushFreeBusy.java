/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.Iterator;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.fb.FreeBusyProvider;
import com.zimbra.soap.ZimbraSoapContext;

public class PushFreeBusy extends AdminDocumentHandler {
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
    	Provisioning prov = Provisioning.getInstance();
        
    	Element domainElem = request.getOptionalElement(AdminConstants.E_DOMAIN);
        if (domainElem == null) {
        	Iterator<Element> accounts = request.elementIterator(AdminConstants.E_ACCOUNT);
        	while (accounts.hasNext()) {
        		String accountId = accounts.next().getAttribute(AdminConstants.A_ID, null);
        		if (accountId == null)
        			continue;
        		Account acct = prov.get(Provisioning.AccountBy.id, accountId);
        		if (acct == null) {
        			ZimbraLog.misc.warn("invalid accountId: "+accountId);
        			continue;
        		}
        		if (!Provisioning.onLocalServer(acct)) {
        			ZimbraLog.misc.warn("account is not on this server: "+accountId);
        			continue;
        		}
            	FreeBusyProvider.mailboxChanged(accountId);
        	}
        } else {
        	String[] domains = domainElem.getAttribute(AdminConstants.A_NAME).split(",");
        	Server s = prov.getLocalServer();
    		NamedEntry.Visitor visitor = new NamedEntry.Visitor() {
    			public void visit(NamedEntry entry) {
    				if (entry instanceof Account) {
    					FreeBusyProvider.mailboxChanged(((Account)entry).getId());
    				}
    			}
    		};
        	for (String domain : domains) {
            	Domain d = prov.get(Provisioning.DomainBy.name, domain);
        		prov.getAllAccounts(d, s, visitor);
        	}
        }

        Element response = lc.createElement(AdminConstants.PUSH_FREE_BUSY_RESPONSE);
        return response;
    }
}