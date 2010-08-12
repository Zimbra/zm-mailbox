package com.zimbra.cs.mailbox.alerts;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxListener;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.ScheduledTaskManager;
import com.zimbra.cs.session.PendingModifications;

import java.util.Date;
import java.util.Map;

/**
 * @author vmahajan
 */
public class CalItemReminderService extends MailboxListener {

    public void handleMailboxChange(String accountId, PendingModifications mods, OperationContext octxt, int lastChangeId) {
        Account account = null;
        try {
            account = Provisioning.getInstance().getAccountById(accountId);
        } catch (ServiceException e) {
            ZimbraLog.scheduler.error("Error in getting account", e);
        }
        if (account == null) {
            ZimbraLog.scheduler.error("Account not found for id " + accountId);
            return;
        }
        if (!account.getBooleanAttr(Provisioning.A_zimbraPrefCalendarReminderSendEmail, false)) {
            // calendar reminders not enabled for this account
            return;
        }
        if (mods.created != null) {
            for (Map.Entry<PendingModifications.ModificationKey, MailItem> entry : mods.created.entrySet()) {
                MailItem item = entry.getValue();
                if (item instanceof Appointment) {
                    if (ZimbraLog.scheduler.isDebugEnabled())
                        ZimbraLog.scheduler.debug("Handling creation of calendar item (id=" + item.getId() + ",mailboxId=" + item.getMailboxId() + ")");
                    scheduleNextReminder((Appointment) item);
                }
            }
        }
        if (mods.modified != null) {
            for (Map.Entry<PendingModifications.ModificationKey, PendingModifications.Change> entry : mods.modified.entrySet()) {
                PendingModifications.Change change = entry.getValue();
                if (change.what instanceof Appointment) {
                    Appointment calItem = (Appointment) change.what;
                    if (ZimbraLog.scheduler.isDebugEnabled())
                        ZimbraLog.scheduler.debug("Handling modification of calendar item (id=" + calItem.getId() + ",mailboxId=" + calItem.getMailboxId() + ")");
                    boolean calItemCanceled = false;
                    try {
                        if ((change.why & PendingModifications.Change.MODIFIED_FOLDER) != 0 && calItem.inTrash()) {
                            calItemCanceled = true;
                        }
                    } catch (ServiceException e) {
                        ZimbraLog.scheduler.error("Error in fetching calendar item's folder", e);
                    }
                    boolean scheduleNext = cancelExistingReminder(calItem);
                    if (scheduleNext && !calItemCanceled) {
                        scheduleNextReminder(calItem);
                    }
                }
            }
        }
        if (mods.deleted != null) {
            for (Map.Entry<PendingModifications.ModificationKey, Object> entry : mods.deleted.entrySet()) {
                Object deletedObj = entry.getValue();
                if (deletedObj instanceof Appointment) {
                    Appointment calItem = (Appointment) deletedObj;
                    if (ZimbraLog.scheduler.isDebugEnabled())
                        ZimbraLog.scheduler.debug("Handling deletion of calendar item (id=" + calItem.getId() + ",mailboxId=" + calItem.getMailboxId() + ")");
                    cancelExistingReminder(calItem);
                } else if (deletedObj instanceof Integer) {
                    // We only have item id
                    Mailbox mbox = null;
                    try {
                        mbox = MailboxManager.getInstance().getMailboxByAccountId(accountId, MailboxManager.FetchMode.DO_NOT_AUTOCREATE);
                    } catch (ServiceException e) {
                        ZimbraLog.scheduler.error("Error looking up the mailbox of account " + accountId, e);
                    }
                    if (mbox != null) {
                        cancelExistingReminder((Integer) deletedObj, mbox.getId());
                    }
                }
            }
        }
    }

    /**
     * Cancels any reminder for an existing calendar item.
     *
     * @param calItem
     * @return true if no error was encountered during cancellation
     */
    static boolean cancelExistingReminder(CalendarItem calItem) {
        return cancelExistingReminder(calItem.getId(), calItem.getMailboxId());
    }

    /**
     * Cancels any reminder for an existing calendar item.
     *
     * @param calItemId
     * @param mailboxId
     * @return true if no error was encountered during cancellation
     */
    static boolean cancelExistingReminder(int calItemId, long mailboxId) {
        try {
            ScheduledTaskManager.cancel(CalItemReminderTask.class.getName(),
                                        "reminderTask" + Integer.toString(calItemId),
                                        mailboxId,
                                        false);
        } catch (ServiceException e) {
            ZimbraLog.scheduler.warn("Canceling reminder task failed", e);
            return false;
        }
        return true;
    }

    /**
     * Schedules reminder task using calendar item's current alarm data.
     *
     * @param calItem
     */
    static void scheduleNextReminder(CalendarItem calItem) {
        scheduleNextReminder(calItem, false);
    }

    /**
     * Schedules reminder task for a calendar item.
     *
     * @param calItem
     * @param updateNextAlarm whether to update the next alarm before scheduling
     */
    static void scheduleNextReminder(CalendarItem calItem, boolean updateNextAlarm) {
        if (updateNextAlarm) {
            try {
                // The following operation results in the calendar item getting modified, hence this case would
                // be handled when we handle pending notifications for modified items
                calItem.getMailbox().dismissCalendarItemAlarm(null, calItem.getId(), System.currentTimeMillis());
            } catch (ServiceException e) {
                ZimbraLog.scheduler.error("Error in updating calendar item's next alarm", e);
            }
        } else {
            CalendarItem.AlarmData alarmData = calItem.getAlarmData();
            if (alarmData == null)
                return;
            CalItemReminderTask reminderTask = new CalItemReminderTask();
            reminderTask.setMailboxId(calItem.getMailboxId());
            reminderTask.setExecTime(new Date(alarmData.getNextAt()));
            reminderTask.setProperty("calItemId", Integer.toString(calItem.getId()));
            reminderTask.setProperty("invId", Integer.toString(alarmData.getInvId()));
            reminderTask.setProperty("compNum", Integer.toString(alarmData.getCompNum()));
            reminderTask.setProperty("nextInstStart", Long.toString(alarmData.getNextInstanceStart()));
            try {
                ScheduledTaskManager.schedule(reminderTask);
            } catch (ServiceException e) {
                ZimbraLog.scheduler.error("Error in scheduling reminder task " + reminderTask, e);
            }
        }
    }

    public int registerForItemTypes() {
        return MailItem.typeToBitmask(MailItem.TYPE_APPOINTMENT);
    }
}
