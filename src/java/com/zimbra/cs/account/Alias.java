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
 * Created on Nov 24, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.account;

import java.util.List;
import java.util.Map;

import com.zimbra.cs.account.Provisioning.SearchOptions;
import com.zimbra.common.service.ServiceException;

/**
 * @author anandp
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Alias extends NamedEntry {
    
    private static final String ALIAS_TARGET_NAME = "targetName";
    private static final String ALIAS_TARGET_TYPE = "targetType";

    public Alias(String name, String id, Map<String, Object> attrs) {
        super(name, id, attrs, null);
    }
    
    private NamedEntry searchTarget(boolean mustFind) throws ServiceException {
        String targetId = getAttr(Provisioning.A_zimbraAliasTargetId);
        SearchOptions options = new SearchOptions();
    
        int flags = 0;

        flags |= Provisioning.SA_ACCOUNT_FLAG;
        flags |= Provisioning.SA_CALENDAR_RESOURCE_FLAG;
        flags |= Provisioning.SA_DISTRIBUTION_LIST_FLAG;
            
        String query = "(" + Provisioning.A_zimbraId + "=" + targetId + ")";
    
        options.setFlags(flags);
        options.setQuery(query);
    
        List<NamedEntry> entries = Provisioning.getInstance().searchDirectory(options);
        
        if (mustFind && entries.size() == 0)
            throw ServiceException.FAILURE("target " + targetId + " of alias " +  getName() + " not found " + query, null);
        
        if (entries.size() > 1)
            throw AccountServiceException.TOO_MANY_SEARCH_RESULTS("too many results for search " + query, null);

        if (entries.size() == 0)
            return null;
        else
            return entries.get(0);
    }
    
    private String getTargetInfo(String forInfo) throws ServiceException {
        Object data = getCachedData(forInfo);
        if (data != null)
            return (String)data;
        
        /*
         * not cached, search directory and put info in cache
         */
        NamedEntry entry = searchTarget(true);
        
        String targetName = entry.getName();
        String targetType;
        
        if (entry instanceof CalendarResource)
            targetType = "resource";
        else if (entry instanceof Account)
            targetType = "account";
        else if (entry instanceof DistributionList)
            targetType = "distributionlist";
        else
            throw ServiceException.FAILURE("invalid target type for alias " + getName(), null);
        
        /*
         * put targetName and targetType in cache
         */
        setCachedData(ALIAS_TARGET_NAME, targetName);
        setCachedData(ALIAS_TARGET_TYPE, targetType);
        
        if (forInfo == ALIAS_TARGET_NAME)
            return targetName;
        else 
            return targetType;
    }

    public String getTargetName() throws ServiceException {
        return getTargetInfo(ALIAS_TARGET_NAME);
    }
    
    public String getTargetType() throws ServiceException {
        return getTargetInfo(ALIAS_TARGET_TYPE);
    }
    
    public boolean isDangling() throws ServiceException {
        NamedEntry entry = searchTarget(false);
        return (entry == null);
    }

}
