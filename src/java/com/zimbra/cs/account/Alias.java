/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
public class Alias extends MailTarget {
    
    private static final String ALIAS_TARGET_NAME = "targetName";
    private static final String ALIAS_TARGET_TYPE = "targetType";

    public Alias(String name, String id, Map<String, Object> attrs, Provisioning prov) {
        super(name, id, attrs, null, prov);
    }
    
    private String getTargetInfo(Provisioning prov, String forInfo) throws ServiceException {
        Object data = getCachedData(forInfo);
        if (data != null)
            return (String)data;
        
        /*
         * not cached, search directory and put info in cache
         */
        NamedEntry entry = prov.searchAliasTarget(this, true);
        
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

    public String getTargetName(Provisioning prov) throws ServiceException {
        return getTargetInfo(prov, ALIAS_TARGET_NAME);
    }
    
    public String getTargetUnicodeName(Provisioning prov) throws ServiceException {
        String targetName =  getTargetInfo(prov, ALIAS_TARGET_NAME);
        return IDNUtil.toUnicodeEmail(targetName);
    }
    
    public String getTargetType(Provisioning prov) throws ServiceException {
        return getTargetInfo(prov, ALIAS_TARGET_TYPE);
    }
    
    public boolean isDangling(Provisioning prov) throws ServiceException {
        NamedEntry entry = prov.searchAliasTarget(this, false);
        return (entry == null);
    }

}
