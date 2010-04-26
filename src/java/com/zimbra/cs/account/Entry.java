/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.account;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.zclient.ToZJSONObject;
import com.zimbra.cs.zclient.ZJSONObject;
import org.json.JSONException;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.zimbra.cs.account.AttributeManager.IDNType;

public abstract class Entry implements ToZJSONObject {

    private Map<String,Object> mAttrs;
    private Map<String,Object> mDefaults;
    private Map<String,Object> mSecondaryDefaults;
    private Map<String, Object> mData;
    private Map<String, Set<String>> mMultiAttrSetCache;
    private Locale mLocale;
    private Provisioning mProvisioning;
    private AttributeManager mAttrMgr;
    
    protected static String[] sEmptyMulti = new String[0];

    /*
    protected Entry(Map<String,Object> attrs, Map<String,Object> defaults) {
        mAttrs = attrs;
        mDefaults = defaults;
    }
    
    protected Entry(Map<String,Object> attrs, Map<String,Object> defaults, Map<String,Object> secondaryDefaults) {
        mAttrs = attrs;
        mDefaults = defaults;
        mSecondaryDefaults = secondaryDefaults;
    }
    */
    
    protected Entry(Map<String,Object> attrs, Map<String,Object> defaults, Provisioning provisioning) {
    	mProvisioning = provisioning;
    	mAttrs = attrs;
        mDefaults = defaults;
        setAttributeManager();
    }
    
    protected Entry(Map<String,Object> attrs, Map<String,Object> defaults, Map<String,Object> secondaryDefaults, Provisioning provisioning) {
    	mProvisioning = provisioning;
    	mAttrs = attrs;
        mDefaults = defaults;
        mSecondaryDefaults = secondaryDefaults;
        setAttributeManager();
    }
    
    private void setAttributeManager() {
        try {
            mAttrMgr = AttributeManager.getInstance();
        } catch (ServiceException se) {
            ZimbraLog.account.warn("failed to get AttributeManager instance", se);
        }
    }
    
    private AttributeManager getAttributeManager() {
        return mAttrMgr;
    }
    
    public Provisioning getProvisioning() {
    	return mProvisioning;
    }
    
    // for debugging/logging, subclass should define a proper "label" for the entry by that it is best identified
    public String getLabel() {
        return "unknown";
    }
    
    public synchronized void setAttrs(Map<String,Object> attrs, Map<String,Object> defaults, Map<String,Object> secondaryDefaults) {
        mAttrs = attrs;
        mDefaults = defaults;
        mSecondaryDefaults = secondaryDefaults;
        resetData();
    }
    
    public synchronized void setAttrs(Map<String,Object> attrs) {
        mAttrs = attrs;
        resetData();
    }
    
    public synchronized void setDefaults(Map<String,Object> defaults) {
        mDefaults = defaults;
        resetData();
    }
    
    public synchronized void setDefaults(Map<String,Object> defaults, Map<String,Object> secondaryDefaults) {
        mDefaults = defaults;
        mSecondaryDefaults = secondaryDefaults;
        resetData();
    }
    
    public synchronized void setSecondaryDefaults(Map<String,Object> secondaryDefaults) {
        mSecondaryDefaults = secondaryDefaults;
        resetData();
    }
    
    protected synchronized void resetData()
    {
        if (mMultiAttrSetCache != null)            
            mMultiAttrSetCache.clear();
        if (mData != null)
            mData.clear();
        mLocale = null;
    }

    /**
     * looks up name in map, and if found, returns its value.
     * if not found, get real attr name from AttributeManager and try getting from the map again
     * 
     * @param name
     * @return
     */
    private Object getObject(String name, boolean applyDefaults) {
        Object v = mAttrs.get(name);
        if (v != null) return v;
        
        v = getValueByRealAttrName(name, mAttrs);
        if (v != null) return v;
        
        if (!applyDefaults)
            return null;
        
        return getAttrDefault(name);
    }
    
