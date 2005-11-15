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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.zimlet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ZimbraLog;

/**
 * 
 * @author jylee
 *
 */
public class ZimletUserProperties {
	
	private static final String SEPARATOR = ";";
	private static final long   TTL = 30 * 60 * 1000;  // 30 mins TTL for cache
	private long mCreateTime;

	private static Map sUserPropMap;
	
	static {
		sUserPropMap = new HashMap();
	}
	
	public static ZimletUserProperties getProperties(Account ac) {
		String id = ac.getId();
		ZimletUserProperties prop = (ZimletUserProperties) sUserPropMap.get(id);
		if (prop != null) {
			if (prop.mCreateTime + TTL < System.currentTimeMillis()) {
				prop = null;
				sUserPropMap.remove(id);
			}
		}
		if (prop == null) {
			prop = new ZimletUserProperties(ac);
			sUserPropMap.put(id, prop);
		}
		return prop;
	}
	
	private ZimletUserProperties(Account ac) {
		String[] props = ac.getMultiAttr(Provisioning.A_zimbraZimletUserProperties);
		mPropSet = new HashSet();
		for (int i = 0; i < props.length; i++) {
			try {
				ZimletProp zp = new ZimletProp(props[i]);
				mPropSet.add(zp);
			} catch (IllegalArgumentException iae) {
				ZimbraLog.zimlet.warn("invalid property: "+iae.getMessage());
			}
		}
		mCreateTime = System.currentTimeMillis();
	}
	
	public void setProperty(String zimlet, String key, String value) {
		ZimletProp newProp = new ZimletProp(zimlet, key, value);
		Iterator iter = mPropSet.iterator();
		while (iter.hasNext()) {
			ZimletProp zp = (ZimletProp) iter.next();
			if (zp.matches(newProp)) {
				zp.replace(newProp);
				return;
			}
		}
		mPropSet.add(newProp);
	}

	public Set getAllProperties() {
		return mPropSet;
	}
	
	public void saveProperties(Account ac) throws ServiceException {
		String[] props = new String[mPropSet.size()];
		Iterator iter = mPropSet.iterator();
		int index = 0;
		while (iter.hasNext()) {
			ZimletProp zp = (ZimletProp) iter.next();
			props[index++] = zp.prop;
		}
		Map propMap = new HashMap();
		propMap.put(Provisioning.A_zimbraZimletUserProperties, props);
		ac.modifyAttrs(propMap);
	}
	
	private Set mPropSet;
		
	public static class ZimletProp implements ZimletProperty {
		public ZimletProp(String zimlet, String key, String value) {
			this.zimlet = zimlet;
			this.key = key;
			this.value = value;
			this.prop = zimlet + SEPARATOR + key + SEPARATOR + value;
		}
		public ZimletProp(String prop) throws IllegalArgumentException {
			this.prop = prop;
			int sep1 = prop.indexOf(SEPARATOR);
			int sep2 = prop.indexOf(SEPARATOR, sep1+1);
			if (sep1 < 0 || sep2 < 0) {
				throw new IllegalArgumentException(prop);
			}
			zimlet = prop.substring(0, sep1);
			key = prop.substring(sep1+1, sep2);
			value = prop.substring(sep2+1);
		}
		public boolean matches(ZimletProp prop) {
			return (zimlet.equals(prop.zimlet) && key.equals(prop.key));
		}
		public void replace(ZimletProp prop) {
			this.zimlet = prop.zimlet;
			this.key = prop.key;
			this.value = prop.value;
			this.prop = prop.prop;
		}
		public String getZimletName() {
			return zimlet;
		}
		public String getKey() {
			return key;
		}
		public String getValue() {
			return value;
		}
		public String zimlet;
		public String key;
		public String value;
		public String prop;
	}
	
	public static void main(String[] args) throws Exception {
		Provisioning prov = Provisioning.getInstance();
		Account acct = prov.getAccountByName("user1");
		ZimletUserProperties prop = new ZimletUserProperties(acct);
		prop.setProperty("phone","123123","aaaaaaaaaaaa");
		prop.setProperty("phone","number","bar");
		prop.saveProperties(acct);
	}
}
