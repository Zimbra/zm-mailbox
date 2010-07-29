package com.zimbra.cs.mailbox.alerts;

import com.zimbra.common.util.ScheduledTaskCallback;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.ScheduledTaskResult;

import java.util.concurrent.Callable;

/**
 * @author vmahajan
 */
public class CalItemReminderTaskCallback implements ScheduledTaskCallback<ScheduledTaskResult> {

    public void afterTaskRun(Callable<ScheduledTaskResult> task, ScheduledTaskResult mLastResult) {
        if (task instanceof CalItemReminderTask && mLastResult != null) {
            if (ZimbraLog.scheduler.isDebugEnabled())
                ZimbraLog.scheduler.debug(getClass().getName() + ".afterTaskRun() for " + CalItemReminderTask.class.getName());
            CalItemReminderService.scheduleNextReminder((CalendarItem) mLastResult, true);
        }
    }
}
