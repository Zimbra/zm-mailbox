/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.zmime.ZMimeBodyPart;
import com.zimbra.common.zmime.ZMimeMultipart;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.EmailToSMS;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.Util;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.util.JMSession;

/**
 */
public class CalItemEmailReminderTask extends CalItemReminderTaskBase {

    static final String TASK_NAME_PREFIX = "emailReminderTask";

    /**
     * Returns the task name.
     */
    @Override
    public String getName() {
        return TASK_NAME_PREFIX + getProperty(CAL_ITEM_ID_PROP_NAME);
    }

    @Override
    protected void sendReminder(CalendarItem calItem, Invite invite) throws Exception {
        Account account = calItem.getAccount();
        Locale locale = account.getLocale();
        TimeZone tz = Util.getAccountTimeZone(account);

        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSmtpSession(account));

        String to = account.getAttr(Provisioning.A_zimbraPrefCalendarReminderEmail);
        if (to == null) {
            ZimbraLog.scheduler.info("Unable to send calendar reminder email since %s is not set", Provisioning.A_zimbraPrefCalendarReminderEmail);
            return;
        }
        mm.setRecipient(javax.mail.Message.RecipientType.TO, new JavaMailInternetAddress(to));

        mm.setSubject(L10nUtil.getMessage(calItem.getType() == MailItem.Type.APPOINTMENT ?
                L10nUtil.MsgKey.apptReminderEmailSubject : L10nUtil.MsgKey.taskReminderEmailSubject,
                locale, calItem.getSubject()), MimeConstants.P_CHARSET_UTF8);

        if (invite.getDescriptionHtml() == null) {
            mm.setText(getBody(calItem, invite, false, locale, tz), MimeConstants.P_CHARSET_UTF8);
        } else {
            MimeMultipart mmp = new ZMimeMultipart("alternative");
            mm.setContent(mmp);

            MimeBodyPart textPart = new ZMimeBodyPart();
            textPart.setText(getBody(calItem, invite, false, locale, tz), MimeConstants.P_CHARSET_UTF8);
            mmp.addBodyPart(textPart);

            MimeBodyPart htmlPart = new ZMimeBodyPart();
            htmlPart.setContent(getBody(calItem, invite, true, locale, tz), MimeConstants.CT_TEXT_HTML + "; " + MimeConstants.P_CHARSET + "=" + MimeConstants.P_CHARSET_UTF8);
            mmp.addBodyPart(htmlPart);
        }

        mm.setSentDate(new Date());

        mm.saveChanges();

        MailSender mailSender = calItem.getMailbox().getMailSender();
        mailSender.sendMimeMessage(null, calItem.getMailbox(), mm);
    }

    private String getBody(CalendarItem calItem, Invite invite, boolean html, Locale locale, TimeZone tz) throws ServiceException {
        DateFormat dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);
        dateTimeFormat.setTimeZone(tz);
        DateFormat onlyDateFormat = DateFormat.getDateInstance(DateFormat.SHORT, locale);
        onlyDateFormat.setTimeZone(tz);
        DateFormat onlyTimeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, locale);
        onlyTimeFormat.setTimeZone(tz);

        String formattedStart;
        String formattedEnd;
        if (calItem.getType() == MailItem.Type.APPOINTMENT) {
            Date start = new Date(new Long(getProperty(NEXT_INST_START_PROP_NAME)));
            formattedStart = dateTimeFormat.format(start);
            Date end = invite.getEffectiveDuration().addToDate(start);
            formattedEnd = onlyDateFormat.format(start).equals(onlyDateFormat.format(end)) ? onlyTimeFormat.format(end) : dateTimeFormat.format(end);
        } else {
            // start date and due date is optional for tasks
            formattedStart = invite.getStartTime() == null ? "" : onlyDateFormat.format(invite.getStartTime().getDate());
            formattedEnd = invite.getEndTime() == null ? "" : onlyDateFormat.format(invite.getEndTime().getDate());
        }

        String location = invite.getLocation();

        String organizer = null;
        ZOrganizer zOrganizer = invite.getOrganizer();
        if (zOrganizer != null) {
            organizer = zOrganizer.hasCn() ? zOrganizer.getCn() : zOrganizer.getAddress();
        }
        if (organizer == null) {
            organizer = "";
        }


        String folder = calItem.getMailbox().getFolderById(null, calItem.getFolderId()).getName();

        String description = html ? invite.getDescriptionHtml() : invite.getDescription();
        if (description == null) description = "";

        return html ?
                L10nUtil.getMessage(calItem.getType() == MailItem.Type.APPOINTMENT ?
                                            L10nUtil.MsgKey.apptReminderEmailBodyHtml : L10nUtil.MsgKey.taskReminderEmailBodyHtml,
                                    locale, formattedStart, formattedEnd, location, organizer, folder, description) :
                L10nUtil.getMessage(calItem.getType() == MailItem.Type.APPOINTMENT ?
                                            L10nUtil.MsgKey.apptReminderEmailBody : L10nUtil.MsgKey.taskReminderEmailBody,
                                    locale, formattedStart, formattedEnd, location, organizer, folder, description);
    }

	@Override
	protected boolean sendReminderSMS(CalendarItem calItem) {
		try {
			Runnable r = new Runnable() {
				@Override
				public void run() {
					try {
						EmailToSMS.getInstance().sendCalendarReminderSMS(calItem);
					} catch (ServiceException e) {
						ZimbraLog.calendar.error("ServiceException while sending reminder sms", e);
					} catch (OutOfMemoryError e) {
						ZimbraLog.calendar.error("OutOfMemoryError while sending reminder sms", e);
					}
				}
			};
			Thread senderThread = new Thread(r, "ReminderSMS");
			senderThread.setDaemon(true);
			senderThread.start();
		} catch (Exception e) {
			ZimbraLog.calendar.error("ServiceException while sending calendar sms", e);
			return false;
		}
		return true;
	}
}
