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

package com.zimbra.cs.account.ldap;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeFlag;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.WellKnownTimeZone;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.service.ServiceException;

/**
 * @author schemers
 */
public class LdapAccount extends LdapNamedEntry implements Account {

    private static final String DATA_DL_SET = "DL_SET";
    
    protected LdapProvisioning mProv;
    private String mName;
    private String mDomainName;
    
    LdapAccount(String dn, Attributes attrs, LdapProvisioning prov) {
        super(dn, attrs);
        mProv = prov;
        initNameAndDomain();
    }

    public String getId() {
        return super.getAttr(Provisioning.A_zimbraId);
    }

    public String getUid() {
        return super.getAttr(Provisioning.A_uid);
    }
    
    public String getName() {
        return mName;
    }

    public boolean inDistributionList(String zimbraId) throws ServiceException {
        return getDistributionLists().contains(zimbraId);
    }

    public Set<String> getDistributionLists() throws ServiceException {      
        Set<String> dls = (Set<String>) getCachedData(DATA_DL_SET);
        if (dls != null) return dls;
     
        dls = new HashSet<String>();
        
        List<DistributionList> lists = getDistributionLists(false, null, true);
        
        for (DistributionList dl : lists) {
            dls.add(dl.getId());
        }
        dls = Collections.unmodifiableSet(dls);
        setCachedData(DATA_DL_SET, dls);
        return dls;
    }
    
    /**
     * @return the domain name for this account (foo.com), or null if an admin account. 
     */
    public String getDomainName() {
        return mDomainName;
    }

    private void initNameAndDomain() {
        String uid = getUid(); 
        mDomainName = LdapUtil.dnToDomain(mDn);
        if (!mDomainName.equals("")) {
            mName =  uid+"@"+mDomainName;
        } else {
            mName = uid;
            mDomainName = null;
        }
    }

    public String getAccountStatus() {
        return super.getAttr(Provisioning.A_zimbraAccountStatus);
    }
     
    public String getAttr(String name) {
        String v = super.getAttr(name);
        if (v != null)
            return v;
        try {
            if (!AttributeManager.getInstance().isAccountInherited(name))
                return null;
            Cos cos = mProv.getCOS(this);
            if (cos != null)
                return cos.getAttr(name);
            else
                return null;            
        } catch (ServiceException e) {
            return null;
        }
    }

    public String[] getMultiAttr(String name) {
        String v[] = super.getMultiAttr(name);
        if (v.length > 0)
            return v;
        try {
            if (!AttributeManager.getInstance().isAccountInherited(name))
                return sEmptyMulti;

            Cos cos = mProv.getCOS(this);
            if (cos != null)
                return cos.getMultiAttr(name);
            else
                return sEmptyMulti;
        } catch (ServiceException e) {
            return null;
        }
    }
    
    public String[] getAliases() {
        return getMultiAttr(Provisioning.A_zimbraMailAlias);
    }
    
    public String getAccountCOSId() {
        return super.getAttr(Provisioning.A_zimbraCOSId);
    }

    public Map getPrefs() throws ServiceException {
        Map<String, Object> prefs = new HashMap<String, Object>();
        try {
            LdapCos cos = (LdapCos) mProv.getCOS(this);
            // get the COS prefs first
            LdapUtil.getAttrs(cos.mAttrs, prefs, "zimbraPref");
            // and override with the account ones
            LdapUtil.getAttrs(mAttrs, prefs, "zimbraPref");
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to get prefs", e);
        }
        return prefs;
    }
    
    public Map<String, Object> getAttrs() throws ServiceException {
        return getAttrs(false, true);
    }

    public Map<String, Object> getAttrs(boolean prefsOnly, boolean applyCos) throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        try {
            // get all the account attrs
            LdapUtil.getAttrs(mAttrs, attrs, prefsOnly ? "zimbraPref" : null);
            
            if (!applyCos)
                return attrs;
            
            // then enumerate through all inheritable attrs and add them if needed
            Set<String> inheritable =  AttributeManager.getInstance().getAttrsWithFlag(AttributeFlag.accountInherited);
            for (String attr : inheritable) {
                if (!prefsOnly || attr.startsWith("zimbraPref")) {
                    Object value = attrs.get(inheritable);
                    if (value == null)
                        value = getMultiAttr(attr);
                    if (value != null)
                        attrs.put(attr, value);
                }
            }
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to get prefs", e);
        }
        return attrs;
    }

    private ICalTimeZone mTimeZone;

    public synchronized ICalTimeZone getTimeZone() throws ServiceException {
        String tzId = getAttr(Provisioning.A_zimbraPrefTimeZoneId);
        if (tzId == null) {
        	if (mTimeZone != null)
                return mTimeZone;
            mTimeZone = ICalTimeZone.getUTC();
            return mTimeZone;
        }

        if (mTimeZone != null) {
            if (mTimeZone.getID().equals(tzId))
                return mTimeZone;
            // Else the account's time zone was updated.  Discard the cached
            // ICalTimeZone object.
        }

    	WellKnownTimeZone z = Provisioning.getInstance().getTimeZoneById(tzId);
        if (z != null)
            mTimeZone = z.toTimeZone();
        if (mTimeZone == null)
            mTimeZone = ICalTimeZone.getUTC();
        return mTimeZone;
    }

    public CalendarUserType getCalendarUserType() {
        String cutype = getAttr(Provisioning.A_zimbraAccountCalendarUserType,
                                CalendarUserType.USER.toString());
        return CalendarUserType.valueOf(cutype);
    }

    public boolean saveToSent() {
        return getBooleanAttr(Provisioning.A_zimbraPrefSaveToSent, false);
    }

    public List<DistributionList> getDistributionLists(boolean directOnly, Map<String, String> via) throws ServiceException {
        return getDistributionLists(directOnly, via, false);
    }
    
    private List<DistributionList> getDistributionLists(boolean directOnly, Map<String, String> via, boolean minimal) throws ServiceException {
        String aliases[] = this.getAliases();
        String addrs[] = new String[aliases.length+1];
        addrs[0] = this.getName();
        for (int i=0; i < aliases.length; i++)
            addrs[i+1] = aliases[i];
        return LdapProvisioning.getDistributionLists(addrs, directOnly, via, minimal);
    }
}
