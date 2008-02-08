/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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
 * Created on Sep 23, 2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.account;

import java.util.Map;

import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;

/**
 * @author schemers
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Domain extends NamedEntry {
    private String mUnicodeName;
 
    public Domain(String name, String id, Map<String, Object> attrs, Map<String, Object> defaults) {
        super(name, id, attrs, defaults);
        if (name == null)
            mUnicodeName = name;
        else
            mUnicodeName = IDNUtil.toUnicodeDomainName(name);
    }
    
    public String getUnicodeName() {
        return mUnicodeName;
    }
    
    public String getDomainStatus() {
        return getAttr(Provisioning.A_zimbraDomainStatus);
    }
    
    public boolean isSuspended() {
        String domainStatus = getDomainStatus();
        boolean suspended = false;
        if (domainStatus != null)
            suspended = domainStatus.equals(Provisioning.DOMAIN_STATUS_SUSPENDED);
        
        if (suspended)
            ZimbraLog.account.warn("domain " + mName + " is " + Provisioning.DOMAIN_STATUS_SUSPENDED);
        return suspended;
    }
    
    public boolean isShutdown() {
        String domainStatus = getDomainStatus();
        boolean shutdown = false;
        if (domainStatus != null)
            shutdown = domainStatus.equals(Provisioning.DOMAIN_STATUS_SHUTDOWN);
        
        if (shutdown)
            ZimbraLog.account.warn("domain " + mName + " is " + Provisioning.DOMAIN_STATUS_SHUTDOWN);
        return shutdown;
    }
    
    public boolean beingRenamed() {
        String renameInfo = getAttr(Provisioning.A_zimbraDomainRenameInfo);
        return (!StringUtil.isNullOrEmpty(renameInfo));
    }

}
