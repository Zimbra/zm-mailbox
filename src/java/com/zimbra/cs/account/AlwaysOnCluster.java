/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;


public class AlwaysOnCluster extends ZAttrAlwaysOnCluster {

    private final Map<String, Object> serverOverrides = new HashMap<String, Object>();

    public AlwaysOnCluster(String name, String id, Map<String,Object> attrs, Map<String,Object> defaults, Provisioning prov) {
        super(name, id, attrs, defaults, prov);
        try {
            getDefaults(AttributeFlag.serverPreferAlwaysOn, serverOverrides);
        } catch (ServiceException se) {
            ZimbraLog.account.warn("error while calculating server overrides", se);
        }
    }

    @Override
    public EntryType getEntryType() {
        return EntryType.ALWAYSONCLUSTER;
    }

    @Override
    public void resetData() {
        super.resetData();
        try {
            getDefaults(AttributeFlag.serverPreferAlwaysOn, serverOverrides);
        } catch (ServiceException e) {
            ZimbraLog.account.warn("error while calculating server overrides", e);
        }
    }

    public Map<String, Object> getServerOverrides() {
        return serverOverrides;
    }
}
