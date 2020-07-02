/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

package com.zimbra.cs.mailbox;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MailDateFormat;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.sun.mail.smtp.SMTPMessage;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CharsetUtil;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.EmailUtil;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.zmime.ZInternetHeader;
import com.zimbra.common.zmime.ZMimeBodyPart;
import com.zimbra.common.zmime.ZMimeMessage;
import com.zimbra.common.zmime.ZMimeMultipart;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbOutOfOffice;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.lmtpserver.LmtpCallback;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.JMSession;

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
    private class MimeMessageWithId extends ZMimeMessage {

        private final String mMessageId;

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
            outOfOfficeIfNecessary(account, mbox, newMessage, recipientEmail, envelopeSender);
        } catch (MessagingException e) {
            ZimbraLog.mailbox.warn("Unable to send out-of-office reply", e);
        } catch (ServiceException e) {
            ZimbraLog.mailbox.warn("Unable to send out-of-office reply", e);
        }
    }

    @Override
    public void forwardWithoutDelivery(Account account, Mailbox mbox, String envelopeSender,
            String recipientEmail, ParsedMessage pm) {
        try {
            outOfOfficeIfNecessary(account, mbox, pm, recipientEmail, envelopeSender);
        } catch (Exception e) {
            ZimbraLog.mailbox.warn("Unable to send out-of-office reply", e);
        }
    }

    public static Notification getInstance() {
        return sInstance;
    }

    private void outOfOfficeIfNecessary(Account account, Mailbox mbox, ParsedMessage pm,
            String rcpt, String envSenderString)
    throws ServiceException, MessagingException {
        outOfOfficeIfNecessary(account, mbox, pm.getMimeMessage(), null, rcpt, envSenderString);
    }

    private void outOfOfficeIfNecessary(Account account, Mailbox mbox, Message msg,
            String rcpt, String envSenderString)
    throws ServiceException, MessagingException {
        // Reject if spam or trash.  If a message ends up in the trash as a result of the user's
        // filter rules, we assume it's not interesting.
        if (msg.inSpam()) {
            ofailed("in spam", null, rcpt, msg.getId());
            return;
        }
        if (msg.inTrash()) {
            ofailed("in trash", null, rcpt, msg.getId());
            return;
        }
        outOfOfficeIfNecessary(account, mbox, msg.getMimeMessage(), msg.getId(), rcpt, envSenderString);
    }

    /**
     * If the recipient's account requires out of office notification,
     * send it out.  We send these out based on users' setting, and
     * the incoming message meeting certain criteria.
     */
    private void outOfOfficeIfNecessary(Account account, Mailbox mbox, MimeMessage mm, Integer msgId,
            String rcpt, String envSenderString)
    throws ServiceException, MessagingException {

        boolean replyEnabled = account.isPrefOutOfOfficeReplyEnabled();
        if (ZimbraLog.mailbox.isDebugEnabled()) {
            ZimbraLog.mailbox.debug("outofoffice reply enabled=" + replyEnabled + " rcpt='" + rcpt + "' mid=" + msgId);
        }
        if (!replyEnabled) {
            return;
        }

        // Check if we are in any configured out of office interval
        Date now = new Date();
        Date fromDate = account.getGeneralizedTimeAttr(Provisioning.A_zimbraPrefOutOfOfficeFromDate, null);
        if (fromDate != null && now.before(fromDate)) {
            ofailed("from date not reached", null, rcpt, msgId);
            return;
        }
        Date untilDate = account.getGeneralizedTimeAttr(Provisioning.A_zimbraPrefOutOfOfficeUntilDate, null);
        if (untilDate != null && now.after(untilDate)) {
            ofailed("until date reached", null, rcpt, msgId);
            return;
        }

        // If envelope sender is empty
        if (envSenderString == null) {
            ofailed("envelope sender null", null, rcpt, msgId);
            return;
        }
        if (envSenderString.length() < 1) {
            ofailed("envelope sender empty", null, rcpt, msgId); // be conservative
            return;
        }

        InternetAddress envSender;
        try {
            // NB: 'strict' being 'true' causes <> to except
            envSender = new JavaMailInternetAddress(envSenderString, true);
        } catch (AddressException ae) {
            ofailed("envelope sender invalid", envSenderString, rcpt, msgId, ae);
            return;
        }
        String destination = envSender.getAddress();

        if (Mime.isAutoSubmitted(mm)) {
            ofailed("auto-submitted not no", destination, rcpt, msgId);
            return;
        }

        // If precedence is bulk, junk or list
        String[] precedence = mm.getHeader("Precedence");
        if (hasPrecedence(precedence, "bulk")) {
            ofailed("precedence bulk", destination, rcpt, msgId);
            return;
        } else if (hasPrecedence(precedence, "junk")) {
            ofailed("precedence junk", destination, rcpt, msgId);
            return;
        } else if (hasPrecedence(precedence, "list")) {
            ofailed("precedence list", destination, rcpt, msgId);
            return;
        }

        // Check if the envelope sender indicates a mailing list owner and such
        String[] envSenderAddrParts = EmailUtil.getLocalPartAndDomain(destination);
        if (envSenderAddrParts == null) {
            ofailed("envelope sender invalid", destination, rcpt, msgId);
            return;
        }
        String envSenderLocalPart = envSenderAddrParts[0];
        envSenderLocalPart = envSenderLocalPart.toLowerCase();
        if (envSenderLocalPart.startsWith("owner-") || envSenderLocalPart.endsWith("-owner")) {
            ofailed("envelope sender has owner- or -owner", destination, rcpt, msgId);
            return;
        }
        if (envSenderLocalPart.contains("-request")) {
            ofailed("envelope sender contains -request", destination, rcpt, msgId);
            return;
        }
        if (envSenderLocalPart.equals("mailer-daemon")) {
            ofailed("envelope sender is mailer-daemon", destination, rcpt, msgId);
            return;
        }
        if (envSenderLocalPart.equals("majordomo")) {
            ofailed("envelope sender is majordomo", destination, rcpt, msgId);
            return;
        }
        if (envSenderLocalPart.equals("listserv")) {
            ofailed("envelope sender is listserv", destination, rcpt, msgId);
            return;
        }

        // multipart/report is also machine generated
        String ct = mm.getContentType();
        if (ct != null && ct.equalsIgnoreCase("multipart/report")) {
            ofailed("content-type multipart/report", destination, rcpt, msgId);
            return;
        }

        // Check if recipient was directly mentioned in to/cc of this message
        String[] otherAccountAddrs = account.getMultiAttr(Provisioning.A_zimbraPrefOutOfOfficeDirectAddress);
        if (!AccountUtil.isDirectRecipient(account, otherAccountAddrs, mm, OUT_OF_OFFICE_DIRECT_CHECK_NUM_RECIPIENTS)) {
            ofailed("not direct", destination, rcpt, msgId);
            return;
        }

        // If we've already sent to this user, do not send again
        DbConnection conn = null;
        try {
            conn = DbPool.getConnection(mbox);
            if (DbOutOfOffice.alreadySent(conn, mbox, destination, account.getTimeInterval(Provisioning.A_zimbraPrefOutOfOfficeCacheDuration, DEFAULT_OUT_OF_OFFICE_CACHE_DURATION_MILLIS))) {
                ofailed("already sent", destination, rcpt, msgId);
                return;
            }
        } finally {
            DbPool.quietClose(conn);
        }

        // Send the message
        try {
            SMTPMessage out = AccountUtil.getSmtpMessageObj(account);

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
            String subject = Mime.getSubject(mm);
            String replySubjectPrefix = L10nUtil.getMessage(L10nUtil.MsgKey.replySubjectPrefix, account.getLocale());
            if (subject == null) {
                subject = replySubjectPrefix;
            } else if (!subject.toLowerCase().startsWith(replySubjectPrefix.toLowerCase())) {
                subject = replySubjectPrefix + " " + subject;
            }
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
            if (skipOutOfOfficeMsg(destination, account, mbox)) {
                ZimbraLog.mailbox.info("%s is external user and no external reply option is set, so no OOO will be sent.", destination);
                return;
            }
            boolean sendExternalReply = sendOutOfOfficeExternalReply(destination, account, mbox);
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
            sender.setDsnNotifyOptions(MailSender.DsnNotifyOption.NEVER);
            sender.sendMimeMessage(null, mbox, out);
            ZimbraLog.mailbox.info("outofoffice sent dest='" + destination + "' rcpt='" + rcpt + "' mid=" + msgId);

            // Save so we will not send to again
            try {
                conn = DbPool.getConnection(mbox);
                DbOutOfOffice.setSentTime(conn, mbox, destination);
                conn.commit();
            } finally {
                DbPool.quietClose(conn);
            }
        } catch (MessagingException me) {
            ofailed("send failed", destination, rcpt, msgId, me);
        }
    }

    /**
     * whether OutOfOffice message has to be sent to external sender or not
     * @return   true  - message should  not be sent
     *           false - message should be sent
     */
    public static boolean skipOutOfOfficeMsg(String senderAddr, Account account, Mailbox mbox) {
        return account.isPrefOutOfOfficeSuppressExternalReply() && isOfExternalSenderType(senderAddr, account, mbox)
                &&  !isInternalSender(senderAddr, account) && !isOfSpecificDomainSenderType(senderAddr, account);
    }

    /**
     * standard Out of Office standard message should be sent or custom message
     * @return    true - custom message should be sent
     *            false - standard message should be sent
     */
    public static boolean sendOutOfOfficeExternalReply(String senderAddr, Account account, Mailbox mbox) {
        boolean sendExternalReply = account.isPrefOutOfOfficeExternalReplyEnabled()
                && !isInternalSender(senderAddr, account) && isOfExternalSenderType(senderAddr, account, mbox);
         sendExternalReply = sendExternalReply || isOfSpecificDomainSenderType(senderAddr, account);
         return sendExternalReply;
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

    private static boolean isOfExternalSenderType(String senderAddr, Account account, Mailbox mbox) {
        switch (account.getPrefExternalSendersType()) {
            case ALLNOTINAB:
                try {
                    return !mbox.index.existsInContacts(
                            Collections.singleton(new com.zimbra.common.mime.InternetAddress(senderAddr)));
                } catch (IOException | ServiceException e) {
                    ZimbraLog.mailbox.error("Failed to lookup contacts", e);
                    return true;
                }
            case INAB:
                try {
                    return mbox.index.existsInContacts(Collections
                        .singleton(new com.zimbra.common.mime.InternetAddress(senderAddr)));
                } catch (IOException | ServiceException e) {
                    ZimbraLog.mailbox.error("Failed to lookup contacts", e);
                    return true;
                }
            case ALL:
            default:
                return true;
        }
    }

    private static boolean isOfSpecificDomainSenderType(String senderAddr, Account account) {
        if (account.getPrefExternalSendersType().isINSD()) {
            String[] senderAddrParts = EmailUtil.getLocalPartAndDomain(senderAddr);
            String senderDomain = senderAddrParts[1];
            for (String specificDom : account.getPrefOutOfOfficeSpecificDomains()) {
                if (specificDom.equalsIgnoreCase(senderDomain)) {
                    return true;
                }
            }
        }
        return false;
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

        // Send the message
        try {
            Domain domain = Provisioning.getInstance().getDomain(account);
            Session smtpSession = JMSession.getSmtpSession(domain);

            // Assemble message components
            MimeMessage out = assembleNotificationMessage(account, msg, rcpt, destination, smtpSession);
            if (out == null) {
            	return;
            }
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

            MailSender.logMessage(out, out.getAllRecipients(), envFrom,
                smtpSession.getProperties().getProperty("mail.smtp.host"), String.valueOf(msg.getId()), null, null, "notify");
        } catch (MessagingException me) {
            nfailed("send failed", destination, rcpt, msg, me);
        }
    }

    private MimeMessage assembleNotificationMessage(Account account, Message msg, String rcpt,
			String destination, Session smtpSession)
		throws MessagingException {

    	String recipientDomain = getDomain(rcpt);

        Map<String, String> vars = new HashMap<String, String>();
        vars.put("SENDER_ADDRESS", ZInternetHeader.decode(msg.getSender()));
        vars.put("RECIPIENT_ADDRESS", rcpt);
        vars.put("RECIPIENT_DOMAIN", recipientDomain);
        vars.put("NOTIFICATION_ADDRESS", destination);
        vars.put("SUBJECT", msg.getSubject());
        vars.put("DATE", new MailDateFormat().format(new Date()));
        vars.put("NEWLINE", "\n");


        MimeMessage out = null;
        String template = account.getAttr(Provisioning.A_zimbraNewMailNotificationMessage, null);
        if (template != null) {
            String msgBody = StringUtil.fillTemplate(template, vars);
            InputStream is = new ByteArrayInputStream(msgBody.getBytes());
            out = new MimeMessage(smtpSession, is);
            InternetAddress address = new JavaMailInternetAddress(destination);
            out.setRecipient(javax.mail.Message.RecipientType.TO, address);
        } else {
            out = new ZMimeMessage(smtpSession);

            String from = account.getAttr(Provisioning.A_zimbraNewMailNotificationFrom);
            String subject = account.getAttr(Provisioning.A_zimbraNewMailNotificationSubject);
            String body = account.getAttr(Provisioning.A_zimbraNewMailNotificationBody);
            if (from == null || subject == null || body == null) {
                nfailed("null from, subject or body", destination, rcpt, msg);
                return null;
            }

            from = StringUtil.fillTemplate(from, vars);
            subject = StringUtil.fillTemplate(subject, vars);
            body = StringUtil.fillTemplate(body, vars);

            InternetAddress address = new JavaMailInternetAddress(from);
            out.setFrom(address);
            address = new JavaMailInternetAddress(destination);
            out.setRecipient(javax.mail.Message.RecipientType.TO, address);

            String charset = getCharset(account, subject);
            out.setSubject(subject, charset);
            charset = getCharset(account, body);
            out.setText(body, charset);
        }
        if (out != null) {
                out.setHeader("Auto-Submitted", "auto-replied (notification; " + rcpt + ")");
        }
        return out;
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

                    SMTPMessage out = AccountUtil.getSmtpMessageObj(account);
                    out.setHeader("Auto-Submitted", "auto-replied (zimbra; intercept)");
                    InternetAddress address = new JavaMailInternetAddress(from);
                    out.setFrom(address);

                    address = new JavaMailInternetAddress(interceptAddress);
                    out.setRecipient(javax.mail.Message.RecipientType.TO, address);

                    String charset = getCharset(account, subject);
                    out.setSubject(subject, charset);
                    charset = getCharset(account, bodyText);

                    MimeMultipart multi = new ZMimeMultipart();

                    // Add message body
                    MimeBodyPart part = new ZMimeBodyPart();
                    part.setText(bodyText, charset);
                    multi.addBodyPart(part);

                    // Add original message
                    MimeBodyPart part2 = new ZMimeBodyPart();
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
        failed(op, why, destAddr, rcptAddr, msg.getId(), e);
    }

    private static void failed(String op, String why, String destAddr, String rcptAddr, Integer msgId, Exception e) {
        StringBuffer sb = new StringBuffer(128);
        sb.append(op).append(" not sent (");
        sb.append(why).append(")");
        sb.append(" mid=").append(msgId);
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

    private static void ofailed(String why, String destAddr, String rcptAddr, Integer msgId, Exception e) {
        failed("outofoffice", why, destAddr, rcptAddr, msgId, e);
    }

    private static void ofailed(String why, String destAddr, String rcptAddr, Integer msgId) {
        failed("outofoffice", why, destAddr, rcptAddr, msgId, null);
    }
}
