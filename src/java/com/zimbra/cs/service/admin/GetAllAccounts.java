/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
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

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.account.ToXML;
import com.zimbra.soap.Element;
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

        Element d = request.getOptionalElement(AdminService.E_DOMAIN);
        if (d != null || isDomainAdminOnly(lc)) {
            
            String key = d == null ? BY_NAME : d.getAttribute(AdminService.A_BY);
            String value = d == null ? getAuthTokenAccountDomain(lc).getName() : d.getText();
	    
            Domain domain = null;
        
            if (key.equals(BY_NAME)) {
                domain = prov.get(DomainBy.NAME, value);
            } else if (key.equals(BY_ID)) {
                domain = prov.get(DomainBy.ID, value);
            } else {
                throw ServiceException.INVALID_REQUEST("unknown value for by: "+key, null);
            }
	    
            if (domain == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(value);
            
            if (!canAccessDomain(lc, domain)) 
                throw ServiceException.PERM_DENIED("can not access domain"); 

            response = lc.createElement(getResponseQName());
            doDomain(response, domain);

        } else {
            response = lc.createElement(getResponseQName());
            List domains = prov.getAllDomains();
            for (Iterator dit=domains.iterator(); dit.hasNext(); ) {
                Domain domain = (Domain) dit.next();
                doDomain(response, domain);                
            }
        }
        return response;        
	}

    protected QName getResponseQName() {
        return AdminService.GET_ALL_ACCOUNTS_RESPONSE;
    }

    protected void doDomain(final Element e, Domain d) throws ServiceException {
        NamedEntry.Visitor visitor = new NamedEntry.Visitor() {
            public void visit(com.zimbra.cs.account.NamedEntry entry) throws ServiceException {
                ToXML.encodeAccount(e, (Account) entry);
            }
        };
        Provisioning.getInstance().getAllAccounts(d, visitor);
    }
}
