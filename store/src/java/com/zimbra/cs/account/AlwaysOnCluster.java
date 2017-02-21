/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2016 Synacor, Inc.
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
