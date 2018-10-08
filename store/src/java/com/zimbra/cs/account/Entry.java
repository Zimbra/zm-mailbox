/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import java.text.CollationKey;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import com.zimbra.client.ToZJSONObject;
import com.zimbra.client.ZJSONObject;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeManager.IDNType;
import com.zimbra.cs.ephemeral.EphemeralInput;
import com.zimbra.cs.ephemeral.EphemeralInput.Expiration;
import com.zimbra.cs.ephemeral.EphemeralKey;
import com.zimbra.cs.ephemeral.EphemeralLocation;
import com.zimbra.cs.ephemeral.EphemeralResult;
import com.zimbra.cs.ephemeral.EphemeralStore;
import com.zimbra.cs.ephemeral.EphemeralStore.FailureMode;
import com.zimbra.cs.ephemeral.LdapEntryLocation;
import com.zimbra.cs.ephemeral.LdapEphemeralStore;
import com.zimbra.cs.ldap.LdapDateUtil;
import com.zimbra.cs.util.MemoryUnitUtil;

public abstract class Entry implements ToZJSONObject {

    public static enum EntryType {
        ENTRY,  // a generic entry, only used in extension
        ACCOUNT,
        ALIAS,
        CALRESOURCE,
        COS,
        DATASOURCE,
        DISTRIBUTIONLIST,
        DOMAIN,
        DYNAMICGROUP,
        DYNAMICGROUP_DYNAMIC_UNIT,
        DYNAMICGROUP_STATIC_UNIT,
        GLOBALCONFIG,
        GLOBALGRANT,
        IDENTITY,
        MIMETYPE,
        SERVER,
        ALWAYSONCLUSTER,
        UCSERVICE,
        SIGNATURE,
        XMPPCOMPONENT,
        HABGROUP,
        ZIMLET,
        ADDRESS_LIST;

        public String getName() {
            return name();
        }
    }

    private Map<String,Object> mAttrs;
    private Map<String,Object> mDefaults;
    private Map<String,Object> mSecondaryDefaults;
    private Map<String, Object> overrideDefaults;
    private Map<String, Object> mData;
    private Map<String, Set<String>> mMultiAttrSetCache;
    private Map<String, Set<byte[]>> mMultiBinaryAttrSetCache;
    private Locale mLocale;
    private final Provisioning mProvisioning;
    private AttributeManager mAttrMgr;

    protected static String[] sEmptyMulti = new String[0];
    protected static List<byte[]> sEmptyListMulti = new ArrayList<byte[]>();

    public EntryType getEntryType() {
        return EntryType.ENTRY;
    }

    protected Entry(Map<String,Object> attrs, Map<String,Object> defaults,
            Provisioning provisioning) {
    	mProvisioning = provisioning;
    	mAttrs = attrs;
        mDefaults = defaults;
        setAttributeManager();
    }

    protected Entry(Map<String,Object> attrs, Map<String,Object> defaults,
            Map<String,Object> secondaryDefaults, Provisioning provisioning) {
    	mProvisioning = provisioning;
    	mAttrs = attrs;
        mDefaults = defaults;
        mSecondaryDefaults = secondaryDefaults;
        setAttributeManager();
    }

