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
public class Cos extends ZAttrCos {
 
    private Map<String, Object> mAccountDefaults = new HashMap<String, Object>();

    public Cos(String name, String id, Map<String,Object> attrs, Provisioning prov) {
        super(name, id, attrs, prov);
        resetData();
    }
    
    @Override
    public EntryType getEntryType() {
        return EntryType.COS;
    }

    public void modify(Map<String, Object> attrs) throws ServiceException {
        getProvisioning().modifyAttrs(this, attrs);
    }

    public Cos copyCos(String destCosName) throws ServiceException {
        return getProvisioning().copyCos(getId(), destCosName);
    }

    public void renameCos(String newName) throws ServiceException {
        getProvisioning().renameCos(getId(), newName);
    }

    public void deleteCos() throws ServiceException {
        getProvisioning().deleteCos(getId());
    }

    @Override
    protected void resetData() {
        super.resetData();
        try {
            getDefaults(AttributeFlag.accountInherited, mAccountDefaults);
        } catch (ServiceException e) {
            // TODO log
        }
    }

    public Map<String, Object> getAccountDefaults() {
        return mAccountDefaults;
    }
    
    public boolean isDefaultCos() {
        return getName().equals(Provisioning.DEFAULT_COS_NAME) ||
                getName().equals(Provisioning.DEFAULT_EXTERNAL_COS_NAME);
    }
}
