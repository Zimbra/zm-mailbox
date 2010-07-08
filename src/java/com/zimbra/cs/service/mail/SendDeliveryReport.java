/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.sun.mail.smtp.SMTPMessage;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.L10nUtil.MsgKey;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
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
    @Override protected String[] getProxiedIdPath(Element request) { return TARGET_ITEM_PATH; }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);

        int msgid = new ItemId(request.getAttribute(MailConstants.A_MESSAGE_ID), zsc).getId();
        Message msg = mbox.getMessageById(octxt, msgid);

        // sending a read receipt requires write access to the message
        if ((mbox.getEffectivePermissions(octxt, msgid, MailItem.TYPE_MESSAGE) & ACL.RIGHT_WRITE) == 0)
            throw ServiceException.PERM_DENIED("you do not have sufficient permissions on the message");

        // first, send the notification
        sendReport(getSenderAccount(zsc), msg, false, zsc.getRequestIP(), zsc.getUserAgent());

        // then mark the message as \Notified
        mbox.alterTag(octxt, msgid, MailItem.TYPE_MESSAGE, Flag.ID_FLAG_NOTIFIED, true);

        Element response = zsc.createElement(MailConstants.SEND_REPORT_RESPONSE);
        return response;
    }

    protected Account getSenderAccount(ZimbraSoapContext zsc) throws ServiceException {
        return getAuthenticatedAccount(zsc);
    }

    void sendReport(Account authAccount, Message msg, boolean automatic, String requestHost, String userAgent)
    throws ServiceException {
        MimeMessage mm = msg.getMimeMessage();
        Account owner = msg.getMailbox().getAccount();

        String charset = authAccount.getAttr(Provisioning.A_zimbraPrefMailDefaultCharset, MimeConstants.P_CHARSET_UTF8);
        try {
            InternetAddress[] recipients = Mime.parseAddressHeader(mm, "Disposition-Notification-To");
            if (recipients == null || recipients.length == 0)
                return;

            SMTPMessage report = new SMTPMessage(JMSession.getSmtpSession());
            String subject = "Read-Receipt: " + msg.getSubject();
            report.setSubject(subject, StringUtil.checkCharset(subject, charset));
            report.setSentDate(new Date());
            report.setFrom(AccountUtil.getFriendlyEmailAddress(authAccount));
            report.addRecipients(javax.mail.Message.RecipientType.TO, recipients);
            report.setHeader("Auto-Submitted", "auto-replied (zimbra; read-receipt)");
            report.setHeader("Precedence", "bulk");

            if (Provisioning.getInstance().getConfig().getBooleanAttr(Provisioning.A_zimbraAutoSubmittedNullReturnPath, true))
                report.setEnvelopeFrom("<>");
            else
                report.setEnvelopeFrom(authAccount.getName());

            MimeMultipart multi = new MimeMultipart("report");

            // part 1: human-readable notification
            String text = generateTextPart(owner, mm, authAccount.getLocale());
            MimeBodyPart mpText = new MimeBodyPart();
            mpText.setText(text, StringUtil.checkCharset(text, charset));
            multi.addBodyPart(mpText);

            // part 2: disposition notification
            String mdn = generateReport(owner, mm, automatic, requestHost, userAgent);
            MimeBodyPart mpMDN = new MimeBodyPart();
            mpMDN.setText(mdn, MimeConstants.P_CHARSET_UTF8);
            mpMDN.setHeader("Content-Type", "message/disposition-notification; charset=utf-8");
            multi.addBodyPart(mpMDN);

//            // part 3: original message
//            MimeBodyPart mpOriginal = new MimeBodyPart();
//            mpOriginal.setDataHandler(new DataHandler(new BlobDataSource(msg.getBlob())));
//            mpOriginal.setHeader("Content-Type", MimeConstants.CT_MESSAGE_RFC822);
//            mpOriginal.setHeader("Content-Disposition", Part.ATTACHMENT);
//            multi.addBodyPart(mpOriginal);

            report.setContent(multi);
            report.setHeader("Content-Type", multi.getContentType() + "; report-type=disposition-notification");
            report.saveChanges();

            Transport.send(report);
        } catch (MessagingException me) {
            throw ServiceException.FAILURE("error while sending read receipt", me);
        }
    }

    protected String generateTextPart(Account owner, MimeMessage mm, Locale lc) throws MessagingException {
        String subject = Mime.getSubject(mm);

        String dateStr = "???";
        Calendar cal = DateUtil.parseRFC2822DateAsCalendar(mm.getHeader("Date", null));
        if (cal != null) {
            DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, lc);
            format.setTimeZone(TimeZone.getTimeZone("GMT" + DateUtil.getTimezoneString(cal)));
            dateStr = format.format(cal.getTime());
        }

        return L10nUtil.getMessage(MsgKey.readReceiptNotification, lc, dateStr, owner.getName(), subject);
    }

    protected String generateReport(Account owner, MimeMessage mm, boolean automatic, String requestHost, String userAgent)
    throws MessagingException {
        StringBuilder mdn = new StringBuilder();

        if (userAgent != null || requestHost != null) {
            mdn.append("Reporting-UA: ");
            if (requestHost != null && !requestHost.trim().equals(""))
                mdn.append(requestHost).append(userAgent == null ? "" : "; ");
            if (userAgent != null && !userAgent.trim().equals(""))
                mdn.append(userAgent);
            mdn.append("\r\n");
        }

        mdn.append("Original-Recipient: rfc822;").append(owner.getName()).append("\r\n");
        mdn.append("Final-Recipient: rfc822;").append(owner.getName()).append("\r\n");

        String messageID = mm.getMessageID();
        if (messageID != null && !messageID.trim().equals(""))
            mdn.append("Original-Message-ID: ").append(messageID.trim()).append("\r\n");

        mdn.append("Disposition: manual-action/MDN-sent-");
        mdn.append(automatic ? "automatically" : "manually");
        mdn.append("; displayed\r\n");

        return mdn.toString();
    }
}
