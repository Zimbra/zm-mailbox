/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.mail;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.sun.mail.smtp.SMTPMessage;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.CharsetUtil;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.L10nUtil.MsgKey;
import com.zimbra.common.zmime.ZMimeBodyPart;
import com.zimbra.common.zmime.ZMimeMultipart;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.JMSession;
import com.zimbra.soap.ZimbraSoapContext;

public class SendDeliveryReport extends MailDocumentHandler {

    private static final String[] TARGET_ITEM_PATH = new String[] { MailConstants.A_MESSAGE_ID };

    @Override
    protected String[] getProxiedIdPath(Element request) {
        return TARGET_ITEM_PATH;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);

        int msgid = new ItemId(request.getAttribute(MailConstants.A_MESSAGE_ID), zsc).getId();
        Message msg = mbox.getMessageById(octxt, msgid);

        // sending a read receipt requires write access to the message
        if ((mbox.getEffectivePermissions(octxt, msgid, MailItem.Type.MESSAGE) & ACL.RIGHT_WRITE) == 0) {
            throw ServiceException.PERM_DENIED("you do not have sufficient permissions on the message");
        }
        // first, send the notification
        sendReport(getSenderAccount(zsc), msg, false, zsc.getRequestIP(), zsc.getUserAgent());

        // then mark the message as \Notified
        mbox.alterTag(octxt, msgid, MailItem.Type.MESSAGE, Flag.FlagInfo.NOTIFIED, true, null);

        Element response = zsc.createElement(MailConstants.SEND_REPORT_RESPONSE);
        return response;
    }

    protected Account getSenderAccount(ZimbraSoapContext zsc) throws ServiceException {
        return getAuthenticatedAccount(zsc);
    }

    public static void sendReport(Account authAccount, Message msg, boolean automatic, String requestHost, String userAgent)
    throws ServiceException {
        MimeMessage mm = msg.getMimeMessage();
        Account owner = msg.getMailbox().getAccount();

        String charset = authAccount.getPrefMailDefaultCharset();
        if (charset == null) {
            charset = MimeConstants.P_CHARSET_UTF8;
        }

        try {
            InternetAddress[] recipients = Mime.parseAddressHeader(mm, "Disposition-Notification-To");
            if (recipients == null || recipients.length == 0)
                return;

            Domain domain = Provisioning.getInstance().getDomain(authAccount);
            Session smtpSession = JMSession.getSmtpSession(domain);
            SMTPMessage report = new SMTPMessage(smtpSession);
            String subject = "Read-Receipt: " + msg.getSubject();
            report.setSubject(subject, CharsetUtil.checkCharset(subject, charset));
            report.setSentDate(new Date());
            report.setFrom(AccountUtil.getFriendlyEmailAddress(authAccount));
            report.addRecipients(javax.mail.Message.RecipientType.TO, recipients);
            report.setHeader("Auto-Submitted", "auto-replied (zimbra; read-receipt)");
            report.setHeader("Precedence", "bulk");

            if (Provisioning.getInstance().getConfig().isAutoSubmittedNullReturnPath()) {
                report.setEnvelopeFrom("<>");
            } else {
                report.setEnvelopeFrom(authAccount.getName());
            }

            MimeMultipart multi = new ZMimeMultipart("report");

            // part 1: human-readable notification
            String text = generateTextPart(owner, mm, authAccount.getLocale());
            MimeBodyPart mpText = new ZMimeBodyPart();
            mpText.setText(text, CharsetUtil.checkCharset(text, charset));
            multi.addBodyPart(mpText);

            // part 2: disposition notification
            String mdn = generateReport(owner, mm, automatic, requestHost, userAgent);
            MimeBodyPart mpMDN = new ZMimeBodyPart();
            mpMDN.setText(mdn, MimeConstants.P_CHARSET_UTF8);
            mpMDN.setHeader("Content-Type", "message/disposition-notification; charset=utf-8");
            multi.addBodyPart(mpMDN);

//            // part 3: original message
//            MimeBodyPart mpOriginal = new ZMimeBodyPart();
//            mpOriginal.setDataHandler(new DataHandler(new BlobDataSource(msg.getBlob())));
//            mpOriginal.setHeader("Content-Type", MimeConstants.CT_MESSAGE_RFC822);
//            mpOriginal.setHeader("Content-Disposition", Part.ATTACHMENT);
//            multi.addBodyPart(mpOriginal);

            report.setContent(multi);
            report.setHeader("Content-Type", multi.getContentType() + "; report-type=disposition-notification");
            report.saveChanges();

            Transport.send(report);
            MailSender.logMessage((MimeMessage)report, report.getAllRecipients(), report.getEnvelopeFrom(),
                    smtpSession.getProperties().getProperty("mail.smtp.host"),
                    String.valueOf(msg.getId()), null, null, "read receipt");
        } catch (MessagingException me) {
            throw ServiceException.FAILURE("error while sending read receipt", me);
        }
    }

    public static String generateTextPart(Account owner, MimeMessage mm, Locale lc) throws MessagingException {
        String subject = Mime.getSubject(mm);

        String dateStr = "???";
        Calendar cal = DateUtil.parseRFC2822DateAsCalendar(mm.getHeader("Date", null));
        if (cal != null) {
            DateFormat format = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, lc);
            format.setTimeZone(TimeZone.getTimeZone("GMT" + DateUtil.getTimezoneString(cal)));
            dateStr = format.format(cal.getTime());
        }

        return L10nUtil.getMessage(MsgKey.readReceiptNotification, lc, dateStr, owner.getName(), subject);
    }

    public static String generateReport(Account owner, MimeMessage mm, boolean automatic, String requestHost, String userAgent)
    throws MessagingException {
        StringBuilder mdn = new StringBuilder();

        if (userAgent != null || requestHost != null) {
            mdn.append("Reporting-UA: ");
            if (requestHost != null && !requestHost.trim().equals("")) {
                mdn.append(requestHost).append(userAgent == null ? "" : "; ");
            }
            if (userAgent != null && !userAgent.trim().equals("")) {
                mdn.append(userAgent);
            }
            mdn.append("\r\n");
        }

        mdn.append("Original-Recipient: rfc822;").append(owner.getName()).append("\r\n");
        mdn.append("Final-Recipient: rfc822;").append(owner.getName()).append("\r\n");

        String messageID = mm.getMessageID();
        if (messageID != null && !messageID.trim().equals("")) {
            mdn.append("Original-Message-ID: ").append(messageID.trim()).append("\r\n");
        }

        mdn.append("Disposition: manual-action/MDN-sent-");
        mdn.append(automatic ? "automatically" : "manually");
        mdn.append("; displayed\r\n");

        return mdn.toString();
    }
}
