/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
    
    @Override
    public EntryType getEntryType() {
        return EntryType.GLOBALCONFIG;
    }
    
    @Override
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