    protected Entry(Map<String,Object> attrs, Map<String,Object> defaults,
            Map<String,Object> secondaryDefaults, Map<String,Object> overrideDefaults, Provisioning provisioning) {
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

    // for debugging/logging, subclass should define a proper "label"
    // for the entry by that the entry is best identified
    public String getLabel() {
        return "unknown";
    }

    public synchronized void setAttrs(Map<String,Object> attrs,
            Map<String,Object> defaults, Map<String,Object> secondaryDefaults, Map<String,Object> overrideDefaults) {
        mAttrs = attrs;
        mDefaults = defaults;
        mSecondaryDefaults = secondaryDefaults;
        this.overrideDefaults = overrideDefaults;
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

    public synchronized void setDefaults(Map<String,Object> defaults,
            Map<String,Object> secondaryDefaults) {
        mDefaults = defaults;
        mSecondaryDefaults = secondaryDefaults;
        resetData();
    }

    public synchronized void setSecondaryDefaults(Map<String,Object> secondaryDefaults) {
        mSecondaryDefaults = secondaryDefaults;
        resetData();
    }

    public synchronized void setOverrideDefaults(Map<String,Object> overrideDefaults) {
        this.overrideDefaults = overrideDefaults;
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
     * if not found, get real attr name from AttributeManager and try getting
     * from the map again
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

        if (overrideDefaults != null) {
            v = overrideDefaults.get(name);
            if (v != null) return v;

            v = getValueByRealAttrName(name, overrideDefaults);
            if (v != null) return v;
        }

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
     * We are modifying a copy of the map created in getAttrs, not the
     * mAttrs/mDefaults/mSecondaryDefaults data member
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
        return getAttr(name, true, false);
    }

    public String getAttr(String name, boolean applyDefaults) {
        return getAttr(name, applyDefaults, false);
    }

    public String getAttr(String name, boolean applyDefaults, boolean skipEphemeralCheck) {
        if (!skipEphemeralCheck && mAttrMgr.isEphemeral(name)) {
            try {
                if (mAttrMgr.isDynamic(name)) {
                    ZimbraLog.ephemeral.warn("can't get value of dynamic ephemeral attribute %s without the dynamic component", name);
                    return null;
                }
                String value = getEphemeralAttr(name).getValue();
                if (!Strings.isNullOrEmpty(value)) {
                    return value;
                } else {
                    return applyDefaults ? objectToString(getAttrDefault(name)) : null;
                }
            } catch (ServiceException e) {
                ZimbraLog.ephemeral.warn("error getting value for %s, returning default", name);
                return applyDefaults ? objectToString(getAttrDefault(name)) : null;
            }
        } else {
            Object v = getObject(name, applyDefaults);
            return objectToString(v);
        }
    }

    public String getAttr(String name, String defaultValue) {
        String v = getAttr(name, true, false);
        return v == null ? defaultValue : v;
    }

    protected String getAttr(String name, String defaultValue, boolean skipEphemeralCheck) {
        String v = getAttr(name, true, skipEphemeralCheck);
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
        return getAttrs(applyDefaults, true);
    }

    public Map<String, Object> getAttrs(boolean applyDefaults, boolean includeEphemeral) {
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

            // override with overrides if set
            if (overrideDefaults != null) {
                attrs.putAll(overrideDefaults);
            }
            attrs.putAll(mAttrs);
            if (includeEphemeral) {
                attrs.putAll(getEphemeralAttrs());
            }
            return attrs;
        } else if (includeEphemeral) {
            Map<String, Object> attrs = getEphemeralAttrs();
            attrs.putAll(mAttrs);
            return attrs;
        } else {
            return mAttrs;
        }
    }

    /**
     * Returns values for non-dynamic ephemeral attributes.
     */
    public Map<String, Object> getEphemeralAttrs() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        try {
            EphemeralStore.Factory ephemeralFactory = EphemeralStore.getFactory(FailureMode.safe);
            if (ephemeralFactory == null || ephemeralFactory instanceof LdapEphemeralStore.Factory) {
                //Short-circuit for LDAP backends, since the data will already be in mAttrs.
                //This also catches scenarios where the EphemeralStore is not available.
                return attrs;
            }
            Map<String, AttributeInfo> ephemeralAttrs = mAttrMgr.getNonDynamicEphemeralAttrs(getEntryType());
            if (ephemeralAttrs == null) {
                return attrs;
            }
            for (Map.Entry<String, AttributeInfo> entry: ephemeralAttrs.entrySet()) {
                String attrName= entry.getKey();
                AttributeInfo info = entry.getValue();
                EphemeralResult result = getEphemeralAttr(attrName);
                if (!result.isEmpty()) {
                    switch(info.getType()) {
                    case TYPE_BOOLEAN:
                        attrs.put(attrName, result.getBoolValue());
                        break;
                    case TYPE_INTEGER:
                        attrs.put(attrName, result.getIntValue());
                        break;
                    case TYPE_LONG:
                        attrs.put(attrName, result.getLongValue());
                        break;
                    default:
                        attrs.put(attrName, result.getValue());
                        break;
                    }
                }
            }
        } catch (ServiceException e) {
            // don't propagate this exception, since we don't want to interrupt getAttrs() calls
            ZimbraLog.ephemeral.warn("unable to get ephemeral attributes for %s %s", getEntryType().getName(), getLabel());
        }
        return attrs;
    }

