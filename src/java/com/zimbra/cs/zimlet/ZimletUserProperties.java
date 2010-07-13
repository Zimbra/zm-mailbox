/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.zimlet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.util.MapUtil;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

/**
 * 
 * @author jylee
 *
 */
public class ZimletUserProperties {
	
	private static final String SEPARATOR = ":";
	private static final long   TTL = 30 * 60 * 1000;  // 30 mins TTL for cache
	private long mCreateTime;

	private static Map sUserPropMap;
	
	static {
		sUserPropMap = MapUtil.newLruMap(1024);
	}
	
	public static ZimletUserProperties getProperties(Account ac) {
		String id = ac.getId();
        ZimletUserProperties prop;
        synchronized (sUserPropMap) {
            prop = (ZimletUserProperties)sUserPropMap.get(id);
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
        }
		return prop;
	}
	
	private ZimletUserProperties(Account ac) {
		String[] props = ac.getMultiAttr(Provisioning.A_zimbraZimletUserProperties);
		mPropSet = new HashSet<ZimletProp>();
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

	public Set<ZimletProp> getAllProperties() {
		return mPropSet;
	}
	
	public void saveProperties(Account ac) throws ServiceException {
		String[] props = new String[mPropSet.size()];
		Iterator<ZimletProp> iter = mPropSet.iterator();
		int index = 0;
		while (iter.hasNext()) {
			ZimletProp zp = iter.next();
			props[index++] = zp.prop;
		}
		Map<String, String[]> propMap = new HashMap<String, String[]>();
		propMap.put(Provisioning.A_zimbraZimletUserProperties, props);
		Provisioning.getInstance().modifyAttrs(ac, propMap);
	}
	
	private Set<ZimletProp> mPropSet;
		
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
		public boolean matches(ZimletProp other) {
			return (zimlet.equals(other.zimlet) && key.equals(other.key));
		}
		public void replace(ZimletProp other) {
			this.zimlet = other.zimlet;
			this.key = other.key;
			this.value = other.value;
			this.prop = other.prop;
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
		Account acct = prov.get(AccountBy.name, "user1");
		ZimletUserProperties prop = new ZimletUserProperties(acct);
		prop.setProperty("phone","123123","aaaaaaaaaaaa");
		prop.setProperty("phone","number","bar");
		prop.saveProperties(acct);
	}
}
