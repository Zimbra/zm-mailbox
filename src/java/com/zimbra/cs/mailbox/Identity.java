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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;
import com.zimbra.soap.Element.Attribute;

public class Identity {
	public static final String sSECTION_IDENTITIES = "identities";
	
	public static final String sKEY_NAME = "name";
	public static final String sKEY_ID = "id";
	public static final String sKEY_LAST_ID = "lastid";
	
	public static List<Identity> get(Account acct, OperationContext octxt) throws ServiceException {
		ArrayList<Identity> identities = new ArrayList<Identity>();
		
		Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(acct.getId(), false);
		if (mbox == null)
			return identities;
		
		Metadata md = mbox.getConfig(octxt, sSECTION_IDENTITIES);
		if (md == null)
			return identities;
		List mdlist = md.getList(sSECTION_IDENTITIES).asList();
		for (Object obj : mdlist)
			if (obj instanceof Metadata)
				identities.add(new Identity((Metadata)obj));
		return identities;
	}

	public synchronized static Identity create(Account acct, OperationContext octxt, Element req) throws ServiceException {
		Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(acct.getId());
		Metadata md = mbox.getConfig(octxt, sSECTION_IDENTITIES);
		long lastId;
		
		if (md == null) {
			md = new Metadata();
		}

		try {
			lastId = md.getLong(sKEY_LAST_ID);
		} catch (ServiceException e) {
			lastId = 0;
		}
		
		MetadataList ls;
		try {
			ls = md.getList(sSECTION_IDENTITIES);
		} catch (ServiceException e) {
			ls = new MetadataList();
			md.put(sSECTION_IDENTITIES, ls);
		}
		
		Metadata newOne = new Metadata();
		populate(req, newOne);
		
		newOne.put(sKEY_ID, Long.toString(++lastId));
		ls.add(newOne);
		md.put(sKEY_LAST_ID, lastId);
		md.put(sSECTION_IDENTITIES, ls);
		mbox.setConfig(octxt, sSECTION_IDENTITIES, md);
		return new Identity(newOne);
	}
	
	public synchronized static Identity update(String id, Account acct, OperationContext octxt, Element req, boolean delete) throws ServiceException {
		Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(acct.getId());
		Metadata md = mbox.getConfig(octxt, sSECTION_IDENTITIES);
		MetadataList ls;
		
		if (md == null)
			throw ServiceException.INVALID_REQUEST("identity not found", null);
		else
			ls = md.getList(sSECTION_IDENTITIES);

		Metadata identity = null;
		for (int index = 0; index < ls.size(); index++) {
			identity = ls.getMap(index);
			if (id.equals(identity.get(sKEY_ID))) {
				ls.remove(index);
				break;
			}
			identity = null;
		}
		
		if (identity == null)
			throw ServiceException.INVALID_REQUEST("identity not found", null);

		if (!delete) {
			populate(req, identity);
			ls.add(identity);
		}
		
		md.put(sSECTION_IDENTITIES, ls);
		mbox.setConfig(octxt, sSECTION_IDENTITIES, md);
		return new Identity(identity);
	}
	
	private static void populate(Element elem, Metadata md) throws ServiceException {
		for (Attribute a : elem.listAttributes())
			md.put(a.getKey(), a.getValue());
		
		TreeSet<String> multiValueAttrs = new TreeSet<String>();
		for (Element e : elem.listElements(null)) {
			String key = e.getName();
			if (multiValueAttrs.contains(key))
				continue;
			multiValueAttrs.add(key);
			MetadataList ls = new MetadataList();
			for (Element attr : elem.listElements(key))
				ls.add(attr.getText());
			md.put(key, ls);
		}
	}
	
	private Map mData;

	private Identity(Metadata md) throws ServiceException {
		mData = md.asMap();
	}

	public String getName() {
		return get(sKEY_NAME);
	}
	
	public String getId() {
		return get(sKEY_ID);
	}

	public enum AttributeType {
		string, map, list, unknown
	}
	
	public Set getAttributeNames() {
		return mData.keySet();
	}
	
	public AttributeType getAttributeType(String key) {
		Object val = mData.get(key);
		if (val instanceof String)
			return AttributeType.string;
		else if (val instanceof Metadata)
			return AttributeType.map;
		else if (val instanceof MetadataList)
			return AttributeType.list;
		
		// then what?
		return AttributeType.unknown;
	}
	
	public String get(String key) {
		Object val = mData.get(key);
		if (val instanceof String)
			return (String)val;
		return null;
	}
	
	public List getList(String key) {
		Object val = mData.get(key);
		if (val instanceof MetadataList)
			return ((MetadataList)val).asList();
		return null;
	}
	
	public Map getMap(String key) {
		Object val = mData.get(key);
		if (val instanceof Metadata)
			return ((Metadata)val).asMap();
		return null;
	}
}
