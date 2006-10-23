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
package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.soap.Element;

public class Identity {
	public static final String sSECTION_IDENTITIES = "identities";
	public static final String sSIGNATURES = "signatures";
	
	public static final String sKEY_NAME = "name";
	
	public static List<Identity> get(Account acct, OperationContext octxt) throws ServiceException {
		ArrayList<Identity> identities = new ArrayList<Identity>();
		
		Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(acct.getId(), false);
		if (mbox == null)
			return identities;
		
		Metadata md = mbox.getConfig(octxt, sSECTION_IDENTITIES);
		if (md == null)
			return identities;
		
		for (Object key : md.asMap().keySet())
			if (key instanceof String) {
				String k = (String)key;
				identities.add(new Identity(k, md.getMap(k)));
			}
				
		return identities;
	}

	public synchronized static void create(Account acct, OperationContext octxt, Element req, String name) throws ServiceException {
		Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(acct.getId());
		Metadata md = mbox.getConfig(octxt, sSECTION_IDENTITIES);
		
		if (md == null)
			md = new Metadata();

		if (md.getMap(name, true) != null)
			throw ServiceException.INVALID_REQUEST("identity "+name+" already exists", null);
		
		Metadata newOne = new Metadata();
		populate(req, newOne);
		md.put(name, newOne);
		
		mbox.setConfig(octxt, sSECTION_IDENTITIES, md);
	}
	
	public synchronized static void modify(String id, Account acct, OperationContext octxt, Element req, String name) throws ServiceException {
		Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(acct.getId());
		Metadata md = mbox.getConfig(octxt, sSECTION_IDENTITIES);
		
		if (md == null)
			throw ServiceException.INVALID_REQUEST("identity not found", null);

		Metadata identity = md.getMap(name, true);
		if (identity == null)
			throw ServiceException.INVALID_REQUEST("identity not found", null);

		populate(req, identity);
		// rename
		if (identity.containsKey(MailService.A_NAME)) {
			md.remove(name);
			name = identity.get(MailService.A_NAME);
			identity.remove(MailService.A_NAME);
		}
		md.put(name, identity);
		
		mbox.setConfig(octxt, sSECTION_IDENTITIES, md);
	}
	
	public synchronized static void delete(String id, Account acct, OperationContext octxt, Element req, String name) throws ServiceException {
		Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(acct.getId());
		Metadata md = mbox.getConfig(octxt, sSECTION_IDENTITIES);
		
		if (md == null)
			throw ServiceException.INVALID_REQUEST("identity not found", null);

		Metadata identity = md.getMap(name, true);
		if (identity == null)
			throw ServiceException.INVALID_REQUEST("identity not found", null);

		md.remove(name);
		
		mbox.setConfig(octxt, sSECTION_IDENTITIES, md);
	}
	
	private static void populate(Element elem, Metadata md) throws ServiceException {
		HashMap<String,Object> attrs = new HashMap<String,Object>();
		Metadata sigmap = md.getMap(sSIGNATURES, true);
		if (sigmap == null)
			sigmap = new Metadata();
		for (Element a : elem.listElements()) {
			String name = a.getName();
			String v = a.getText();
			if (name.equals(MailService.E_ATTRIBUTE)) {
				StringUtil.addToMultiMap(attrs, a.getAttribute(MailService.E_NAME), v);
			} else if (name.equals(MailService.E_SIGNATURE)) {
				sigmap.put(a.getAttribute(MailService.E_NAME), v);
			}
		}

		for (Map.Entry<String,Object> entry : attrs.entrySet()) {
			String k = entry.getKey();
			Object v = entry.getValue();
			if (v instanceof String)
				md.put(k, (String)v);
			else if (v instanceof String[])
				md.put(k, new MetadataList(Arrays.asList((String[])v)));
		}
		md.put(sSIGNATURES, sigmap);
	}

	private String mName;
	private Metadata mData;

	private Identity(String name, Metadata md) throws ServiceException {
		mName = name;
		mData = md;
	}

	public String getName() {
		return mName;
	}
	
	public Map getAttrs() {
		HashMap<String,Object> attrs = new HashMap<String,Object>();
		Map md = mData.asMap();
		for (Object k : md.keySet()) {
			String key = (String) k;
			Object val = md.get(key);
			if (val instanceof String)
				attrs.put(key, val);
			else if (val instanceof MetadataList)
				attrs.put(key, ((MetadataList)val).asList().toArray(new String[0]));
		}
		return attrs;
	}
	
	public Map<String,String> getSignatures() throws ServiceException {
		Metadata sigmap = mData.getMap(sSIGNATURES, true);
		if (sigmap == null)
			return Collections.emptyMap();
		@SuppressWarnings("unchecked")
		Map<String,String> m = sigmap.asMap();
		return m;
	}
}
