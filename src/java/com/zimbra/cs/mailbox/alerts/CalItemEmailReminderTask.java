package com.zimbra.cs.mailbox.alerts;

import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.util.JMSession;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

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

    protected void sendReminder(CalendarItem calItem, Invite invite) throws Exception {
        Account account = calItem.getAccount();
        Locale locale = account.getLocale();
        TimeZone tz = ICalTimeZone.getAccountTimeZone(account);

        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession());

        String to = account.getAttr(Provisioning.A_zimbraPrefCalendarReminderEmail);
        if (to == null) {
            ZimbraLog.scheduler.info("Unable to send calendar reminder email since %s is not set", Provisioning.A_zimbraPrefCalendarReminderEmail);
            return;
        }
        mm.setRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));

        mm.setSubject(L10nUtil.getMessage(calItem.getType() == MailItem.TYPE_APPOINTMENT ? L10nUtil.MsgKey.apptReminderEmailSubject : L10nUtil.MsgKey.taskReminderEmailSubject, 
                                          locale,
                                          calItem.getSubject()),
                      MimeConstants.P_CHARSET_UTF8);

        MimeMultipart mmp = new MimeMultipart("alternative");
        mm.setContent(mmp);

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(getBody(calItem, invite, false, locale, tz), MimeConstants.P_CHARSET_UTF8);
        mmp.addBodyPart(textPart);

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(getBody(calItem, invite, true, locale, tz), MimeConstants.CT_TEXT_HTML + "; " + MimeConstants.P_CHARSET + "=" + MimeConstants.P_CHARSET_UTF8);
        mmp.addBodyPart(htmlPart);

        mm.setSentDate(new Date());

        mm.saveChanges();

        calItem.getMailbox().getMailSender().sendMimeMessage(null, calItem.getMailbox(), mm);
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
        if (calItem.getType() == MailItem.TYPE_APPOINTMENT) {
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
            if (zOrganizer.hasCn()) {
                organizer = zOrganizer.getCn();
            } else {
                organizer = zOrganizer.getAddress();
            }
        }

        String folder = calItem.getMailbox().getFolderById(calItem.getFolderId()).getName();

        String description = html ? invite.getDescriptionHtml() : invite.getDescription();

        return html ? L10nUtil.getMessage(calItem.getType() == MailItem.TYPE_APPOINTMENT ? L10nUtil.MsgKey.apptReminderEmailBodyHtml : L10nUtil.MsgKey.taskReminderEmailBodyHtml,
                                          locale,
                                          formattedStart,
                                          formattedEnd,
                                          location,
                                          organizer,
                                          folder,
                                          description) :
                      L10nUtil.getMessage(calItem.getType() == MailItem.TYPE_APPOINTMENT ? L10nUtil.MsgKey.apptReminderEmailBody : L10nUtil.MsgKey.taskReminderEmailBody,
                                          locale,
                                          formattedStart,
                                          formattedEnd,
                                          location,
                                          organizer,
                                          folder,
                                          description);
    }
}
