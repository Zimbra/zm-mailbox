/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 VMware, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account;

import com.zimbra.common.service.ServiceException;

/**
 * Search options for account search.
 * 
 * Provides a convinient way for setting object types.
 * 
 * Callsites can use SearchAccountsOptions.setIncludeType() instead of 
 * SearchObjectsOptions.setTypes().  Default types for SearchAccountsOptions
 * is acounts and calendar resources,
 */
public class SearchAccountsOptions extends SearchDirectoryOptions {
    private static final SearchAccountsOptions.IncludeType DEFAULT_INCLUDE_TYPE = 
        IncludeType.ACCOUNTS_AND_CALENDAR_RESOURCES;
    
    public static enum IncludeType {
        ACCOUNTS_AND_CALENDAR_RESOURCES,
        ACCOUNTS_ONLY,
        CALENDAR_RESOURCES_ONLY;
    };
    
    private SearchAccountsOptions.IncludeType includeType;
            
    public SearchAccountsOptions() {
        initIncludeType();
    }
    
    public SearchAccountsOptions(Domain domain) {
        initIncludeType();
        setDomain(domain);
    }
    
    public SearchAccountsOptions(String[] returnAttrs) {
        initIncludeType();
        setReturnAttrs(returnAttrs);
    }

    public SearchAccountsOptions(Domain domain, String[] returnAttrs) {
        initIncludeType();
        setDomain(domain);
        setReturnAttrs(returnAttrs);
    }        
    
    private void initIncludeType() {
        setIncludeType(DEFAULT_INCLUDE_TYPE);
    }
    
    public void setIncludeType(SearchAccountsOptions.IncludeType includeType) {
        assert(includeType != null);
        
        this.includeType = includeType;
        switch (this.includeType) {
            case ACCOUNTS_AND_CALENDAR_RESOURCES:
                setTypesInternal(SearchDirectoryOptions.ObjectType.accounts, SearchDirectoryOptions.ObjectType.resources);
                break;
            case ACCOUNTS_ONLY:
                setTypesInternal(SearchDirectoryOptions.ObjectType.accounts);
                break;
            case CALENDAR_RESOURCES_ONLY:
                setTypesInternal(SearchDirectoryOptions.ObjectType.resources);
                break;
        }
    }
    
    public SearchAccountsOptions.IncludeType getIncludeType() {
        return includeType;
    }
    
    @Override
    public void setTypes(String typesStr) throws ServiceException {
        throw ServiceException.FAILURE("internal error, use setIncludeType instead", null);
    }
    
    @Override
    public void setTypes(SearchDirectoryOptions.ObjectType... objTypes) throws ServiceException {
        throw ServiceException.FAILURE("internal error, use setIncludeType instead", null);
    }
    
}