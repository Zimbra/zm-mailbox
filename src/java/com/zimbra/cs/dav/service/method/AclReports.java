/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.service.method;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.SearchGalResult;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.DavContext.RequestProp;
import com.zimbra.cs.dav.property.Acl.Ace;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.dav.resource.MailItemResource;
import com.zimbra.cs.dav.resource.UrlNamespace;
import com.zimbra.cs.dav.service.DavResponse;

public class AclReports extends Report {
	public void handle(DavContext ctxt) throws DavException, ServiceException {
		ctxt.setStatus(DavProtocol.STATUS_MULTI_STATUS);
		Element query = ctxt.getRequestMessage().getRootElement();
		if (query.getQName().equals(DavElements.E_PRINCIPAL_PROPERTY_SEARCH))
			handlePrincipalPropertySearch(ctxt, query);
		else if (query.getQName().equals(DavElements.E_ACL_PRINCIPAL_PROP_SET))
			handleAclPrincipalPropSet(ctxt, query);
		else if (query.getQName().equals(DavElements.E_PRINCIPAL_MATCH))
			handlePrincipalMatch(ctxt, query);
		else if (query.getQName().equals(DavElements.E_PRINCIPAL_SEARCH_PROPERTY_SET))
			handlePrincipalSearchPropertySet(ctxt);
		else
			throw new DavException("msg "+query.getName()+" is not an ACL report", HttpServletResponse.SC_BAD_REQUEST);
	}
	
	private void handlePrincipalPropertySearch(DavContext ctxt, Element query) throws DavException, ServiceException {
		RequestProp reqProp = ctxt.getRequestProp();
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
		
		// apple hack to do user / resource search
		Provisioning.GalSearchType type = Provisioning.GalSearchType.all;
		String queryType = query.attributeValue("type");
		if (queryType != null) {
			if (queryType.compareToIgnoreCase("INDIVIDUAL") == 0)
				type = Provisioning.GalSearchType.account;
			else if (queryType.compareToIgnoreCase("RESOURCE") == 0)
				type = Provisioning.GalSearchType.resource;
		}
		List propSearch = query.elements(DavElements.E_PROPERTY_SEARCH);
		for (Object obj : propSearch) {
			if (!(obj instanceof Element))
				continue;
			Element ps = (Element) obj;
			Element prop = ps.element(DavElements.E_PROP);
			Element match = ps.element(DavElements.E_MATCH);
			if (prop != null && match != null) {
				Element e = (Element)prop.elements().get(0);
				ret.addAll(getMatchingPrincipals(ctxt, e.getQName(), match.getText(), type));
			}
		}
		
		return ret;
	}
	
	private ArrayList<DavResource> getMatchingPrincipals(DavContext ctxt, QName prop, String match, Provisioning.GalSearchType type) throws DavException, ServiceException {
		Provisioning prov = Provisioning.getInstance();
		ArrayList<DavResource> ret = new ArrayList<DavResource>();
		Account authAccount = ctxt.getAuthAccount();
		if (prop.equals(DavElements.E_DISPLAYNAME)) {
	        SearchGalResult result = prov.searchGal(prov.getDomain(authAccount), match, type, Provisioning.GalMode.zimbra, null);
	        for (GalContact ct : result.getMatches()) {
	            String email = (String)ct.getAttrs().get(ContactConstants.A_email);
	            if (email != null) {
	            	Account acct = prov.get(Provisioning.AccountBy.name, email);
	            	if (acct != null)
	            		ret.add(UrlNamespace.getPrincipal(ctxt, acct));
	            }
	        }
		} else if (prop.equals(DavElements.E_CALENDAR_HOME_SET)) {
			int index = match.lastIndexOf('/');
			if (index > 0)
				match = match.substring(index+1);
			Account acct = prov.get(Provisioning.AccountBy.name, match);
			if (acct != null)
        		ret.add(UrlNamespace.getPrincipal(ctxt, acct));
		}
		return ret;
	}
	
	private void handleAclPrincipalPropSet(DavContext ctxt, Element query) throws DavException, ServiceException {
		RequestProp reqProp = ctxt.getRequestProp();
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
					ret.add(UrlNamespace.getPrincipal(ctxt, acct));
			}
		}
		return ret;
	}
	
	private void handlePrincipalMatch(DavContext ctxt, Element query) throws DavException, ServiceException {
		if (ctxt.getDepth() != DavContext.Depth.zero)
			throw new DavException("non-zero depth", HttpServletResponse.SC_BAD_REQUEST);
		ArrayList<DavResource> ret = new ArrayList<DavResource>();
		RequestProp reqProp = ctxt.getRequestProp();
		DavResponse resp = ctxt.getDavResponse();
		Element principalProp = query.element(DavElements.E_PRINCIPAL_PROPERTY);
		if (principalProp == null) {
			// request must be to the principals path
			String path = ctxt.getUri();
			if (path.startsWith(UrlNamespace.PRINCIPALS_PATH))
				ret.add(UrlNamespace.getPrincipal(ctxt, ctxt.getAuthAccount()));
		} else {
			// we know of only <owner/> element
			Element owner = principalProp.element(DavElements.E_OWNER);
			if (owner != null) {
				// return the all the members of the collection.
				DavResource rs = ctxt.getRequestedResource();
				if (rs.isCollection())
					ret.addAll(rs.getChildren(ctxt));
			}
		}
		for (DavResource rs : ret)
            resp.addResource(ctxt, rs, reqProp, false);
	}
	
	private static final ArrayList<Pair<QName,Element>> PRINCIPAL_SEARCH_PROPERTIES;
	
	static {
		PRINCIPAL_SEARCH_PROPERTIES = new ArrayList<Pair<QName,Element>>();
		addSearchProperty(DavElements.E_DISPLAYNAME, "Full name");
		addSearchProperty(DavElements.E_EMAIL_ADDRESS_SET, "Email Address");
		addSearchProperty(DavElements.E_CALENDAR_USER_TYPE, "User type");
		addSearchProperty(DavElements.E_CALENDAR_USER_ADDRESS_SET, "Calendar user address");
		addSearchProperty(DavElements.E_CALENDAR_HOME_SET, "Calendar home");
	}

	private static void addSearchProperty(QName prop, String desc) {
		Element elem = DocumentHelper.createElement(DavElements.E_DESCRIPTION);
		elem.addAttribute(DavElements.E_LANG, DavElements.LANG_EN_US);
		elem.setText(desc);
		PRINCIPAL_SEARCH_PROPERTIES.add(new Pair<QName,Element>(prop, elem));
	}
	
	private void handlePrincipalSearchPropertySet(DavContext ctxt) throws DavException, ServiceException {
		if (ctxt.getDepth() != DavContext.Depth.zero)
			throw new DavException("non-zero depth", HttpServletResponse.SC_BAD_REQUEST);
		Element response = ctxt.getDavResponse().getTop(DavElements.E_PRINCIPAL_SEARCH_PROPERTY_SET);
		ctxt.setStatus(HttpServletResponse.SC_OK);
		for (Pair<QName,Element> prop : PRINCIPAL_SEARCH_PROPERTIES) {
			Element searchProp = response.addElement(DavElements.E_PRINCIPAL_SEARCH_PROPERTY);
			searchProp.addElement(DavElements.E_PROP).addElement(prop.getFirst());
			searchProp.add(prop.getSecond().createCopy());
		}
	}
}
