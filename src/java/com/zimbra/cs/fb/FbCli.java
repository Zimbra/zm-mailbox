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
package com.zimbra.cs.fb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.cs.httpclient.URLUtil;

public class FbCli {
	public static class FbProvider {
		public String name;
		public boolean propagate;
		public String queue;
		public String prefix;
		public long fbstart;
		public long fbend;
		public String toString() {
			StringBuilder buf = new StringBuilder();
			buf.append(name).append("\n");
			buf.append("\t").append("propagate: ").append(propagate).append("\n");
			buf.append("\t").append("queue:     ").append(queue).append("\n");
			buf.append("\t").append("prefix:    ").append(prefix).append("\n");
			buf.append("\t").append("start:     ").append(new Date(fbstart)).append("\n");
			buf.append("\t").append("end:       ").append(new Date(fbend)).append("\n");
			return buf.toString();
		}
	}
	public static class FbQueue {
		public String name;
		Collection<String> accounts;
		public FbQueue() {
			accounts = new ArrayList<String>();
		}
		public String toString() {
			StringBuilder buf = new StringBuilder();
			buf.append(name).append("\n");
			for (String acct : accounts) {
				buf.append("\t").append(acct).append("\n");
			}
			return buf.toString();
		}
	}
	public FbCli() throws ServiceException {
		String server = LC.zimbra_zmprov_default_soap_server.value();
		String adminUrl = URLUtil.getAdminURL(server);
		mTransport = new SoapHttpTransport(adminUrl);
	}

	public Collection<FbProvider> getAllFreeBusyProviders() throws ServiceException, IOException {
		ArrayList<FbProvider> providers = new ArrayList<FbProvider>();
		try {
			auth();
			XMLElement req = new XMLElement(AdminConstants.GET_ALL_FREE_BUSY_PROVIDERS_REQUEST);
			Element resp = mTransport.invoke(req);
			for (Element provElem : resp.listElements(AdminConstants.E_PROVIDER)) {
				FbProvider prov = new FbProvider();
				prov.name = provElem.getAttribute(AdminConstants.A_NAME, null);
				prov.propagate = provElem.getAttributeBool(AdminConstants.A_PROPAGATE, false);
				prov.fbstart = provElem.getAttributeLong(AdminConstants.A_START, 0);
				prov.fbend = provElem.getAttributeLong(AdminConstants.A_END, 0);
				prov.queue = provElem.getAttribute(AdminConstants.A_QUEUE, null);
				prov.prefix = provElem.getAttribute(AdminConstants.A_PREFIX, null);
				providers.add(prov);
			}
		} finally {
			mTransport.shutdown();
		}
		return providers;
	}
	
	public Collection<FbQueue> getFreeBusyQueueInfo(String provider) throws ServiceException, IOException {
		ArrayList<FbQueue> queues = new ArrayList<FbQueue>();
		try {
			auth();
			XMLElement req = new XMLElement(AdminConstants.GET_FREE_BUSY_QUEUE_INFO_REQUEST);
			if (provider != null)
				req.addElement(AdminConstants.E_PROVIDER).addAttribute(AdminConstants.A_NAME, provider);
			Element resp = mTransport.invoke(req);
			for (Element provElem : resp.listElements(AdminConstants.E_PROVIDER)) {
				FbQueue queue = new FbQueue();
				queue.name = provElem.getAttribute(AdminConstants.A_NAME, null);
				for (Element acctElem : provElem.listElements(AdminConstants.E_ACCOUNT)) {
					queue.accounts.add(acctElem.getAttribute(AdminConstants.A_ID, null));
				}
				queues.add(queue);
			}
		} finally {
			mTransport.shutdown();
		}
		return queues;
	}
	
	public void pushFreeBusyForDomain(String domain) throws ServiceException, IOException {
		try {
			auth();
			XMLElement req = new XMLElement(AdminConstants.PUSH_FREE_BUSY_REQUEST);
			req.addElement(AdminConstants.E_DOMAIN).addAttribute(AdminConstants.A_NAME, domain);
			mTransport.invoke(req);
		} finally {
			mTransport.shutdown();
		}
	}
	
	public void pushFreeBusyForAccounts(Collection<String> accounts) throws ServiceException, IOException {
		try {
			auth();
			XMLElement req = new XMLElement(AdminConstants.PUSH_FREE_BUSY_REQUEST);
			for (String acct : accounts)
				req.addElement(AdminConstants.E_ACCOUNT).addAttribute(AdminConstants.A_ID, acct);
			mTransport.invoke(req);
		} finally {
			mTransport.shutdown();
		}
	}
	
	private void auth() throws ServiceException, IOException {
		XMLElement req = new XMLElement(AdminConstants.AUTH_REQUEST);
		req.addElement(AdminConstants.E_NAME).setText(LC.zimbra_ldap_user.value());
		req.addElement(AdminConstants.E_PASSWORD).setText(LC.zimbra_ldap_password.value());
		Element resp = mTransport.invoke(req);
		mTransport.setAuthToken(resp.getElement(AccountConstants.E_AUTH_TOKEN).getText());
	}

	private SoapHttpTransport mTransport;

}
