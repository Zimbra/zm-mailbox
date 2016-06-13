/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.mailbox.MessageCache;
import com.zimbra.cs.store.BlobInputStream;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.util.JMSession;
import com.zimbra.cs.util.Zimbra;

/**
 * Central place for updating server attributes that we cache in memory.
 */
public class ServerConfig extends AttributeCallback {

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
        
        // do not run this callback unless inside the server
        if (!Zimbra.started())
            return;
        
        try {
            if (attrName.equals(Provisioning.A_zimbraMailUncompressedCacheMaxBytes) ||
                attrName.equals(Provisioning.A_zimbraMailUncompressedCacheMaxFiles) ||
                attrName.equals(Provisioning.A_zimbraMailFileDescriptorCacheSize)) {
                BlobInputStream.getFileDescriptorCache().loadSettings();
            } else if (attrName.equals(Provisioning.A_zimbraMailDiskStreamingThreshold)) {
                StoreManager.loadSettings();
            } else if (attrName.equals(Provisioning.A_zimbraMessageCacheSize)) {
                MessageCache.loadSettings();
            } else if (attrName.equals(Provisioning.A_zimbraSmtpHostname)) {
                JMSession.resetSmtpHosts();
            } else if (attrName.equals(Provisioning.A_zimbraDatabaseSlowSqlThreshold)) {
                DbPool.loadSettings();
            }
        } catch (ServiceException e) {
            ZimbraLog.account.warn("Unable to update %s.", attrName, e);
        }
    }

    @Override
    public void preModify(CallbackContext context, String attrName, Object attrValue,
            Map attrsToModify, Entry entry)
    throws ServiceException {
    }

}
