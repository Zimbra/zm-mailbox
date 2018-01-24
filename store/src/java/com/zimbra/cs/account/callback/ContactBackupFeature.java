/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailbox.ContactBackupThread;

public class ContactBackupFeature extends AttributeCallback  {

    @SuppressWarnings("rawtypes")
    @Override
    public void preModify(CallbackContext context, String attrName, Object attrValue, Map attrsToModify, Entry entry)
            throws ServiceException {
        // empty method
    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
        Server localServer = CallbackUtil.verificationBeforeStartingThread(Provisioning.A_zimbraFeatureContactBackupFrequency, attrName, entry, "Contact Backup Feature");
        if (localServer == null) {
            return;
        }
        long interval = localServer.getTimeInterval(Provisioning.A_zimbraFeatureContactBackupFrequency, 0);
        ZimbraLog.contactbackup.info("Contact backup interval set to %d.", interval);
        if (interval > 0) {
            if (ContactBackupThread.isRunning()) {
                ContactBackupThread.shutdown();
            }
            ContactBackupThread.startup();
        }
        if (interval == 0 && ContactBackupThread.isRunning()) {
            ContactBackupThread.shutdown();
        }
    }
}