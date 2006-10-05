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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.common.util.DateUtil;

public abstract class Entry {

    private Map<String,Object> mAttrs;
    private Map<String,Object> mDefaults;    
    private Map<String, Object> mData;
    private Map<String, Set<String>> mMultiAttrSetCache;

    protected static String[] sEmptyMulti = new String[0];

    protected Entry(Map<String,Object> attrs, Map<String,Object> defaults) {
        mAttrs = attrs;
        mDefaults = defaults;
    }
    
    public void setAttrs(Map<String,Object> attrs, Map<String,Object> defaults) {
        mAttrs = attrs;
        mDefaults = defaults;
        resetData();
    }
    
    public void setAttrs(Map<String,Object> attrs) {
        mAttrs = attrs;
        resetData();
    }
    
    public void setDefaults(Map<String,Object> defaults) {
        mDefaults = defaults;
        resetData();
    }
    
    protected synchronized void resetData()
    {
        if (mMultiAttrSetCache != null)            
            mMultiAttrSetCache.clear();
        if (mData != null)
            mData.clear();
    }

    /**
     * looks up name in map, and if found, returns its value.
     * if not found, iterate through key names and compare using equalsIgnoreCase
     * @param name
     * @return
     */
    private Object getObject(String name, boolean applyDefaults) {
        Object v = mAttrs.get(name);
        if (v != null) return v;
        
        for (String key: mAttrs.keySet()) {
            if (key.equalsIgnoreCase(name))
                return mAttrs.get(key);
        }
        
        if (mDefaults == null || !applyDefaults) return null;
        
        v = mDefaults.get(name);
        if (v != null) return v;
        
        for (String key: mDefaults.keySet()) {
            if (key.equalsIgnoreCase(name))
                return mDefaults.get(key);
        }
        return null;
    }
    
    public String getAttr(String name) {
        return getAttr(name, true);
    }

    public String getAttr(String name, boolean applyDefaults) {
        Object v = getObject(name, applyDefaults);
        if (v instanceof String) {
            return (String) v;
        } else if (v instanceof String[]) {
            String[] a = (String[]) v;
            return a.length > 0 ? a[0] : null;
        } else {
            return null;
        }
    }

    public String getAttr(String name, String defaultValue) {
        String v = getAttr(name);
        return v == null ? defaultValue : v;
    }

    protected Map<String, Object> getRawAttrs() {
        return mAttrs;
    }

    public Map<String, Object> getAttrs() {
        return getAttrs(true);
    }

    public Map<String, Object> getAttrs(boolean applyDefaults) {
        if (applyDefaults && mDefaults != null) {
            Map<String, Object> attrs = new HashMap<String, Object>();
            // put the defaults
            attrs.putAll(mDefaults);
            // override with currently set
            attrs.putAll(mAttrs);
            return attrs;
        } else {
            return mAttrs;
        }
    }

    /**
     * 
     * @param name name of the attribute to retreive. 
     * @param defaultValue value to use if attr is not present
     * @return
     */
    public boolean getBooleanAttr(String name, boolean defaultValue) {
        String v = getAttr(name);
        return v == null ? defaultValue : Provisioning.TRUE.equals(v);
    }

    /**
     * 
     * @param name name of the attribute to retreive. 
     * @param defaultValue value to use if attr is not present or can't be parsed.
     * @return
     */
    public Date getGeneralizedTimeAttr(String name, Date defaultValue) {
        String v = getAttr(name);
        if (v == null)
            return defaultValue;
        Date d = DateUtil.parseGeneralizedTime(v);
        return d == null ? defaultValue : d;
    }

    /**
     * 
     * @param name name of the attribute to retreive. 
     * @param defaultValue value to use if attr is not present or can't be parsed.
     * @return
     */
    public int getIntAttr(String name, int defaultValue) {
        String v = getAttr(name);
        try {
            return v == null ? defaultValue : Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Locale mLocale;
    
    public Locale getLocale() throws ServiceException {
        // Don't synchronize the entire method because Provisioning.getLocale
        // can recursively call LdapEntry.getLocale() on multiple entries.
        // If LdapEntry.getLocale() was synchronized, we might get into a
        // deadlock.
        synchronized (this) {
            if (mLocale != null)
                return mLocale;
        }
        Locale lc = Provisioning.getInstance().getLocale(this);
        synchronized (this) {
            mLocale = lc;
            return mLocale;
        }
    }

    /**
     * 
     * @param name name of the attribute to retreive. 
     * @param defaultValue value to use if attr is not present or can't be parsed.
     * @return
     */
    public long getLongAttr(String name, long defaultValue) {
        String v = getAttr(name);
        try {
            return v == null ? defaultValue : Long.parseLong(v);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public String[] getMultiAttr(String name) {
        return getMultiAttr(name, true);
    }

    public String[] getMultiAttr(String name, boolean applyDefaults) {
        Object v = getObject(name, applyDefaults);
        if (v instanceof String) return new String[]{(String) v};
        else if (v instanceof String[]) {
            return (String[]) v;
        } else {
            return sEmptyMulti;
        }
    }

    public Set<String> getMultiAttrSet(String name) {
        if (mMultiAttrSetCache == null)        
            mMultiAttrSetCache = new HashMap<String, Set<String>>();        
        Set<String> result = mMultiAttrSetCache.get(name);
        if (result == null) {
            result = new HashSet<String>(Arrays.asList(getMultiAttr(name)));
            mMultiAttrSetCache.put(name, result);
        }
        return result;
    }

    /**
     * get a time interval, which is a number, optional followed by a character denoting the units
     * (d = days, h = hours, m = minutes, s = seconds. If no character unit is specified, the default is
     * seconds.
     * 
     * the time interval is returned in milliseconds.
     * 
     * @param name name of the attribute to retreive. 
     * @param defaultValue value to use if attr is not present or can't be parsed.
     * @return interval in milliseconds
     */
    public long getTimeInterval(String name, long defaultValue) {
        return DateUtil.getTimeInterval(getAttr(name), defaultValue);        
    }

    /**
     * temporarily associate a key/value pair with this entry. When an entry is reloaded, any cached data is cleared via
     * a call to resetData.
     * @param key
     * @param value
     */
    public synchronized void setCachedData(String key, Object value) {
        if (mData == null)
            mData = new HashMap<String, Object>();
        mData.put(key, value);
    }

    /**
     * get an entry from the cache.
     * @param key
     * @return
     */
    public synchronized Object getCachedData(String key) {
        if (mData == null) return null;
        return mData.get(key);
    }
    
    protected void getDefaults(AttributeFlag flag, Map<String,Object> defaults) throws ServiceException {
        defaults.clear();
        Set<String> attrs = AttributeManager.getInstance().getAttrsWithFlag(flag);
        for (String a : attrs) {
            Object obj = getObject(a, true);
            if (obj != null) defaults.put(a, obj);
        }
        //return Collections.unmodifiableMap(defaults);
    }

    public synchronized String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName()).append(": {  ");        
        //sb.append(getClass().getName()).append(": { name=").append(getName()).append(" ");
        sb.append(mAttrs.toString());
        sb.append("}");
        return sb.toString();           
    }
}
