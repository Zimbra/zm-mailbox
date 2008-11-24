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

import com.zimbra.common.service.ServiceException;

import java.util.HashMap;
import java.util.Map;

/**
 * @author schemers
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Config extends ZAttrConfig {
    
    private Map<String, Object> mDomainDefaults = new HashMap<String, Object>();
    private Map<String, Object> mServerDefaults = new HashMap<String, Object>();    

    public Config(Map<String, Object> attrs, Provisioning provisioning) {
        super(attrs, provisioning);
        resetData();
    }
    
    public String getLabel() {
        return "globalconfig";
    }

    public void modify(Map<String, Object> attrs) throws ServiceException {
        getProvisioning().modifyAttrs(this, attrs);
    }

    @Override
    public void resetData() {
        super.resetData();
        try {
            getDefaults(AttributeFlag.domainInherited, mDomainDefaults);
            getDefaults(AttributeFlag.serverInherited, mServerDefaults);            
        } catch (ServiceException e) {
            // TODO log?
        }
    }
    
    @SuppressWarnings("unchecked")
    public Map<String, Object> getDomainDefaults() {
        return mDomainDefaults;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getServerDefaults() {
        return mServerDefaults;
    }

}
