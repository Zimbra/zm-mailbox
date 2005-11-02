/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
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

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
import com.zimbra.cs.service.ServiceException;

/**
 * @author schemers
 *
 */
public class LdapEntry implements Entry {

    protected Attributes mAttrs;
    protected String mDn;
    private Map mMultiAttrCache;
    private Map mMultiAttrSetCache;
    private Map mData;
    private long mLoadtime;
    
    protected static final String[] sEmptyMulti = new String[0];

    LdapEntry(String dn, Attributes attrs) {
        mDn = dn;
        mAttrs = attrs;
        mLoadtime = System.currentTimeMillis();
    }

    public String getDN() {
        return mDn;
    }

    synchronized Attributes getRawAttrs() {
        Attributes attrs = (Attributes) mAttrs.clone();
        return attrs;
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

    public boolean isStale(long ageInMillis) {
        return mLoadtime + ageInMillis < System.currentTimeMillis(); 
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
            mLoadtime = curr != 0 ? curr : System.currentTimeMillis();
        } finally {
            if (initCtxt == null)
                LdapUtil.closeContext(ctxt);
        }
    }


    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Account#modifyAccount(java.util.Map)
     */
    public void modifyAttrs(Map attrs) throws ServiceException {
        modifyAttrs(attrs, false);
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Account#modifyAccount(java.util.Map)
     */
    public void modifyAttrs(Map attrs, boolean checkImmutable) throws ServiceException {
        HashMap context = new HashMap();
        AttributeManager.getInstance().preModify(attrs, this, context, false, checkImmutable);
        modifyAttrsInternal(null, attrs);
        AttributeManager.getInstance().postModify(attrs, this, context, false);
    }

  
    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Account#modifyAccount(java.util.Map)
     */
    private void modifyAttrsInternal(Map attrs) throws ServiceException {
        modifyAttrsInternal(null, attrs);
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
                ctxt = LdapUtil.getDirContext();
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

    public void setBooleanAttr(String name, boolean value) throws ServiceException {
        HashMap attrs = new HashMap(1);
        attrs.put(name, value ? LdapUtil.LDAP_TRUE : LdapUtil.LDAP_FALSE);
        modifyAttrs(attrs);
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
            mData = new HashMap();
        mData.put(key, value);
    }

    public synchronized Object getCachedData(Object key) {
        if (mData == null)
            mData = new HashMap();
        return mData.get(key);
    }

    public synchronized String[] getMultiAttr(String name) {
        if (mMultiAttrCache == null)
            mMultiAttrCache = new HashMap();
        try {
            String[] result = (String[]) mMultiAttrCache.get(name);
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

    public synchronized Set getMultiAttrSet(String name) {
        if (mMultiAttrSetCache == null)        
            mMultiAttrSetCache = new HashMap();        
        Set result = (Set) mMultiAttrSetCache.get(name);
        if (result == null) {
            result = new HashSet();            
            String values[] = getMultiAttr(name);
            if (values != null && values.length != 0) {
                for (int i=0; i < values.length; i++) {
                    result.add(values[i]);
                }
                mMultiAttrSetCache.put(name, result);
            }
        }
        return result;        
    }

    public boolean getBooleanAttr(String name, boolean defaultValue) {
        String v = getAttr(name);
        if (v == null)
            return defaultValue;
        else 
            return LdapUtil.LDAP_TRUE.equals(v);
    }

    public int getIntAttr(String name, int defaultValue) {
        String v = getAttr(name);
        if (v == null)
            return defaultValue;
        else {
            try {
                return Integer.parseInt(v);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
    }

    public long getTimeInterval(String name, long defaultValue) throws AccountServiceException {
        String v = getAttr(name);
        if (v == null || v.length() == 0)
            return defaultValue;
        else {
            try {
                char units = v.charAt(v.length()-1);
                if (units >= '0' && units <= '9') {
                    return Long.parseLong(v)*1000;
                } else {
                    long n = Long.parseLong(v.substring(0, v.length()-1));
                    switch (units) {
                    case 'd':
                        n = n * (1000*60*60*24);
                        break;
                    case 'h':
                        n = n * (1000*60*60);
                        break;
                    case 'm':
                        n = n * (1000*60);
                        break;
                    case 's':
                        n = n * (1000);
                        break;
                    default:
                        throw AccountServiceException.INVALID_ATTR_VALUE("unable to parse duration: "+v, null);
                    }
                    return n;
                }
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
    }
    
    public long getLongAttr(String name, long defaultValue) {
        String v = getAttr(name);
        if (v == null)
            return defaultValue;
        else {
            try {
                return Long.parseLong(v);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
    }

    public String getAttr(String name, String defaultValue) {
        String v = getAttr(name);
        if (v == null)
            return defaultValue;
        else
            return v;
    }

    public Date getGeneralizedTimeAttr(String name, Date defaultValue) {    
        String v = getAttr(name);
        if (v == null)
            return defaultValue;
        Date d = LdapUtil.generalizedTime(v);
        return d == null ? defaultValue : d;
    }

    public synchronized Map getAttrs() throws ServiceException {
        HashMap attrs = new HashMap();
        try {
            LdapUtil.getAttrs(mAttrs, attrs, null);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to get all attrs", e);
        }
        return attrs;
    }
    
	public synchronized String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getName()).append(": { dn=").append(mDn).append(" ");
        sb.append(mAttrs.toString());
        sb.append("}");
        return sb.toString();    	    
	}
}
