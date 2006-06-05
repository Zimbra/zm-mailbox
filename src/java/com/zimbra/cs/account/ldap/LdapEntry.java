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

/*
 * Created on Sep 23, 2004
 *
 */
package com.zimbra.cs.account.ldap;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InvalidAttributeIdentifierException;
import javax.naming.directory.InvalidAttributeValueException;
import javax.naming.directory.InvalidAttributesException;

import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.DateUtil;

/**
 * @author schemers
 *
 */
public class LdapEntry implements Entry {

    protected Attributes mAttrs;
    protected String mDn;
    private Map<String, String[]> mMultiAttrCache;
    private Map<String, Set<String>> mMultiAttrSetCache;
    private Map<Object, Object> mData;
    
    protected static final String[] sEmptyMulti = new String[0];

    LdapEntry(String dn, Attributes attrs) {
        mDn = dn;
        mAttrs = attrs;
    }

    public String getDN() {
        return mDn;
    }

    public synchronized String getAttr(String name) {
        try {
            return LdapUtil.getAttrString(mAttrs, name);
        } catch (NamingException e) {
            return null;
        }
    }

    public void reload() throws ServiceException {
        try {
            refresh(null, 0);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to refresh entry: "+mDn, e);
        }
    }
    
    private synchronized void refresh(DirContext initCtxt, long curr) throws NamingException, ServiceException {
        DirContext ctxt = initCtxt;
        try {
            if (ctxt == null)
                ctxt = LdapUtil.getDirContext();
            if (mMultiAttrCache != null)
                mMultiAttrCache.clear();
            if (mMultiAttrSetCache != null)            
                mMultiAttrSetCache.clear();
            if (mData != null)
                mData.clear();
            mAttrs = ctxt.getAttributes(mDn);
        } finally {
            if (initCtxt == null)
                LdapUtil.closeContext(ctxt);
        }
    }


    public void modifyAttrs(Map<String, ? extends Object> attrs) throws ServiceException {
        modifyAttrs(attrs, false);
    }

    public void modifyAttrs(Map<String, ? extends Object> attrs, boolean checkImmutable) throws ServiceException {
        HashMap context = new HashMap();
        AttributeManager.getInstance().preModify(attrs, this, context, false, checkImmutable);
        modifyAttrsInternal(null, attrs);
        AttributeManager.getInstance().postModify(attrs, this, context, false);
    }

  
    /**
     * should only be called internally.
     * 
     * @param initCtxt
     * @param attrs
     * @throws ServiceException
     */
    synchronized void modifyAttrsInternal(DirContext initCtxt, Map attrs) throws ServiceException {
        DirContext ctxt = initCtxt;
        try {
            if (ctxt == null)
                ctxt = LdapUtil.getDirContext(true);
            LdapUtil.modifyAttrs(ctxt, mDn,  attrs, mAttrs);
            refresh(ctxt, 0);
        } catch (InvalidAttributeIdentifierException e) {
            throw AccountServiceException.INVALID_ATTR_NAME("invalid attr name", e);
        } catch (InvalidAttributeValueException e) {
            throw AccountServiceException.INVALID_ATTR_VALUE("invalid attr value", e);            
        } catch (InvalidAttributesException e) {
            throw ServiceException.INVALID_REQUEST("invalid set of attributes", e);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to modify attrs", e);
        } finally {
            if (initCtxt == null)
                LdapUtil.closeContext(ctxt);
        }
    }

    protected synchronized void addAttr(DirContext ctxt, String name, String value) throws NamingException, ServiceException {
        LdapUtil.addAttr(ctxt, mDn,  name, value);
        refresh(ctxt, 0);
    }
    
    protected synchronized void removeAttr(DirContext ctxt, String name, String value) throws NamingException, ServiceException {
        LdapUtil.removeAttr(ctxt, mDn,  name, value);
        refresh(ctxt, 0);        
    }
    
    public synchronized void setCachedData(Object key, Object value) {
        if (mData == null)
            mData = new HashMap<Object, Object>();
        mData.put(key, value);
    }

    public synchronized Object getCachedData(Object key) {
        if (mData == null)
            mData = new HashMap<Object, Object>();
        return mData.get(key);
    }

    public synchronized String[] getMultiAttr(String name) {
        if (mMultiAttrCache == null)
            mMultiAttrCache = new HashMap<String, String[]>();
        try {
            String[] result = mMultiAttrCache.get(name);
            if (result == null) {
                result = LdapUtil.getMultiAttrString(mAttrs, name); 
                if (result != null)
                    mMultiAttrCache.put(name, result);
            }
            return result;
        } catch (NamingException e) {
            return sEmptyMulti;
        }        
    }

    public synchronized Set<String> getMultiAttrSet(String name) {
        if (mMultiAttrSetCache == null)        
            mMultiAttrSetCache = new HashMap<String, Set<String>>();        
        Set<String> result = mMultiAttrSetCache.get(name);
        if (result == null) {
            result =  new HashSet<String>(Arrays.asList(getMultiAttr(name)));
            mMultiAttrSetCache.put(name, result);
        }
        return result;        
    }

    public boolean getBooleanAttr(String name, boolean defaultValue) {
        String v = getAttr(name);
        return v == null ? defaultValue : Provisioning.TRUE.equals(v);
    }

    public int getIntAttr(String name, int defaultValue) {
        String v = getAttr(name);
        try {
            return v == null ? defaultValue : Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public long getTimeInterval(String name, long defaultValue) {
        return DateUtil.getTimeInterval(getAttr(name), defaultValue);
    }
    
    public long getLongAttr(String name, long defaultValue) {
        String v = getAttr(name);
        try {
            return v == null ? defaultValue : Long.parseLong(v);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public String getAttr(String name, String defaultValue) {
        String v = getAttr(name);
        return v == null ? defaultValue : v; 
    }

    public Date getGeneralizedTimeAttr(String name, Date defaultValue) {    
        String v = getAttr(name);
        if (v == null)
            return defaultValue;
        Date d = DateUtil.parseGeneralizedTime(v);
        return d == null ? defaultValue : d;
    }

    public synchronized Map<String, Object> getAttrs() throws ServiceException {
        HashMap<String, Object> attrs = new HashMap<String, Object>();
        try {
            LdapUtil.getAttrs(mAttrs, attrs, null);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to get all attrs", e);
        }
        return attrs;
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

    public synchronized String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getName()).append(": { dn=").append(mDn).append(" ");
        sb.append(mAttrs.toString());
        sb.append("}");
        return sb.toString();    	    
	}
}
