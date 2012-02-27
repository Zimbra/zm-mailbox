/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