    public Map<String, Object> getUnicodeAttrs(boolean applyDefaults) {
        return getUnicodeAttrs(applyDefaults, true);
    }

    public Map<String, Object> getUnicodeAttrs(boolean applyDefaults, boolean includeEphemeral) {
        Map<String, Object> attrs = getAttrs(applyDefaults, includeEphemeral);
        return toUnicode(attrs);
    }

    /**
     *
     * @param name name of the attribute to retreive.
     * @param defaultValue value to use if attr is not present
     * @return
     */
    public boolean getBooleanAttr(String name, boolean defaultValue) {
        return getBooleanAttr(name, defaultValue, false);
    }

    protected boolean getBooleanAttr(String name, boolean defaultValue, boolean skipEphemeralCheck) {
        String v = getAttr(name, true, skipEphemeralCheck);
        return v == null ? defaultValue : ProvisioningConstants.TRUE.equals(v);
    }

    public byte[] getBinaryAttr(String name) {
        return getBinaryAttr(name, false);
    }

    protected byte[] getBinaryAttr(String name, boolean skipEphemeralCheck) {
        String v = getAttr(name, true, skipEphemeralCheck);
        return v == null ? null : ByteUtil.decodeLDAPBase64(v);
    }

    /**
     *
     * @param name name of the attribute to retreive.
     * @param defaultValue value to use if attr is not present or can't be parsed.
     * @return
     */
    public Date getGeneralizedTimeAttr(String name, Date defaultValue) {
        return getGeneralizedTimeAttr(name, defaultValue, false);
    }

    protected Date getGeneralizedTimeAttr(String name, Date defaultValue, boolean skipEphemeralCheck) {
        String v = getAttr(name, true, skipEphemeralCheck);
        if (v == null)
            return defaultValue;
        Date d = LdapDateUtil.parseGeneralizedTime(v);
        return d == null ? defaultValue : d;
    }

    /**
     *
     * @param name name of the attribute to retreive.
     * @param defaultValue value to use if attr is not present or can't be parsed.
     * @return
     */
    public int getIntAttr(String name, int defaultValue) {
        return getIntAttr(name, defaultValue, false);
    }

    protected int getIntAttr(String name, int defaultValue, boolean skipEphemeralCheck) {
        String v = getAttr(name, true, skipEphemeralCheck);
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
        return getLongAttr(name, defaultValue, false);
    }

