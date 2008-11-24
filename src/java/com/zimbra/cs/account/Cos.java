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
public class Cos extends ZAttrCos {
 
    private Map<String, Object> mAccountDefaults = new HashMap<String, Object>();

    public Cos(String name, String id, Map<String,Object> attrs, Provisioning prov) {
        super(name, id, attrs, prov);
        resetData();
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
}
