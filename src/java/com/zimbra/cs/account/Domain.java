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

/*
 * Created on Sep 23, 2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.account;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning.GAL_SEARCH_TYPE;
import com.zimbra.cs.account.Provisioning.GalMode;
import com.zimbra.cs.account.Provisioning.SearchGalResult;
import com.zimbra.cs.account.ZAttrProvisioning.DomainStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author schemers
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Domain extends ZAttrDomain {
    private String mUnicodeName;
    private Map<String, Object> mAccountDefaults = new HashMap<String, Object>();
    
    public Domain(String name, String id, Map<String, Object> attrs, Map<String, Object> defaults, Provisioning prov) {
        super(name, id, attrs, defaults, prov);
        if (name == null)
            mUnicodeName = name;
        else
            mUnicodeName = IDNUtil.toUnicodeDomainName(name);
        resetData();
    }

    public void modify(Map<String, Object> attrs) throws ServiceException {
        getProvisioning().modifyAttrs(this, attrs);
    }

    public void deleteDomain(String zimbraId) throws ServiceException {
        getProvisioning().deleteDomain(getId());
    }
    
    public List getAllAccounts() throws ServiceException {
            return getProvisioning().getAllAccounts(this);
    }

    public void getAllAccounts(NamedEntry.Visitor visitor) throws ServiceException {
        getProvisioning().getAllAccounts(this, visitor);
    }

    public void getAllAccounts(Server s, NamedEntry.Visitor visitor) throws ServiceException {
        getProvisioning().getAllAccounts(this, s, visitor);
    }

    public List getAllCalendarResources() throws ServiceException {
        return getProvisioning().getAllCalendarResources(this);
    }

    public void getAllCalendarResources(NamedEntry.Visitor visitor) throws ServiceException {
        getProvisioning().getAllCalendarResources(this, visitor);
    }

    public void getAllCalendarResources(Server s, NamedEntry.Visitor visitor) throws ServiceException {
        getProvisioning().getAllCalendarResources(this, s, visitor);
    }

    public List getAllDistributionLists() throws ServiceException {
        return getProvisioning().getAllDistributionLists(this);
    }

    public List<NamedEntry> searchAccounts(String query, String returnAttrs[], String sortAttr, boolean sortAscending, int flags) throws ServiceException {
        return getProvisioning().searchAccounts(this, query, returnAttrs, sortAttr, sortAscending, flags);
    }

    public List searchCalendarResources(EntrySearchFilter filter, String returnAttrs[], String sortAttr, boolean sortAscending) throws ServiceException {
        return getProvisioning().searchCalendarResources(this, filter, returnAttrs, sortAttr, sortAscending);
    }

    public SearchGalResult searchGal(String query, GAL_SEARCH_TYPE type, String token) throws ServiceException {
        return getProvisioning().searchGal(this, query, type, token);
    }

    public SearchGalResult searchGal(String query, GAL_SEARCH_TYPE type, String token, GalContact.Visitor visitor) throws ServiceException {
        return getProvisioning().searchGal(this, query, type, token, visitor);
    }

    public SearchGalResult searchGal(String query, GAL_SEARCH_TYPE type, GalMode mode, String token) throws ServiceException {
        return getProvisioning().searchGal(this, query, type, mode, token);
    }

    @Override
    protected void resetData() {
        super.resetData();
        try {
            getDefaults(AttributeFlag.accountCosDomainInherited, mAccountDefaults);
        } catch (ServiceException e) {
            // TODO log
        }
    }
    
    public Map<String, Object> getAccountDefaults() {
        return mAccountDefaults;
    }
    
    public String getUnicodeName() {
        return mUnicodeName;
    }

    public boolean isSuspended() {
        DomainStatus status = getDomainStatus();
        boolean suspended = status != null && status.isSuspended();

        if (suspended)
            ZimbraLog.account.warn("domain " + mName + " is " + Provisioning.DOMAIN_STATUS_SUSPENDED);
        return suspended;
    }
    
    public boolean isShutdown() {
        DomainStatus status = getDomainStatus();
        boolean shutdown = status != null && status.isShutdown();
        
        if (shutdown)
            ZimbraLog.account.warn("domain " + mName + " is " + Provisioning.DOMAIN_STATUS_SHUTDOWN);
        return shutdown;
    }
    
    public boolean beingRenamed() {
        String renameInfo = getAttr(Provisioning.A_zimbraDomainRenameInfo);
        return (!StringUtil.isNullOrEmpty(renameInfo));
    }

}
