package com.zimbra.cs.mailbox.alerts;

import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.ScheduledTask;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;
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
        Account account = calItem.getAccount();
        Locale locale = account.getLocale();
        TimeZone tz = ICalTimeZone.getAccountTimeZone(account);

        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession());

        InternetAddress acctAddr = AccountUtil.getFriendlyEmailAddress(account);
        mm.setFrom(acctAddr);
        String to = account.getAttr(Provisioning.A_zimbraPrefCalendarReminderEmail);
        if (to == null) {
            ZimbraLog.scheduler.warn("Unable to send calendar reminder email since " + Provisioning.A_zimbraPrefCalendarReminderEmail + " is not set");
            return;
        }
        mm.setRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));

        mm.setSubject(L10nUtil.getMessage(L10nUtil.MsgKey.calendarReminderEmailSubject, locale, calItem.getSubject()), MimeConstants.P_CHARSET_UTF8);

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

        Date start = new Date(new Long(getProperty("nextInstStart")));
        String formattedStart = dateTimeFormat.format(start);

        DateFormat onlyDateFormat = DateFormat.getDateInstance(DateFormat.SHORT, locale);
        onlyDateFormat.setTimeZone(tz);
        DateFormat onlyTimeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, locale);
        onlyTimeFormat.setTimeZone(tz);

        Date end = invite.getEffectiveDuration().addToDate(start);
        String formattedEnd =
                onlyDateFormat.format(start).equals(onlyDateFormat.format(end)) ? onlyTimeFormat.format(end) : dateTimeFormat.format(end);

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

        String calendar = calItem.getMailbox().getFolderById(calItem.getFolderId()).getName();

        String description = html ? invite.getDescriptionHtml() : invite.getDescription();

        return html ? L10nUtil.getMessage(L10nUtil.MsgKey.calendarReminderEmailBodyHtml,
                                          locale,
                                          formattedStart,
                                          formattedEnd,
                                          location,
                                          organizer,
                                          calendar,
                                          description) :
                      L10nUtil.getMessage(L10nUtil.MsgKey.calendarReminderEmailBody,
                                          locale,
                                          formattedStart,
                                          formattedEnd,
                                          location,
                                          organizer,
                                          calendar,
                                          description);
    }
}
