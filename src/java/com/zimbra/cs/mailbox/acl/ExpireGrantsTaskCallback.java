/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.mailbox.acl;

import com.zimbra.common.util.ScheduledTaskCallback;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.ScheduledTaskResult;

import java.util.concurrent.Callable;

/**
 * After the execution of a {@link ExpireGrantsTask} for a mail item, we need to schedule the next
 * {@link ExpireGrantsTask} for that item.
 */
public class ExpireGrantsTaskCallback implements ScheduledTaskCallback<ScheduledTaskResult> {

    public void afterTaskRun(Callable<ScheduledTaskResult> task, ScheduledTaskResult lastResult) {
        if (lastResult == null) {
            return;
        }
        if (task instanceof ExpireGrantsTask) {
            ZimbraLog.scheduler.debug("afterTaskRun() for %s", task);
            ShareExpirationListener.scheduleExpireAccessOpIfReq((MailItem) lastResult);
        }
    }
}