    public Object getAttrDefault(String name) {
        
        Object v;
        
        // check defaults
        if (mDefaults != null) {
            v = mDefaults.get(name);
            if (v != null) return v;
            
            v = getValueByRealAttrName(name, mDefaults);
            if (v != null) return v;
        }
        
        // check secondary defaults
        if (mSecondaryDefaults != null) {
            v = mSecondaryDefaults.get(name);
            if (v != null) return v;
            
            v = getValueByRealAttrName(name, mSecondaryDefaults);
            if (v != null) return v;
            
        }
        
        return null;
    }

    
    private Object getValueByRealAttrName(String attrName, Map<String,Object> map) {
        AttributeManager attrMgr = getAttributeManager();
        if (attrMgr != null) {
            AttributeInfo ai = attrMgr.getAttributeInfo(attrName);
            if (ai != null)
                return map.get(ai.getName());
        }
        return null;
    }
    
    /*
     * convert attr values to unicode and put back the converted value to the same attr map
     * We are modifying a copy of the map created in getAttrs, not the mAttrs/mDefaults/mSecondaryDefaults data member
     */
    private Map<String, Object> toUnicode(Map<String, Object> attrs) {
        AttributeManager attrMgr = getAttributeManager();
                
        Set<String> keySet = new HashSet<String>(attrs.keySet());
        for (String key : keySet) {
            IDNType idnType = AttributeManager.idnType(attrMgr, key);
            if (idnType.isEmailOrIDN()) {
                Object value = attrs.get(key);
                if (value instanceof String[]) {
                    String sv[] = (String[]) value;
                    for (int i=0; i<sv.length; i++) {
                        sv[i] = IDNUtil.toUnicode(sv[i], idnType);
                    }
                } else if (value instanceof String){
                    attrs.put(key, IDNUtil.toUnicode((String)value, idnType));
                }
            }
        }
        
        return attrs;
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
    
    public Map<String, Object> getUnicodeAttrs() {
        Map<String, Object> attrs = getAttrs(true);
        return toUnicode(attrs);
    }

    public Map<String, Object> getAttrs(boolean applyDefaults) {
        if (applyDefaults && (mDefaults != null || mSecondaryDefaults != null)) {
            Map<String, Object> attrs = new HashMap<String, Object>();
            // put the second defaults
            if (mSecondaryDefaults != null)
                attrs.putAll(mSecondaryDefaults);
            
            // put the defaults
            if (mDefaults != null)
                attrs.putAll(mDefaults);
            
            // override with currently set
            attrs.putAll(mAttrs);
            return attrs;
        } else {
            return mAttrs;
        }
    }
    
    public Map<String, Object> getUnicodeAttrs(boolean applyDefaults) {
        Map<String, Object> attrs = getAttrs(applyDefaults);
        return toUnicode(attrs);
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

    /**
     * Returns the set of values for the given attribute, or an empty
     * array if no values are defined.
     */
    public String[] getMultiAttr(String name) {
        return getMultiAttr(name, true);
    }
    
    /**
     * Returns the set of values for the given attribute, or an empty
     * array if no values are defined.
     */
    public String[] getUnicodeMultiAttr(String name) {
        String[] values = getMultiAttr(name, true);
        
        AttributeManager attrMgr = getAttributeManager();
        IDNType idnType = AttributeManager.idnType(attrMgr, name);
        if (idnType.isEmailOrIDN() && values != null) {
            String[] unicodeValues = new String[values.length];
            for (int i=0; i<values.length; i++)
                unicodeValues[i] = IDNUtil.toUnicode(values[i], idnType);
            return unicodeValues;
        } else
            return values;
    }

    /**
     * Returns the set of values for the given attribute, or an empty
     * array if no values are defined.
     */
    public String[] getMultiAttr(String name, boolean applyDefaults) {
        Object v = getObject(name, applyDefaults);
        if (v instanceof String) return new String[]{(String) v};
        else if (v instanceof String[]) {
            return (String[]) v;
        } else {
            return sEmptyMulti;
        }
    }

    /**
     * Returns the set of values for the given attribute, or an empty
     * set if no values are defined.
     */
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
     * @param name name of the attribute to retrieve. 
     * @param defaultValue value to use if attr is not present or can't be parsed.
     * @return interval in milliseconds
     */
    public long getTimeInterval(String name, long defaultValue) {
        return DateUtil.getTimeInterval(getAttr(name), defaultValue);        
    }

    /**
     * get a time interval, which is a number, optional followed by a character denoting the units
     * (d = days, h = hours, m = minutes, s = seconds. If no character unit is specified, the default is
     * seconds.
     * 
     * the time interval is returned in seconds.
     * 
     * @param name name of the attribute to retrieve. 
     * @param defaultValue value to use if attr is not present or can't be parsed.
     * @return interval in seconds
     */
    public long getTimeIntervalSecs(String name, long defaultValue) {
        return DateUtil.getTimeIntervalSecs(getAttr(name), defaultValue);        
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
     * temporarily associate a key/value pair with this entry. When an entry is reloaded, any cached data is cleared via
     * a call to resetData.
     *
     * TODO: retire setCachedData(String key, Object value) and use only this signature
     *       IMPORTANT: REMEMBER TO ADD synchronized TO THIS METHOD WHEN WE DO THE REFACTORING. 

     * @param key
     * @param value
     */
    public void setCachedData(EntryCacheDataKey key, Object value) {
        setCachedData(key.getKeyName(), value);
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
    
    /**
     * get an entry from the cache.
     * 
     * TODO: retire getCachedData(String key) and use only this signature
     *       IMPORTANT: REMEMBER TO ADD synchronized TO THIS METHOD WHEN WE DO THE REFACTORING. 
     * 
     * @param key
     * @return
     */
    public Object getCachedData(EntryCacheDataKey key) {
        return getCachedData(key.getKeyName());
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
        return String.format("[%s]", getClass().getName());
        /*
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName()).append(": {  ");        
        //sb.append(getClass().getName()).append(": { name=").append(getName()).append(" ");
        sb.append(mAttrs.toString());
        sb.append("}");
        return sb.toString();
                   */
    }

    public ZJSONObject toZJSONObject() throws JSONException {
            return toZJSONObject(null, true);
    }

    static Pattern regex = Pattern.compile("^/(.*)/(i)?$");
    
    public ZJSONObject toZJSONObject(String filter, boolean applyDefaults) throws JSONException {
        Map<String, Object> attrs = getAttrs(applyDefaults);

        if (this instanceof NamedEntry) {
            NamedEntry ne = (NamedEntry) this;
            attrs.put("id", ne.getId());
            attrs.put("name", ne.getName());
        }

        Pattern pattern = null;
        if (filter != null) {
            Matcher rm = regex.matcher(filter);
            if (rm.matches())
                pattern = Pattern.compile(rm.group(1), "i".equals(rm.group(2)) ? Pattern.CASE_INSENSITIVE : 0);
            else
                filter = filter.toLowerCase();
        }

        ZJSONObject zj = new ZJSONObject();
        for (Map.Entry<String,Object> entry : attrs.entrySet()) {
            if (pattern != null) {
                if (!pattern.matcher(entry.getKey()).find()) continue;
            } else if (filter != null) {
                if (!entry.getKey().toLowerCase().contains(filter)) continue;
            }
            Object o = entry.getValue();
            if (o instanceof String) {
                zj.put(entry.getKey(), (String)o);
            } else if (o instanceof String[]) {
                zj.put(entry.getKey(), (String[])o);
            }
        }
        return zj;
    }

    public String dump() throws JSONException {
        return dump(null, true);
    }

    public String dump(String filter) throws JSONException {
        return dump(filter, true);
    }

    public String dump(String filter, boolean applyDefaults) throws JSONException {
        return toZJSONObject(filter, applyDefaults).toString();
    }


}
