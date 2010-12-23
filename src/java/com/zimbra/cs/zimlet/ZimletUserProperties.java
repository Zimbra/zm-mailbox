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
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.MapUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.soap.account.type.Prop;

/**
 * 
 * @author jylee
 *
 */
public class ZimletUserProperties {
    
    private static final long   TTL = 30 * 60 * 1000;  // 30 mins TTL for cache
    private long mCreateTime;

    private static Map<String, ZimletUserProperties> sUserPropMap;
    
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
        mPropSet = new HashSet<Prop>();
        for (int i = 0; i < props.length; i++) {
            try {
                Prop zp = new Prop(props[i]);
                mPropSet.add(zp);
            } catch (IllegalArgumentException iae) {
                ZimbraLog.zimlet.warn("invalid property: "+iae.getMessage());
            }
        }
        mCreateTime = System.currentTimeMillis();
    }
    
    public void setProperty(String zimlet, String key, String value) {
        Prop newProp = new Prop(zimlet, key, value);
        for (Prop zp: mPropSet) {
            if (zp.matches(newProp)) {
                zp.replace(newProp);
                return;
            }
        }
        mPropSet.add(newProp);
    }

    public Set<Prop> getAllProperties() {
        return mPropSet;
    }
    
    public void saveProperties(Account ac) throws ServiceException {
        String[] props = new String[mPropSet.size()];
        int index = 0;
        for (Prop zp: mPropSet) {
            props[index++] = zp.getSerialization();
        }
        Map<String, String[]> propMap = new HashMap<String, String[]>();
        propMap.put(Provisioning.A_zimbraZimletUserProperties, props);
        Provisioning.getInstance().modifyAttrs(ac, propMap);
    }
    
    private Set<Prop> mPropSet;
    
    public static void main(String[] args) throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Account acct = prov.get(AccountBy.name, "user1");
        ZimletUserProperties prop = new ZimletUserProperties(acct);
        prop.setProperty("phone","123123","aaaaaaaaaaaa");
        prop.setProperty("phone","number","bar");
        prop.saveProperties(acct);
    }
}
