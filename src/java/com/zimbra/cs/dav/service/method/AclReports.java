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
package com.zimbra.cs.dav.service.method;

import javax.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;

import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.SearchGalResult;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.property.Acl.Ace;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.dav.resource.MailItemResource;
import com.zimbra.cs.dav.resource.UrlNamespace;
import com.zimbra.cs.dav.service.DavResponse;
import com.zimbra.cs.mailbox.Contact;

public class AclReports extends Report {
	public void handle(DavContext ctxt) throws DavException, ServiceException {
		Element query = ctxt.getRequestMessage().getRootElement();
		if (query.getQName().equals(DavElements.E_PRINCIPAL_PROPERTY_SEARCH))
			handlePrincipalPropertySearch(ctxt, query);
		else if (query.getQName().equals(DavElements.E_ACL_PRINCIPAL_PROP_SET))
			handleAclPrincipalPropSet(ctxt, query);
		else
			throw new DavException("msg "+query.getName()+" is not an ACL report", HttpServletResponse.SC_BAD_REQUEST, null);
	}
	
	private void handlePrincipalPropertySearch(DavContext ctxt, Element query) throws DavException, ServiceException {
		RequestProp reqProp = getRequestProp(ctxt);
		DavResponse resp = ctxt.getDavResponse();
		for (DavResource rs : getMatchingResources(ctxt, query))
            resp.addResource(ctxt, rs, reqProp, false);
	}
	
	private ArrayList<DavResource> getMatchingResources(DavContext ctxt, Element query) throws DavException, ServiceException {
		// needs to be /principals/users, or apply-to-principal-collection-set is set.
		ArrayList<DavResource> ret = new ArrayList<DavResource>();
		boolean applyToPrincipalCollection = query.element(DavElements.E_APPLY_TO_PRINCIPAL_COLLECTION_SET) != null;
		String path = ctxt.getUri();
		if (!applyToPrincipalCollection && !path.startsWith(UrlNamespace.PRINCIPALS_PATH))
			return ret;
		
		List propSearch = query.elements(DavElements.E_PROPERTY_SEARCH);
		for (Object obj : propSearch) {
			if (!(obj instanceof Element))
				continue;
			Element ps = (Element) obj;
			Element prop = ps.element(DavElements.E_PROP);
			Element match = ps.element(DavElements.E_MATCH);
			if (prop != null && match != null) {
				Element e = (Element)prop.elements().get(0);
				ret.addAll(getMatchingResources(ctxt.getAuthAccount(), e.getQName(), match.getText()));
			}
		}
		
		return ret;
	}
	
	private ArrayList<DavResource> getMatchingResources(Account authAccount, QName prop, String match) throws DavException, ServiceException {
		Provisioning prov = Provisioning.getInstance();
		ArrayList<DavResource> ret = new ArrayList<DavResource>();
		if (prop.equals(DavElements.E_DISPLAYNAME)) {
	        SearchGalResult result = prov.searchGal(prov.getDomain(authAccount), match, Provisioning.GAL_SEARCH_TYPE.USER_ACCOUNT, null);
	        for (GalContact ct : result.matches) {
	            String email = (String)ct.getAttrs().get(Contact.A_email);
	            if (email != null) {
	            	Account acct = prov.get(Provisioning.AccountBy.name, email);
	            	if (acct != null)
	            		ret.add(UrlNamespace.getPrincipal(acct));
	            }
	        }
		} else if (prop.equals(DavElements.E_CALENDAR_HOME_SET)) {
			int index = match.lastIndexOf('/');
			if (index > 0)
				match = match.substring(index+1);
			Account acct = prov.get(Provisioning.AccountBy.name, match);
			if (acct != null)
        		ret.add(UrlNamespace.getPrincipal(acct));
		}
		return ret;
	}
	
	private void handleAclPrincipalPropSet(DavContext ctxt, Element query) throws DavException, ServiceException {
		RequestProp reqProp = getRequestProp(ctxt);
		DavResponse resp = ctxt.getDavResponse();
		for (DavResource rs : getAclPrincipals(ctxt))
            resp.addResource(ctxt, rs, reqProp, false);
	}
	
	private ArrayList<DavResource> getAclPrincipals(DavContext ctxt) throws DavException, ServiceException {
		ArrayList<DavResource> ret = new ArrayList<DavResource>();
		DavResource res = ctxt.getRequestedResource();
		if (!(res instanceof MailItemResource))
			return ret;
		List<Ace> aces = ((MailItemResource)res).getAce(ctxt);
		Provisioning prov = Provisioning.getInstance();
		for (Ace ace : aces) {
			if (ace.hasHref()) {
				Account acct = prov.get(Provisioning.AccountBy.id, ace.getZimbraId());
				if (acct != null)
					ret.add(UrlNamespace.getPrincipal(acct));
			}
		}
		return ret;
	}
}
