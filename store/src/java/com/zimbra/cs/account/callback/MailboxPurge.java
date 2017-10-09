/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailbox.PurgeThread;

/**
 * Starts the mailbox purge thread if it is not running and the purge sleep
 * interval is set to a non-zero value.  
 */
public class MailboxPurge extends AttributeCallback {

    @Override
    public void preModify(CallbackContext context, String attrName, Object attrValue, 
            Map attrsToModify, Entry entry) {
    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
        Server localServer = CallbackUtil.verificationBeforeStartingThread(Provisioning.A_zimbraMailPurgeSleepInterval, attrName, entry, "Mailbox Purge");
        if (localServer == null) {
            return;
        }

        ZimbraLog.purge.info("Mailbox purge interval set to %s.",
            localServer.getAttr(Provisioning.A_zimbraMailPurgeSleepInterval, null));
        long interval = localServer.getTimeInterval(Provisioning.A_zimbraMailPurgeSleepInterval, 0);
        if (interval > 0 && !PurgeThread.isRunning()) {
            PurgeThread.startup();
        }
        if (interval == 0 && PurgeThread.isRunning()) {
            PurgeThread.shutdown();
        }
    }
}
