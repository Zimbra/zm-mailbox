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

package com.zimbra.cs.account.soap;

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
import com.zimbra.cs.util.DateUtil;

public abstract class SoapEntry implements Entry {

    private Map<String,Object> mAttrs;
    private Map<Object, Object> mData;

    private static String[] sEmptyMulti = new String[0];

    protected SoapEntry(Map<String,Object> attrs) {
        mAttrs = attrs;
    }
    
    public abstract void modifyAttrs(SoapProvisioning prov, Map<String, ? extends Object> attrs, boolean checkImmutable) throws ServiceException;
     
    public abstract void reload(SoapProvisioning prov) throws ServiceException;

    protected void setAttrs(Map<String,Object> attrs) {
        mAttrs = attrs;
        resetData();
    }
    
    protected void resetData()
    {
        if (mData != null)
            mData.clear();
    }

    /**
     * looks up name in map, and if found, returns its value.
     * if not found, iterate through key names and compare using equalsIgnoreCase
     * @param name
     * @return
     */
    private Object getObject(String name) {
        Object v = mAttrs.get(name);
        if (v != null) return v;
        
        for (String key: mAttrs.keySet()) {
            if (key.equalsIgnoreCase(name))
                return mAttrs.get(key);
        }
        return null;
    }
    
    public String getAttr(String name) {
        Object v = getObject(name);
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

    public Map<String, Object> getAttrs() throws ServiceException {
        return mAttrs;
    }

    public boolean getBooleanAttr(String name, boolean defaultValue) {
        String v = getAttr(name);
        return v == null ? defaultValue : Provisioning.TRUE.equals(v);
    }

    public synchronized Object getCachedData(Object key) {
        if (mData == null)
            mData = new HashMap<Object, Object>();
        return mData.get(key);
    }

    public Date getGeneralizedTimeAttr(String name, Date defaultValue) {
        String v = getAttr(name);
        if (v == null)
            return defaultValue;
        Date d = DateUtil.parseGeneralizedTime(v);
        return d == null ? defaultValue : d;
    }

    public int getIntAttr(String name, int defaultValue) {
        String v = getAttr(name);
        try {
            return v == null ? defaultValue : Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public Locale getLocale() throws ServiceException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("not implemented");
    }

    public long getLongAttr(String name, long defaultValue) {
        String v = getAttr(name);
        try {
            return v == null ? defaultValue : Long.parseLong(v);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public String[] getMultiAttr(String name) {
        Object v = getObject(name);
        if (v instanceof String) return new String[] {(String) v};
        else if (v instanceof String[]) {
            return (String[])v;
        } else {
            return sEmptyMulti;
        }
    }

    public Set<String> getMultiAttrSet(String name) {
        return new HashSet<String>(Arrays.asList(getMultiAttr(name)));
    }

    public long getTimeInterval(String name, long defaultValue) {
        return DateUtil.getTimeInterval(getAttr(name), defaultValue);        
    }

    public synchronized void setCachedData(Object key, Object value) {
        if (mData == null)
            mData = new HashMap<Object, Object>();
        mData.put(key, value);
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
