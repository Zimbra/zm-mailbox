/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.gal;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.extension.ExtensionUtil;
import com.zimbra.cs.gal.GalGroup.GroupInfo;

public class GalGroupInfoProvider {

    private static GalGroupInfoProvider instance;
    
    public synchronized static GalGroupInfoProvider getInstance() {
        if (instance == null) {
            instance = makeInstance();
        }
        return instance;
    }

    private static GalGroupInfoProvider makeInstance() {
        GalGroupInfoProvider provider = null;
        String className = LC.zimbra_class_galgroupinfoprovider.value();
        if (className != null && !className.equals("")) {
            try {
                try {
                    provider = (GalGroupInfoProvider) Class.forName(className).newInstance();
                } catch (ClassNotFoundException cnfe) {
                    // ignore and look in extensions
                    provider = (GalGroupInfoProvider) ExtensionUtil.findClass(className).newInstance();
                }
            } catch (Exception e) {
                ZimbraLog.account.error("could not instantiate GalGroupInfoProvider interface of class '" + className + "'; defaulting to GalGroupInfoProvider", e);
            }
        }
        if (provider == null)
            provider = new GalGroupInfoProvider();
        return provider;
    }
    
    public GroupInfo getGroupInfo(String addr, boolean needCanExpand, Account requestedAcct, Account authedAcct) {
        return GalGroup.getGroupInfo(addr, true, requestedAcct, authedAcct);
    }

}
