/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.mailbox.alerts;

import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.mailbox.BaseItemInfo;
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
import java.util.Calendar;
import com.zimbra.common.util.StringUtil;

/**
 * @author vmahajan
 */
public class CalItemReminderService extends MailboxListener {

    @Override
    public void notify(ChangeNotification notification) {
        Account account = notification.mailboxAccount;
        if (notification.mods.created != null) {
            for (Map.Entry<ModificationKey, BaseItemInfo> entry : notification.mods.created.entrySet()) {
                BaseItemInfo item = entry.getValue();
                if (item instanceof CalendarItem) {
                    CalendarItem calItem = (CalendarItem) item;
                    ZimbraLog.scheduler.debug("Handling creation of calendar item (id=%s,mailboxId=%s)",
                            calItem.getId(), calItem.getMailboxId());
                    scheduleNextReminders((CalendarItem) item, true, true);
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
                        scheduleNextReminders(calItem, true, true);
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
     * @param email
     * @param sms
     */
    static void scheduleNextReminders(CalendarItem calItem, boolean email, boolean sms) {
        try {
            CalendarItem.AlarmData alarmData = calItem.getNextEmailAlarm();
            if (alarmData == null)
                return;
            boolean emailAlarmExists = true;
            boolean smsAlarmExists = false;
            Alarm emailAlarm = alarmData.getAlarm();
            List<ZAttendee> recipients = emailAlarm.getAttendees();
            if (recipients != null && !recipients.isEmpty()) {
                emailAlarmExists = false;
                Account acct = calItem.getAccount();
                String defaultEmailAddress = acct.getPrefCalendarReminderEmail();
                String defaultDeviceAddress = acct.getCalendarReminderDeviceEmail();
                for (ZAttendee recipient : recipients) {
                    if (recipient.getAddress().equals(defaultEmailAddress)) {
                        emailAlarmExists = true;
                    }
                    if (recipient.getAddress().equals(defaultDeviceAddress)) {
                        smsAlarmExists = true;
                    }
                }
            }
            if (emailAlarmExists && email) {
                scheduleReminder(new CalItemEmailReminderTask(), calItem, alarmData);
            }
            if (smsAlarmExists && sms) {
                scheduleReminder(new CalItemSmsReminderTask(), calItem, alarmData);
            }
        } catch (ServiceException e) {
            ZimbraLog.scheduler.error("Error in scheduling reminder task", e);
        }
    }

	public static void scheduleReminder(CalItemReminderTaskBase reminderTask, CalendarItem calItem) throws ServiceException {
		Account acc = calItem.getAccount();
		String mobileNo = acc.getCalendarReminderDeviceEmail();
		if(!StringUtil.isNullOrEmpty(mobileNo)) {
			int min = acc.getPrefCalendarApptReminderWarningTime();
			long startTime = calItem.getStartTime();
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(startTime);
			cal.add(Calendar.MINUTE, -min);
			long currentTime = System.currentTimeMillis();
			if(cal.getTimeInMillis() < currentTime) {
				cal.setTimeInMillis(currentTime);
				cal.add(Calendar.MINUTE, 2);
			}
			Date exeTime = cal.getTime();
			reminderTask.setMailboxId(calItem.getMailboxId());
			reminderTask.setExecTime(exeTime);
			reminderTask.setProperty(CalItemReminderTaskBase.CAL_ITEM_ID_PROP_NAME, Integer.toString(calItem.getId()));
			reminderTask.setProperty(CalItemReminderTaskBase.INV_ID_PROP_NAME, "0");
			reminderTask.setProperty(CalItemReminderTaskBase.COMP_NUM_PROP_NAME, "0");
			reminderTask.setProperty(CalItemReminderTaskBase.NEXT_INST_START_PROP_NAME, Long.toString(startTime));
			ScheduledTaskManager.schedule(reminderTask);
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