    protected long getLongAttr(String name, long defaultValue, boolean skipEphemeralCheck) {
        String v = getAttr(name, true, skipEphemeralCheck);
        try {
            if (MemoryUnitUtil.isMemoryUnit(v))
                return new MemoryUnitUtil(1024).convertToBytes(v);
            else
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

    public List<byte[]> getMultiBinaryAttr(String name) {
        return getMultiBinaryAttr(name, true);
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

    private String objectToString(Object obj) {
        if (obj instanceof String) {
            return (String) obj;
        } else if (obj instanceof String[]) {
            String[] a = (String[]) obj;
            return a.length > 0 ? a[0] : null;
        } else {
            return null;
        }
    }

    private String[] objectToStringArray(Object obj) {
        if (obj instanceof String) {
            return new String[]{(String) obj};
        } else if (obj instanceof String[]) {
            return (String[]) obj;
        } else {
            return sEmptyMulti;
        }
    }

    /**
     * Returns the set of values for the given attribute, or an empty
     * array if no values are defined.
     */
    public String[] getMultiAttr(String name, boolean applyDefaults) {
        return getMultiAttr(name, applyDefaults, false);
    }

    public String[] getMultiAttr(String name, boolean applyDefaults, boolean skipEphemeralCheck) {
        if (!skipEphemeralCheck && mAttrMgr.isEphemeral(name)) {
            try {
                if (mAttrMgr.isDynamic(name)) {
                    ZimbraLog.ephemeral.warn("can't get value of dynamic ephemeral attribute %s without the dynamic component", name);
                    return sEmptyMulti;
                }
                //only returning max of one value here, since multi-valued ephemeral attributes have to be dynamic
                String value = getEphemeralAttr(name).getValue();
                if (value == null) {
                    if (applyDefaults) {
                        Object v = getAttrDefault(name);
                        return objectToStringArray(v);
                    } else {
                        return sEmptyMulti;
                    }
                } else {
                    return new String[] { value };
                }
            } catch (ServiceException e) {
                ZimbraLog.ephemeral.warn("error getting values for %s, returning default", name);
                if (applyDefaults) {
                    Object v = getAttrDefault(name);
                    return objectToStringArray(v);
                }
                else {
                    return sEmptyMulti;
                }
            }
        } else {
            Object v = getObject(name, applyDefaults);
            return objectToStringArray(v);
        }
    }

    public List<byte[]> getMultiBinaryAttr(String name, boolean applyDefaults) {
        String[] values = getMultiAttr(name, applyDefaults);
        if (values.length > 0) {
            List<byte[]> list = new ArrayList<byte[]>();
            for (String value : values) {
                list.add(ByteUtil.decodeLDAPBase64(value));
            }
            return list;
        } else {
            return sEmptyListMulti;
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


    public Set<byte[]> getMultiBinaryAttrSet(String name) {
        if (mMultiBinaryAttrSetCache == null)
            mMultiBinaryAttrSetCache = new HashMap<String, Set<byte[]>>();
        Set<byte[]> result = mMultiBinaryAttrSetCache.get(name);
        if (result == null) {
            result = new HashSet<byte[]>(getMultiBinaryAttr(name));
            mMultiBinaryAttrSetCache.put(name, result);
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
        return getTimeInterval(name, defaultValue, false);
    }

    protected long getTimeInterval(String name, long defaultValue, boolean skipEphemeralCheck) {
        String v = getAttr(name, true, skipEphemeralCheck);
        return DateUtil.getTimeInterval(v, defaultValue);
    }

    /**
     * get a time interval, which is a number, optional followed by a character denoting the units
     * (d = days, h = hours, m = minutes, s = seconds. If no character unit is specified,
     * the default is seconds.
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
     * temporarily associate a key/value pair with this entry. When an entry is reloaded,
     * any cached data is cleared via a call to resetData.
     *
     * @param key
     * @param value
     */
    public synchronized void setCachedData(String key, Object value) {
        if (mData == null)
            mData = new HashMap<String, Object>();
        mData.put(key, value);
    }

    /**
     * temporarily associate a key/value pair with this entry. When an entry is reloaded,
     * any cached data is cleared via a call to resetData.
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
        if (mData == null) {
            return null;
        }
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

    public synchronized void removeCachedData(EntryCacheDataKey key) {
        if (mData == null) {
            return;
        }
        mData.remove(key.getKeyName());
    }

    protected void getDefaults(AttributeFlag flag, Map<String,Object> defaults)
    throws ServiceException {
        defaults.clear();
        Set<String> attrs = AttributeManager.getInstance().getAttrsWithFlag(flag);
        for (String a : attrs) {
            Object obj = getObject(a, true);
            if (obj != null) defaults.put(a, obj);
        }
        //return Collections.unmodifiableMap(defaults);
    }

    @Override
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

    @Override
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


    private static final class SortByLabelAsc implements Comparator<Entry> {
        @Override public int compare(Entry m1, Entry m2) {
            return m1.getLabel().compareTo(m2.getLabel());
        }
    }

    /**
     * Sort a collection of Entries by locale-sensitive String comparison on
     * each entry's displayName.  If there is no display name, use entry.getLabel()
     * as the key.
     */
    public static List<Entry> sortByDisplayName(Collection<? extends Entry> entries, Locale locale) {
        List<Entry> sorted = Lists.newArrayList();

        // short-circuit if there is only one entry or no entry
        if (entries.size() <= 1) {
            sorted.addAll(entries);
        } else {
            Collator collator = Collator.getInstance(locale);

            TreeMultimap<CollationKey, Entry> map =
                TreeMultimap.create(Ordering.natural(), new SortByLabelAsc());

            for (Entry entry : entries) {
                String key = entry.getAttr(Provisioning.A_displayName);
                if (key == null) {
                    key = entry.getLabel();
                }
                CollationKey collationKey = collator.getCollationKey(key);

                map.put(collationKey, entry);
            }

            sorted.addAll(map.values());
        }
        return sorted;
    }

    public EphemeralResult getEphemeralAttr(String key) throws ServiceException {
        return getEphemeralAttr(key, null);
    }

    public EphemeralResult getEphemeralAttr(String key, String dynamicComponent) throws ServiceException {
        EphemeralLocation location = new LdapEntryLocation(this);
        EphemeralStore store = EphemeralStore.getFactory().getStore();
        EphemeralKey ephemeralKey = new EphemeralKey(key, dynamicComponent);
        EphemeralResult result = store.get(ephemeralKey, location);
        return result == null ? EphemeralResult.emptyResult(ephemeralKey) : result;
    }

    protected void deleteEphemeralAttr(String key) throws ServiceException {
        //The EphemeralStore API currently doesn't support deleting all values for a key,
        //but this method is only called by unsetters for single-valued non-dynamic ephemeral attributes,
        //which means we can first fetch the value and then delete it.
        EphemeralLocation location = new LdapEntryLocation(this);
        EphemeralStore store = EphemeralStore.getFactory().getStore();
        EphemeralKey ephemeralKey = new EphemeralKey(key);
        String curValue = store.get(ephemeralKey, location).getValue();
        store.delete(ephemeralKey, curValue, location);
    }

    public void deleteEphemeralAttr(String key, String dynamicComponent, String value) throws ServiceException {
        EphemeralLocation location = new LdapEntryLocation(this);
        EphemeralStore store = EphemeralStore.getFactory().getStore();
        store.delete(new EphemeralKey(key, dynamicComponent), value, location);
    }

    private void modifyEphemeralAttrInternal(String key, String dynamicComponent, String value, boolean update, Expiration expiration, EphemeralStore store, EphemeralLocation location) throws ServiceException {
        EphemeralInput input = new EphemeralInput(new EphemeralKey(key, dynamicComponent), value);
        if (expiration != null) {
            input.setExpiration(expiration);
        }
        if (update) {
            store.update(input, location);
        } else {
            store.set(input, location);
        }
    }

    public void modifyEphemeralAttr(EphemeralInput input, boolean update) throws ServiceException {
        EphemeralLocation location = new LdapEntryLocation(this);
        EphemeralStore store = EphemeralStore.getFactory().getStore();
        if (update) {
            store.update(input, location);
        }
        else {
            store.set(input, location);
        }
    }

    public void modifyEphemeralAttr(String key, String dynamicComponent, String value, boolean update, Expiration expiration) throws ServiceException {
        EphemeralLocation location = new LdapEntryLocation(this);
        EphemeralStore store = EphemeralStore.getFactory().getStore();
        modifyEphemeralAttrInternal(key, dynamicComponent, value, update, expiration, store, location);
    }

    public void modifyEphemeralAttr(String key, String dynamicComponent, String[] values, boolean update, Expiration expiration) throws ServiceException {
        EphemeralLocation location = new LdapEntryLocation(this);
        EphemeralStore store = EphemeralStore.getFactory().getStore();
        for (String value: values) {
            modifyEphemeralAttrInternal(key, dynamicComponent, value, update, expiration, store, location);
        }
    }

    protected long getEphemeralTimeInterval(String key, String dynamicComponent, long defaultValue) throws ServiceException {
        return DateUtil.getTimeInterval(getEphemeralAttr(key, dynamicComponent).getValue(), defaultValue);
    }

    public void purgeEphemeralAttr(String key) throws ServiceException {
        EphemeralLocation location = new LdapEntryLocation(this);
        EphemeralStore store = EphemeralStore.getFactory().getStore();
        store.purgeExpired(new EphemeralKey(key), location);
    }

    public boolean hasEphemeralAttr(String key, String dynamicComponent) throws ServiceException {
        EphemeralLocation location = new LdapEntryLocation(this);
        EphemeralStore store = EphemeralStore.getFactory().getStore();
        return store.has(new EphemeralKey(key, dynamicComponent), location);
    }
}
