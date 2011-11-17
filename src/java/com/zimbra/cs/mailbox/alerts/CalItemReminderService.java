package com.zimbra.cs.mailbox.alerts;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxListener;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.ScheduledTaskManager;
import com.zimbra.cs.mailbox.calendar.Alarm;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.session.PendingModifications.ModificationKey;

import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author vmahajan
 */
public class CalItemReminderService extends MailboxListener {

    @Override
    public void notify(ChangeNotification notification) {
        Account account = notification.mailboxAccount;
        if (notification.mods.created != null) {
            for (Map.Entry<ModificationKey, Change> entry : notification.mods.created.entrySet()) {
                MailItem item = (MailItem) entry.getValue().what;
                if (item instanceof CalendarItem) {
                    ZimbraLog.scheduler.debug("Handling creation of calendar item (id=%s,mailboxId=%s)",
                            item.getId(), item.getMailboxId());
                    scheduleNextReminders((CalendarItem) item);
                }
            }
        }
        if (notification.mods.modified != null) {
            for (Map.Entry<ModificationKey, Change> entry : notification.mods.modified.entrySet()) {
                Change change = entry.getValue();
                if (change.what instanceof CalendarItem) {
                    CalendarItem calItem = (CalendarItem) change.what;
                    ZimbraLog.scheduler.debug("Handling modification of calendar item (id=%s,mailboxId=%s)",
                            calItem.getId(), calItem.getMailboxId());
                    boolean calItemCanceled = false;
                    try {
                        if ((change.why & Change.FOLDER) != 0 && calItem.inTrash()) {
                            calItemCanceled = true;
                        }
                    } catch (ServiceException e) {
                        ZimbraLog.scheduler.error("Error in fetching calendar item's folder", e);
                    }
                    // cancel any existing reminders and schedule new ones if cal item not canceled
                    if (cancelExistingReminders(calItem) && !calItemCanceled)
                        scheduleNextReminders(calItem);
                }
            }
        }
        if (notification.mods.deleted != null) {
            for (Map.Entry<ModificationKey, Change> entry : notification.mods.deleted.entrySet()) {
                MailItem.Type type = (MailItem.Type) entry.getValue().what;
                if (type == MailItem.Type.APPOINTMENT || type == MailItem.Type.TASK) {
                    Mailbox mbox = null;
                    try {
                        mbox = MailboxManager.getInstance().getMailboxByAccount(
                                account, MailboxManager.FetchMode.DO_NOT_AUTOCREATE);
                    } catch (ServiceException e) {
                        ZimbraLog.scheduler.error("Error looking up the mailbox of account %s", account.getId(), e);
                    }
                    if (mbox != null) {
                        cancelExistingReminders(entry.getKey().getItemId(), mbox.getId());
                    }
                }
            }
        }
    }

    /**
     * Cancels email reminders for the calendar item.
     *
     * @param calItem
     * @return true if no error was encountered during cancellation
     */
    static boolean cancelExistingReminders(CalendarItem calItem) {
        return cancelExistingReminders(calItem.getId(), calItem.getMailboxId());
    }

    /**
     * Cancels existing reminders for the calendar item.
     *
     * @param calItemId
     * @param mailboxId
     * @return true if no error was encountered during cancellation
     */
    static boolean cancelExistingReminders(int calItemId, int mailboxId) {
        try {
            ScheduledTaskManager.cancel(CalItemEmailReminderTask.class.getName(),
                                        CalItemEmailReminderTask.TASK_NAME_PREFIX + Integer.toString(calItemId),
                                        mailboxId,
                                        false);
            ScheduledTaskManager.cancel(CalItemSmsReminderTask.class.getName(),
                                        CalItemSmsReminderTask.TASK_NAME_PREFIX + Integer.toString(calItemId),
                                        mailboxId,
                                        false);
        } catch (ServiceException e) {
            ZimbraLog.scheduler.warn("Canceling reminder tasks failed", e);
            return false;
        }
        return true;
    }

    /**
     * Schedules next reminders for the calendar item.
     *
     * @param calItem
     */
    static void scheduleNextReminders(CalendarItem calItem) {
        try {
            CalendarItem.AlarmData alarmData = calItem.getNextEmailAlarm();
            if (alarmData == null)
                return;
            boolean sendEmail = true;
            boolean sendSms = false;
            Alarm emailAlarm = alarmData.getAlarm();
            List<ZAttendee> recipients = emailAlarm.getAttendees();
            if (recipients != null && !recipients.isEmpty()) {
                sendEmail = false;
                Account acct = calItem.getAccount();
                String defaultEmailAddress = acct.getPrefCalendarReminderEmail();
                String defaultDeviceAddress = acct.getCalendarReminderDeviceEmail();
                for (ZAttendee recipient : recipients) {
                    if (recipient.getAddress().equals(defaultEmailAddress))
                        sendEmail = true;
                    if (recipient.getAddress().equals(defaultDeviceAddress))
                        sendSms = true;
                }
            }
            if (sendEmail)
                scheduleReminder(new CalItemEmailReminderTask(), calItem, alarmData);
            if (sendSms)
                scheduleReminder(new CalItemSmsReminderTask(), calItem, alarmData);
        } catch (ServiceException e) {
            ZimbraLog.scheduler.error("Error in scheduling reminder task", e);
        }
    }

    private static void scheduleReminder(
            CalItemReminderTaskBase reminderTask, CalendarItem calItem, CalendarItem.AlarmData alarmData)
    throws ServiceException {
        reminderTask.setMailboxId(calItem.getMailboxId());
        reminderTask.setExecTime(new Date(alarmData.getNextAt()));
        reminderTask.setProperty(CalItemReminderTaskBase.CAL_ITEM_ID_PROP_NAME, Integer.toString(calItem.getId()));
        reminderTask.setProperty(CalItemReminderTaskBase.INV_ID_PROP_NAME, Integer.toString(alarmData.getInvId()));
        reminderTask.setProperty(CalItemReminderTaskBase.COMP_NUM_PROP_NAME, Integer.toString(alarmData.getCompNum()));
        reminderTask.setProperty(CalItemReminderTaskBase.NEXT_INST_START_PROP_NAME, Long.toString(alarmData.getNextInstanceStart()));
        ScheduledTaskManager.schedule(reminderTask);
    }

    @Override
    public Set<MailItem.Type> registerForItemTypes() {
        return EnumSet.of(MailItem.Type.APPOINTMENT, MailItem.Type.TASK);
    }
}
