/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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

/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.QName;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.service.account.ToXML;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class GetAllAccounts extends AdminDocumentHandler {

    public static final String BY_NAME = "name";
    public static final String BY_ID = "id";
    
    /**
     * must be careful and only allow access to domain if domain admin
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }
    
	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
	    
        ZimbraSoapContext lc = getZimbraSoapContext(context);
	    Provisioning prov = Provisioning.getInstance();
	    
        Element response = null;

        Element s = request.getOptionalElement(AdminConstants.E_SERVER);
        Server server = null;
        if (s != null) {
            String key = s.getAttribute(AdminConstants.A_BY);
            String value = s.getText();
            if (key.equals(BY_NAME)) 
                server = prov.get(ServerBy.name, value);
            else if (key.equals(BY_ID))
                server = prov.get(ServerBy.id, value);
            else
                throw ServiceException.INVALID_REQUEST("unknown value for server by: "+key, null);
                
            if (server == null)
                throw AccountServiceException.NO_SUCH_SERVER(value);
        }
        
        Element d = request.getOptionalElement(AdminConstants.E_DOMAIN);
        if (d != null || isDomainAdminOnly(lc)) {
            
            String key = d == null ? BY_NAME : d.getAttribute(AdminConstants.A_BY);
            String value = d == null ? getAuthTokenAccountDomain(lc).getName() : d.getText();
	    
            Domain domain = null;
        
            if (key.equals(BY_NAME)) {
                domain = prov.get(DomainBy.name, value);
            } else if (key.equals(BY_ID)) {
                domain = prov.get(DomainBy.id, value);
            } else {
                throw ServiceException.INVALID_REQUEST("unknown value for domain by: "+key, null);
            }
	    
            if (domain == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(value);
            
            if (!canAccessDomain(lc, domain)) 
                throw ServiceException.PERM_DENIED("can not access domain"); 

            response = lc.createElement(getResponseQName());
            doDomain(response, domain, server);

        } else {
            response = lc.createElement(getResponseQName());
            List domains = prov.getAllDomains();
            if (domains != null) {
	            for (Iterator dit=domains.iterator(); dit.hasNext(); ) {
	                Domain domain = (Domain) dit.next();
	                doDomain(response, domain, server);                
	            }
            } else { //domains not supported, for now only offline
            	doDomain(response, null, server);
            }
        }
        return response;        
	}

    protected QName getResponseQName() {
        return AdminConstants.GET_ALL_ACCOUNTS_RESPONSE;
    }

    protected void doDomain(final Element e, Domain d, Server s) throws ServiceException {
        NamedEntry.Visitor visitor = new NamedEntry.Visitor() {
            public void visit(com.zimbra.cs.account.NamedEntry entry) {
                ToXML.encodeAccountOld(e, (Account) entry);
            }
        };
        Provisioning.getInstance().getAllAccounts(d, s, visitor);
    }
}
