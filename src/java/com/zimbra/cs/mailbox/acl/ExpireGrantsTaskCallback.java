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
