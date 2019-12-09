/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.ScheduledTask;
import com.zimbra.cs.mailbox.calendar.Invite;

/**
 */
public abstract class CalItemReminderTaskBase extends ScheduledTask {

    static final String CAL_ITEM_ID_PROP_NAME = "calItemId";
    static final String INV_ID_PROP_NAME = "invId";
    static final String COMP_NUM_PROP_NAME = "compNum";
    static final String NEXT_INST_START_PROP_NAME = "nextInstStart";
    static final String SMS_DOMAIN = "esms.gov.in";

    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @return computed result
     * @throws Exception if unable to compute a result
     */
    public CalendarItem call() throws Exception {
        if (ZimbraLog.scheduler.isDebugEnabled())
            ZimbraLog.scheduler.debug("Running task %s", this);
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        if (mbox == null) {
            ZimbraLog.scheduler.error("Mailbox with id %s does not exist", getMailboxId());
            return null;
        }
        Integer calItemId = new Integer(getProperty(CAL_ITEM_ID_PROP_NAME));
        CalendarItem calItem;
        try {
            calItem = mbox.getCalendarItemById(null, calItemId);
        } catch (ServiceException e) {
            ZimbraLog.scheduler.warn("Calendar item with id %s does not exist", calItemId);
            return null;
        }
        if (calItem.inTrash()) {
            ZimbraLog.scheduler.debug("Calendar item with id %s is in Trash", calItemId);
            return null;
        }
        Integer invId = new Integer(getProperty(INV_ID_PROP_NAME));
        Integer compNum = new Integer(getProperty(COMP_NUM_PROP_NAME));
        Invite invite = calItem.getInvite(invId, compNum);
        if (invite == null) {
            ZimbraLog.scheduler.warn("Invite with id %s and comp num %s does not exist", invId, compNum);
	        ZimbraLog.scheduler.info("Trying reminder sms");
			sendReminderSMS(calItem);
			return null;
        } else if (invite != null && invite.getAlarms() != null && invite.getAlarms().size() >= 2) {
        	String domain = null;
            for (int i = invite.getAlarms().size() - 1; invite.getAlarms().get(i).getAttendees().size() > 0 && i >= 0; i--) {
                domain = invite.getAlarms().get(i).getAttendees().get(0).getAddress().split("@")[1];
    			if (SMS_DOMAIN.equalsIgnoreCase(domain)) {
    	            ZimbraLog.scheduler.warn("Invite with id %s and comp num %s does not exist", invId, compNum);
    	            if (calItem.getType() == MailItem.Type.APPOINTMENT) {
    	            	ZimbraLog.scheduler.info("Trying reminder sms for Calendar Appointment");
    	            } else {
    	            	ZimbraLog.scheduler.info("Trying reminder sms for Task");
                    }
    				sendReminderSMS(calItem);
    			} else if (invite.getAlarms().get(i).getAction().equals("EMAIL") && !SMS_DOMAIN.equalsIgnoreCase(domain)) {
    				sendReminder(calItem, invite);
    			}
    		}
        }
        return calItem;
    }

    protected abstract void sendReminder(CalendarItem calItem, Invite invite) throws Exception;

	protected abstract boolean sendReminderSMS(CalendarItem calItem);
}
