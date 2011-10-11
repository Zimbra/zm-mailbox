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