package com.zimbra.cs.mailbox.alerts;

import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.ScheduledTask;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.JMSession;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author vmahajan
 */
public class CalItemReminderTask extends ScheduledTask {

    /**
     * Returns the task name.
     */
    @Override
    public String getName() {
        return "reminderTask" + getProperty("calItemId");
    }

    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @return computed result
     * @throws Exception if unable to compute a result
     */
    public CalendarItem call() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        if (mbox == null) {
            ZimbraLog.scheduler.error("Error while running reminder task " + this + ". Mailbox does not exist.");
            return null;
        }
        Integer calItemId = new Integer(getProperty("calItemId"));
        CalendarItem calItem = (CalendarItem) mbox.getItemById(null, calItemId, MailItem.TYPE_APPOINTMENT);
        if (calItem == null) {
            ZimbraLog.scheduler.warn("Error while running reminder task " + this + ". Calendar item with id " + calItemId + " does not exist in mailbox.");
            return null;
        }
        Integer invId = new Integer(getProperty("invId"));
        Integer compNum = new Integer(getProperty("compNum"));
        Invite invite = calItem.getInvite(invId, compNum);
        if (invite == null) {
            ZimbraLog.scheduler.warn("Error while running reminder task " + this + ". Invite with id " + invId + " and comp num " + compNum + " does not exist.");
            return null;
        }
        sendReminderEmail(calItem, invite);
        return calItem;
    }

    private void sendReminderEmail(CalendarItem calItem, Invite invite) throws MessagingException, ServiceException {
        ZimbraLog.scheduler.debug("Creating reminder email for calendar item (id=" + calItem.getId() + ",mailboxId=" + calItem.getMailboxId() + ")");
        Locale locale = calItem.getAccount().getLocale();
        TimeZone tz = ICalTimeZone.getAccountTimeZone(calItem.getAccount());

        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession());

        InternetAddress emailAddress = AccountUtil.getFriendlyEmailAddress(calItem.getAccount());
        mm.setFrom(emailAddress);
        mm.setRecipient(javax.mail.Message.RecipientType.TO, emailAddress);

        mm.setSubject(L10nUtil.getMessage(L10nUtil.MsgKey.calendarReminderEmailSubject, locale) + ": " + calItem.getSubject(), MimeConstants.P_CHARSET_UTF8);

        MimeMultipart mmp = new MimeMultipart("alternative");
        mm.setContent(mmp);

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(getBody(invite, false, locale, tz), MimeConstants.P_CHARSET_UTF8);
        mmp.addBodyPart(textPart);

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(getBody(invite, true, locale, tz), MimeConstants.CT_TEXT_HTML + "; " + MimeConstants.P_CHARSET + "=" + MimeConstants.P_CHARSET_UTF8);
        mmp.addBodyPart(htmlPart);

        mm.setSentDate(new Date());

        mm.saveChanges();

        calItem.getMailbox().getMailSender().sendMimeMessage(null, calItem.getMailbox(), mm);
    }

    private String getBody(Invite invite, boolean html, Locale locale, TimeZone tz) throws ServiceException {
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.LONG, locale);
        dateFormat.setTimeZone(tz);
        Date startDate = new Date(new Long(getProperty("nextInstStart")));
        Date endDate = invite.getEffectiveDuration().addToDate(startDate);
        String location = invite.getLocation();
        String description = html ? invite.getDescriptionHtml() : invite.getDescription();
        return html ? L10nUtil.getMessage(L10nUtil.MsgKey.calendarReminderEmailBodyHtml,
                                          locale,
                                          dateFormat.format(startDate),
                                          dateFormat.format(endDate),
                                          location == null ? "" : location,
                                          description == null ? "" : description) :
                      L10nUtil.getMessage(L10nUtil.MsgKey.calendarReminderEmailBody,
                                          locale,
                                          dateFormat.format(startDate),
                                          dateFormat.format(endDate),
                                          location == null ? "" : location,
                                          description == null ? "" : description);
    }
}
