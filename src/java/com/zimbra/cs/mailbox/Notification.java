/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

package com.zimbra.cs.mailbox;

import com.google.common.collect.Sets;
import com.sun.mail.smtp.SMTPMessage;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.common.mime.shim.JavaMailMimeBodyPart;
import com.zimbra.common.mime.shim.JavaMailMimeMessage;
import com.zimbra.common.mime.shim.JavaMailMimeMultipart;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CharsetUtil;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.EmailUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbOutOfOffice;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.lmtpserver.LmtpCallback;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.JMSession;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class Notification implements LmtpCallback {

    /**
     * Do not send someone an out of office reply within this number of days.
     */
    public static final long DEFAULT_OUT_OF_OFFICE_CACHE_DURATION_MILLIS = 7 * Constants.MILLIS_PER_DAY;

    /**
     * We have to check all of a user's addresses against all of the
     * to,cc addresses in the message.  This is an M x N problem.
     * We just check the first few addresses in to,cc against all of
     * the user's addresses to guard against messages with a large number
     * of addresses in to,cc.
     */
    private static final int OUT_OF_OFFICE_DIRECT_CHECK_NUM_RECIPIENTS = 10; // M x N, so limit it.

    private static final Notification sInstance = new Notification();

    private Notification() {
    }

    /**
     * Subclass of <tt>MimeMessage</tt> that allows the caller to set an explicit <tt>Message-ID</tt>
     * header (see JavaMail FAQ for details).
     */
    private class MimeMessageWithId extends JavaMailMimeMessage {

        private String mMessageId;

        private MimeMessageWithId(String messageId) {
            super(JMSession.getSession());
            mMessageId = messageId;
        }

        @Override
        protected void updateMessageID() throws MessagingException {
            setHeader("Message-ID", mMessageId);
        }
    }

    @Override
    public void afterDelivery(Account account, Mailbox mbox, String envelopeSender,
                              String recipientEmail, Message newMessage) {
        // If notification fails, log a warning and continue so that message delivery
        // isn't affected
        try {
            notifyIfNecessary(account, newMessage, recipientEmail);
        } catch (MessagingException e) {
            ZimbraLog.mailbox.warn("Unable to send new mail notification", e);
        } catch (ServiceException e) {
            ZimbraLog.mailbox.warn("Unable to send new mail notification", e);
        }

        try {
            outOfOfficeIfNecessary(account, mbox, newMessage,
                recipientEmail, envelopeSender);
        } catch (MessagingException e) {
            ZimbraLog.mailbox.warn("Unable to send out-of-office reply", e);
        } catch (ServiceException e) {
            ZimbraLog.mailbox.warn("Unable to send out-of-office reply", e);
        }
    }

    public static Notification getInstance() {
        return sInstance;
    }

    /**
     * If the recipient's account requires out of office notification,
     * send it out.  We send these out based on users' setting, and
     * the incoming message meeting certain criteria.
     */
    private void outOfOfficeIfNecessary(Account account, Mailbox mbox, Message msg,
                                        String rcpt, String envSenderString)
    throws ServiceException, MessagingException {
        String destination = null;

        boolean replyEnabled = account.isPrefOutOfOfficeReplyEnabled();
        if (ZimbraLog.mailbox.isDebugEnabled()) {
            ZimbraLog.mailbox.debug("outofoffice reply enabled=" + replyEnabled + " rcpt='" + rcpt + "' mid=" + msg.getId());
        }
        if (!replyEnabled) {
            return;
        }

        // Reject if spam or trash.  If a message ends up in the trash as a result of the user's
        // filter rules, we assume it's not interesting.
        if (msg.inSpam()) {
            ofailed("in spam", destination, rcpt, msg);
            return;
        }
        if (msg.inTrash()) {
            ofailed("in trash", destination, rcpt, msg);
            return;
        }

        // Check if we are in any configured out of office interval
        Date now = new Date();
        Date fromDate = account.getGeneralizedTimeAttr(Provisioning.A_zimbraPrefOutOfOfficeFromDate, null);
        if (fromDate != null && now.before(fromDate)) {
            ofailed("from date not reached", destination, rcpt, msg);
            return;
        }
        Date untilDate = account.getGeneralizedTimeAttr(Provisioning.A_zimbraPrefOutOfOfficeUntilDate, null);
        if (untilDate != null && now.after(untilDate)) {
            ofailed("until date reached", destination, rcpt, msg);
            return;
        }

        // Get the JavaMail mime message - we have to look at headers to
        // see this message qualifies for an out of office response.
        MimeMessage mm = msg.getMimeMessage();

        // If envelope sender is empty
        if (envSenderString == null) {
            ofailed("envelope sender null", destination, rcpt, msg);
            return;
        }
        if (envSenderString.length() < 1) {
            ofailed("envelope sender empty", destination, rcpt, msg); // be conservative
            return;
        }
        InternetAddress envSender;
        try {
            // NB: 'strict' being 'true' causes <> to except
            envSender = new JavaMailInternetAddress(envSenderString, true);
        } catch (AddressException ae) {
            ofailed("envelope sender invalid", envSenderString, rcpt, msg, ae);
            return;
        }
        destination = envSender.getAddress();

        if (Mime.isAutoSubmitted(mm)) {
            ofailed("auto-submitted not no", destination, rcpt, msg);
            return;
        }

        // If precedence is bulk, junk or list
        String[] precedence = mm.getHeader("Precedence");
        if (hasPrecedence(precedence, "bulk")) {
            ofailed("precedence bulk", destination, rcpt, msg);
            return;
        } else if (hasPrecedence(precedence, "junk")) {
            ofailed("precedence junk", destination, rcpt, msg);
            return;
        } else if (hasPrecedence(precedence, "list")) {
            ofailed("precedence list", destination, rcpt, msg);
            return;
        }

        // Check if the envelope sender indicates a mailing list owner and such
        String[] envSenderAddrParts = EmailUtil.getLocalPartAndDomain(destination);
        if (envSenderAddrParts == null) {
            ofailed("envelope sender invalid", destination, rcpt, msg);
            return;
        }
        String envSenderLocalPart = envSenderAddrParts[0];
        envSenderLocalPart = envSenderLocalPart.toLowerCase();
        if (envSenderLocalPart.startsWith("owner-") || envSenderLocalPart.endsWith("-owner")) {
            ofailed("envelope sender has owner- or -owner", destination, rcpt, msg);
            return;
        }
        if (envSenderLocalPart.indexOf("-request") != -1) {
            ofailed("envelope sender contains -request", destination, rcpt, msg);
            return;
        }
        if (envSenderLocalPart.equals("mailer-daemon")) {
            ofailed("envelope sender is mailer-daemon", destination, rcpt, msg);
            return;
        }
        if (envSenderLocalPart.equals("majordomo")) {
            ofailed("envelope sender is majordomo", destination, rcpt, msg);
            return;
        }
        if (envSenderLocalPart.equals("listserv")) {
            ofailed("envelope sender is listserv", destination, rcpt, msg);
            return;
        }

        // multipart/report is also machine generated
        String ct = mm.getContentType();
        if (ct != null && ct.equalsIgnoreCase("multipart/report")) {
            ofailed("content-type multipart/report", destination, rcpt, msg);
            return;
        }

        // Check if recipient was directly mentioned in to/cc of this message
        String[] otherAccountAddrs = account.getMultiAttr(Provisioning.A_zimbraPrefOutOfOfficeDirectAddress);
        if (!AccountUtil.isDirectRecipient(account, otherAccountAddrs, mm, OUT_OF_OFFICE_DIRECT_CHECK_NUM_RECIPIENTS)) {
            ofailed("not direct", destination, rcpt, msg);
            return;
        }

        // If we've already sent to this user, do not send again
        DbConnection conn = null;
        try {
            conn = DbPool.getConnection(mbox);
            if (DbOutOfOffice.alreadySent(conn, mbox, destination, account.getTimeInterval(Provisioning.A_zimbraPrefOutOfOfficeCacheDuration, DEFAULT_OUT_OF_OFFICE_CACHE_DURATION_MILLIS))) {
                ofailed("already sent", destination, rcpt, msg);
                return;
            }
        } finally {
            DbPool.quietClose(conn);
        }

        // Send the message
        try {
            SMTPMessage out = new SMTPMessage(JMSession.getSmtpSession());

            // Set From and Reply-To.
            out.setFrom(AccountUtil.getFromAddress(account));
            InternetAddress replyTo = AccountUtil.getReplyToAddress(account);
            if (replyTo != null) {
                out.setReplyTo(new Address[] { replyTo });
            }

            // To
            out.setRecipient(javax.mail.Message.RecipientType.TO, envSender);

            // Date
            out.setSentDate(new Date());

            // Subject
            String subject = "Re: " + msg.getSubject();
            String charset = getCharset(account, subject);
            out.setSubject(subject, charset);

            // In-Reply-To
            String messageId = mm.getMessageID();
            if (messageId != null && !messageId.trim().equals("")) {
                out.setHeader("In-Reply-To", messageId);
            }

            // Auto-Submitted
            out.setHeader("Auto-Submitted", "auto-replied (zimbra; vacation)");

            // Precedence (discourage older systems from responding)
            out.setHeader("Precedence", "bulk");

            // Body
            // check whether to send "external" OOO reply
            boolean sendExternalReply = account.isPrefOutOfOfficeExternalReplyEnabled() &&
                    !isInternalSender(destination, account) && isOfExternalSenderType(destination, account, mbox);
            String body = account.getAttr(sendExternalReply ?
                    Provisioning.A_zimbraPrefOutOfOfficeExternalReply : Provisioning.A_zimbraPrefOutOfOfficeReply, "");
            charset = getCharset(account, body);
            out.setText(body, charset);

            if (Provisioning.getInstance().getConfig().getBooleanAttr(Provisioning.A_zimbraAutoSubmittedNullReturnPath, true)) {
                out.setEnvelopeFrom("<>");
            } else {
                out.setEnvelopeFrom(account.getName());
            }

            MailSender sender = mbox.getMailSender();
            sender.setSaveToSent(false);
            sender.sendMimeMessage(null, mbox, out);
            ZimbraLog.mailbox.info("outofoffice sent dest='" + destination + "' rcpt='" + rcpt + "' mid=" + msg.getId());

            // Save so we will not send to again
            try {
                conn = DbPool.getConnection(mbox);
                DbOutOfOffice.setSentTime(conn, mbox, destination);
                conn.commit();
            } finally {
                DbPool.quietClose(conn);
            }
        } catch (MessagingException me) {
            ofailed("send failed", destination, rcpt, msg, me);
            return;
        }
    }

    private static boolean isInternalSender(String senderAddr, Account account) {
        String[] senderAddrParts = EmailUtil.getLocalPartAndDomain(senderAddr);
        String senderDomain = senderAddrParts[1];
        if (account.getDomainName().equalsIgnoreCase(senderDomain)) {
            // same domain
            return true;
        }
        for (String intDom : account.getInternalSendersDomain()) {
            if (intDom.equalsIgnoreCase(senderDomain)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOfExternalSenderType(String senderAddr, Account account, Mailbox mbox)
            throws ServiceException {
        switch (account.getPrefExternalSendersType()) {
            case ALLNOTINAB:
                return !mbox.index.existsInContacts(Sets.newHashSet(
                        new com.zimbra.common.mime.InternetAddress(senderAddr)));
            case ALL:
            default:
                return true;
        }
    }

    private String getCharset(Account account, String data) {
        String requestedCharset = account.getAttr(Provisioning.A_zimbraPrefMailDefaultCharset, MimeConstants.P_CHARSET_UTF8);
        return CharsetUtil.checkCharset(data, requestedCharset);
    }

    /**
     * If the recipient's account is set up for email notification, sends a notification
     * message to the user's notification address.
     */
    private void notifyIfNecessary(Account account, Message msg, String rcpt)
    throws MessagingException, ServiceException {
        // Does user have new mail notification turned on
        boolean notify = account.getBooleanAttr(Provisioning.A_zimbraPrefNewMailNotificationEnabled, false);
        if (!notify) {
            return;
        }

        // Validate notification address
        String destination = account.getAttr(Provisioning.A_zimbraPrefNewMailNotificationAddress);
        if (destination == null) {
            nfailed("destination not set", null, rcpt, msg, null);
            return;
        }
        try {
            new JavaMailInternetAddress(destination);
        } catch (AddressException ae) {
            nfailed("invalid destination", destination, rcpt, msg, ae);
            return;
        }

        // Reject if spam or trash.  If a message ends up in the trash as a result of the user's
        // filter rules, we assume it's not interesting.
        if (msg.inSpam()) {
            nfailed("in spam", destination, rcpt, msg);
            return;
        }

        try {
            if (msg.inTrash()) {
                nfailed("in trash", destination, rcpt, msg);
                return;
            }
        } catch (ServiceException e) {
            nfailed("call to Message.inTrash() failed", destination, rcpt, msg, e);
            return;
        }

        // If precedence is bulk or junk
        MimeMessage mm = msg.getMimeMessage();
        String[] precedence = mm.getHeader("Precedence");
        if (hasPrecedence(precedence, "bulk")) {
            nfailed("precedence bulk", destination, rcpt, msg);
            return;
        }
        if (hasPrecedence(precedence, "junk")) {
            nfailed("precedence junk", destination, rcpt, msg);
            return;
        }

        // Check for mail loop
        String[] autoSubmittedHeaders = mm.getHeader("Auto-Submitted");
        if (autoSubmittedHeaders != null) {
            for (int i = 0; i < autoSubmittedHeaders.length; i++) {
                String headerValue= autoSubmittedHeaders[i].toLowerCase();
                if (headerValue.indexOf("notification") != -1) {
                    nfailed("detected a mail loop", destination, rcpt, msg);
                    return;
                }
            }
        }

        // Assemble message components
        String from = account.getAttr(Provisioning.A_zimbraNewMailNotificationFrom);
        String subject = account.getAttr(Provisioning.A_zimbraNewMailNotificationSubject);
        String body = account.getAttr(Provisioning.A_zimbraNewMailNotificationBody);
        if (from == null || subject == null || body == null) {
            nfailed("null from, subject or body", destination, rcpt, msg);
            return;
        }
        String recipientDomain = getDomain(rcpt);

        Map<String, String> vars = new HashMap<String, String>();
        vars.put("SENDER_ADDRESS", msg.getSender());
        vars.put("RECIPIENT_ADDRESS", rcpt);
        vars.put("RECIPIENT_DOMAIN", recipientDomain);
        vars.put("NOTIFICATION_ADDRESS", destination);
        vars.put("SUBJECT", msg.getSubject());
        vars.put("NEWLINE", "\n");

        from = StringUtil.fillTemplate(from, vars);
        subject = StringUtil.fillTemplate(subject, vars);
        body = StringUtil.fillTemplate(body, vars);

        // Send the message
        try {
            Session smtpSession = JMSession.getSmtpSession();
            MimeMessage out = new JavaMailMimeMessage(smtpSession);
            out.setHeader("Auto-Submitted", "auto-replied (notification; " + rcpt + ")");
            InternetAddress address = new JavaMailInternetAddress(from);
            out.setFrom(address);
            address = new JavaMailInternetAddress(destination);
            out.setRecipient(javax.mail.Message.RecipientType.TO, address);

            String charset = getCharset(account, subject);
            out.setSubject(subject, charset);
            charset = getCharset(account, body);
            out.setText(body, charset);

            String envFrom = "<>";
            try {
                if (!Provisioning.getInstance().getConfig().getBooleanAttr(Provisioning.A_zimbraAutoSubmittedNullReturnPath, true)) {
                    envFrom = account.getName();
                }
            } catch (ServiceException se) {
                ZimbraLog.mailbox.warn("error encoutered looking up return path configuration, using null return path instead", se);
            }
            smtpSession.getProperties().setProperty("mail.smtp.from", envFrom);

            Transport.send(out);
            ZimbraLog.mailbox.info("notification sent dest='" + destination + "' rcpt='" + rcpt + "' mid=" + msg.getId());
        } catch (MessagingException me) {
            nfailed("send failed", destination, rcpt, msg, me);
        }
    }

    private static String getDomain(String address) {
        String[] parts = EmailUtil.getLocalPartAndDomain(address);
        if (parts == null) {
            return null;
        }
        return parts[1];
    }

    /**
     * If <tt>zimbraInterceptAddress</tt> is specified, sends a message to that
     * address with the given message attached.
     *
     * @param operation name of the operation being performed (send, add message, save draft, etc.)
     * @param folder the folder that the message was filed into, or <tt>null</tt>
     */
    void interceptIfNecessary(Mailbox mbox, MimeMessage msg, String operation, Folder folder)
    throws ServiceException {
            // Don't do anything if intercept is turned off.
            Account account = mbox.getAccount();
            String[] interceptAddresses = account.getMultiAttr(Provisioning.A_zimbraInterceptAddress);
            if (interceptAddresses.length == 0) {
                return;
            }

            for (String interceptAddress : interceptAddresses) {
                try {
                    ZimbraLog.mailbox.info("Sending intercept of message %s to %s.", msg.getMessageID(), interceptAddress);

                    // Fill templates
                    String folderName = "none";
                    String folderId = "none";
                    if (folder != null) {
                        folderName = folder.getName();
                        folderId = Integer.toString(folder.getId());
                    }
                    Map<String, String> vars = new HashMap<String, String>();
                    vars.put("ACCOUNT_DOMAIN", getDomain(account.getName()));
                    vars.put("ACCOUNT_ADDRESS", account.getName());
                    vars.put("MESSAGE_SUBJECT", Mime.getSubject(msg));
                    vars.put("OPERATION", operation);
                    vars.put("FOLDER_NAME", folderName);
                    vars.put("FOLDER_ID", folderId);
                    vars.put("NEWLINE", "\r\n");

                    String from = StringUtil.fillTemplate(account.getAttr(Provisioning.A_zimbraInterceptFrom), vars);
                    String subject = StringUtil.fillTemplate(account.getAttr(Provisioning.A_zimbraInterceptSubject), vars);
                    String bodyText = StringUtil.fillTemplate(account.getAttr(Provisioning.A_zimbraInterceptBody), vars);

                    // Assemble outgoing message
                    MimeMessage attached = msg;
                    boolean headersOnly = account.getBooleanAttr(Provisioning.A_zimbraInterceptSendHeadersOnly, false);
                    if (headersOnly) {
                        attached = new MimeMessageWithId(msg.getMessageID());
                        Enumeration e = msg.getAllHeaderLines();
                        while (e.hasMoreElements()) {
                            attached.addHeaderLine((String) e.nextElement());
                        }
                        attached.setContent("", msg.getContentType());
                        attached.saveChanges();
                    }

                    SMTPMessage out = new SMTPMessage(JMSession.getSmtpSession());
                    out.setHeader("Auto-Submitted", "auto-replied (zimbra; intercept)");
                    InternetAddress address = new JavaMailInternetAddress(from);
                    out.setFrom(address);

                    address = new JavaMailInternetAddress(interceptAddress);
                    out.setRecipient(javax.mail.Message.RecipientType.TO, address);

                    String charset = getCharset(account, subject);
                    out.setSubject(subject, charset);
                    charset = getCharset(account, bodyText);

                    MimeMultipart multi = new JavaMailMimeMultipart();

                    // Add message body
                    MimeBodyPart part = new JavaMailMimeBodyPart();
                    part.setText(bodyText, charset);
                    multi.addBodyPart(part);

                    // Add original message
                    MimeBodyPart part2 = new JavaMailMimeBodyPart();
                    part2.setContent(attached, MimeConstants.CT_MESSAGE_RFC822);
                    multi.addBodyPart(part2);

                    out.setContent(multi);
                    String envFrom = "<>";
                    out.setEnvelopeFrom(envFrom);

                    out.saveChanges();
                    Transport.send(out);

                    // clean up after ourselves...
                    multi.removeBodyPart(part2);
                } catch (MessagingException e) {
                    ZimbraLog.lmtp.warn("Unable to send intercept message to %s.", interceptAddress, e);
                }
            }
    }

    /**
     * Returns <code>true</code> if the array is not <code>null</code>
     * and contains the specified value.
     */
    private static boolean hasPrecedence(String[] precedence, String value) {
        if (precedence != null) {
            for (int i = 0; i < precedence.length; i++) {
                if (precedence[i].equalsIgnoreCase(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void failed(String op, String why, String destAddr, String rcptAddr, Message msg, Exception e) {
        StringBuffer sb = new StringBuffer(128);
        sb.append(op).append(" not sent (");
        sb.append(why).append(")");
        sb.append(" mid=").append(msg.getId());
        sb.append(" rcpt='").append(rcptAddr).append("'");
        if (destAddr != null) {
            sb.append(" dest='").append(destAddr).append("'");
        }
        ZimbraLog.mailbox.info(sb.toString(), e);
    }

    private static void nfailed(String why, String destAddr, String rcptAddr, Message msg, Exception e) {
        failed("notification", why, destAddr, rcptAddr, msg, e);
    }

    private static void nfailed(String why, String destAddr, String rcptAddr, Message msg) {
        failed("notification", why, destAddr, rcptAddr, msg, null);
    }

    private static void ofailed(String why, String destAddr, String rcptAddr, Message msg, Exception e) {
        failed("outofoffice", why, destAddr, rcptAddr, msg, e);
    }

    private static void ofailed(String why, String destAddr, String rcptAddr, Message msg) {
        failed("outofoffice", why, destAddr, rcptAddr, msg, null);
    }
}
