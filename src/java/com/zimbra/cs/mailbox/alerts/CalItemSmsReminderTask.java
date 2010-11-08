package com.zimbra.cs.mailbox.alerts;

import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.util.JMSession;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 */
public class CalItemSmsReminderTask extends CalItemReminderTaskBase {

    static final String TASK_NAME_PREFIX = "smsReminderTask";

    /**
     * Returns the task name.
     */
    @Override
    public String getName() {
        return TASK_NAME_PREFIX + getProperty(CAL_ITEM_ID_PROP_NAME);
    }

    protected void sendReminder(CalendarItem calItem, Invite invite) throws Exception {
        Account account = calItem.getAccount();
        Locale locale = account.getLocale();
        TimeZone tz = ICalTimeZone.getAccountTimeZone(account);
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession());
        String to = account.getAttr(Provisioning.A_zimbraCalendarReminderDeviceEmail);
        if (to == null) {
            ZimbraLog.scheduler.info("Unable to send calendar reminder sms since %s is not set", Provisioning.A_zimbraCalendarReminderDeviceEmail);
            return;
        }
        mm.setRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
        mm.setText(getText(calItem, invite, locale, tz), MimeConstants.P_CHARSET_UTF8);
        mm.saveChanges();
        MailSender mailSender = calItem.getMailbox().getMailSender();
        mailSender.setSaveToSent(false);
        mailSender.setSkipSendAsCheck(true);
        mailSender.sendMimeMessage(null, calItem.getMailbox(), mm);
    }

    private String getText(CalendarItem calItem, Invite invite, Locale locale, TimeZone tz) throws ServiceException {
        DateFormat dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);
        dateTimeFormat.setTimeZone(tz);
        DateFormat onlyDateFormat = DateFormat.getDateInstance(DateFormat.SHORT, locale);
        onlyDateFormat.setTimeZone(tz);

        String formattedStart = null;
        String formattedEnd = null;
        if (calItem.getType() == MailItem.TYPE_APPOINTMENT) {
            Date start = new Date(new Long(getProperty(NEXT_INST_START_PROP_NAME)));
            formattedStart = dateTimeFormat.format(start);
            Date end = invite.getEffectiveDuration().addToDate(start);
            formattedEnd = dateTimeFormat.format(end);
        } else {
            // start date and due date is optional for tasks
            if (calItem.getStartTime() != 0) {
                formattedStart = onlyDateFormat.format(new Date(calItem.getStartTime()));
            }
            if (calItem.getEndTime() != 0) {
                formattedEnd = onlyDateFormat.format(new Date(calItem.getEndTime()));
            }
        }

        String location = invite.getLocation();

        String organizer = null;
        ZOrganizer zOrganizer = invite.getOrganizer();
        if (zOrganizer != null) {
            if (zOrganizer.hasCn()) {
                organizer = zOrganizer.getCn();
            } else {
                organizer = zOrganizer.getAddress();
            }
        }

        String folder = calItem.getMailbox().getFolderById(calItem.getFolderId()).getName();

        return L10nUtil.getMessage(calItem.getType() == MailItem.TYPE_APPOINTMENT ? L10nUtil.MsgKey.apptReminderSmsText : L10nUtil.MsgKey.taskReminderSmsText,
                                   locale,
                                   calItem.getSubject(),
                                   formattedStart,
                                   formattedEnd,
                                   location,
                                   organizer,
                                   folder);
    }
}
