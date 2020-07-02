/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 Synacor, Inc.
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
package com.zimbra.cs.filter;

import static com.zimbra.cs.filter.JsieveConfigMapHandler.CAPABILITY_VARIABLES;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Address;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jsieve.exception.SyntaxException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.sun.mail.smtp.SMTPMessage;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ArrayUtil;
import com.zimbra.common.util.CharsetUtil;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.L10nUtil.MsgKey;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.zmime.ZInternetHeader;
import com.zimbra.common.zmime.ZMimeBodyPart;
import com.zimbra.common.zmime.ZMimeMultipart;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.filter.jsieve.ActionFlag;
import com.zimbra.cs.filter.jsieve.Require;
import com.zimbra.cs.filter.jsieve.SetVariable;
import com.zimbra.cs.lmtpserver.LmtpEnvelope;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.MessageCallbackContext;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.Mailbox.MessageCallback.Type;
import com.zimbra.cs.mime.MPartInfo;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.JMSession;

public final class FilterUtil {

    private FilterUtil() {
    }

    /**
     * Returns a Sieve-escaped version of the given string.  Replaces <tt>\</tt> with
     * <tt>\\</tt> and <tt>&quot;</tt> with <tt>\&quot;</tt>.
     */
    public static String escape(String s) {
        if (s == null || s.length() == 0) {
            return s;
        }
        s = s.replace("\\", "\\\\");
        s = s.replace("\"", "\\\"");
        return s;
    }

    /**
     * Adds a value to the given <tt>Map</tt>.  If <tt>initialKey</tt> already
     * exists in the map, uses the next available index instead.  This way we
     * guarantee that we don't lose data if the client sends two elements with
     * the same index, or doesn't specify the index at all.
     *
     * @return the index used to insert the value
     */
    public static <T> int addToMap(Map<Integer, T> map, int initialKey, T value) {
        int i = initialKey;
        while (true) {
            if (!map.containsKey(i)) {
                map.put(i, value);
                return i;
            }
            i++;
        }
    }

    public static int getIndex(Element actionElement) {
        String s = actionElement.getAttribute(MailConstants.A_INDEX, "0");
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            ZimbraLog.soap.warn("Unable to parse index value %s for element %s.  Ignoring order.",
                s, actionElement.getName());
            return 0;
        }
    }

    /**
     * Parses a Sieve size string and returns the integer value.
     * The string may end with <tt>K</tt> (kilobytes), <tt>M</tt> (megabytes)
     * or <tt>G</tt> (gigabytes).  If the units are not specified, the
     * value is in bytes.
     *
     * @throws NumberFormatException if the value cannot be parsed
     */
    public static int parseSize(String sizeString) {
        if (sizeString == null || sizeString.length() == 0) {
            return 0;
        }
        sizeString = sizeString.toUpperCase();
        int multiplier = 1;
        if (sizeString.endsWith("K")) {
            sizeString = sizeString.substring(0, sizeString.length() - 1);
            multiplier = 1024;
        } else if (sizeString.endsWith("M")) {
            sizeString = sizeString.substring(0, sizeString.length() - 1);
            multiplier = 1024 * 1024;
        } else if (sizeString.endsWith("G")) {
            sizeString = sizeString.substring(0, sizeString.length() - 1);
            multiplier = 1024 * 1024 * 1024;
        }
        return Integer.parseInt(sizeString) * multiplier;
    }

    /**
     * Adds a message to the given folder.  Handles both local folders and mountpoints.
     * @return the id of the new message, or <tt>null</tt> if it was a duplicate
     */
    public static ItemId addMessage(DeliveryContext context, Mailbox mbox, ParsedMessage pm, String recipient,
                                    String folderPath, boolean noICal, int flags, String[] tags, int convId, OperationContext octxt,
                                    MessageCallbackContext ctxt)
    throws ServiceException {
        // Do initial lookup.
        Pair<Folder, String> folderAndPath = mbox.getFolderByPathLongestMatch(
            octxt, Mailbox.ID_FOLDER_USER_ROOT, folderPath);
        Folder folder = folderAndPath.getFirst();
        String remainingPath = folderAndPath.getSecond();
        ZimbraLog.filter.debug("Attempting to file to %s, remainingPath=%s.", folder, remainingPath);

        if (folder instanceof Mountpoint) {
            Mountpoint mountpoint = (Mountpoint) folder;
            ZMailbox remoteMbox = getRemoteZMailbox(mbox, mountpoint);

            // Look up remote folder.
            String remoteAccountId = mountpoint.getOwnerId();
            ItemId id = mountpoint.getTarget();
            ZFolder remoteFolder = remoteMbox.getFolderById(id.toString());
            if (remoteFolder != null) {
                if (remainingPath != null) {
                    remoteFolder = remoteFolder.getSubFolderByPath(remainingPath);
                    if (remoteFolder == null) {
                        String msg = String.format("Subfolder %s of mountpoint %s does not exist.",
                            remainingPath, mountpoint.getName());
                        throw ServiceException.FAILURE(msg, null);
                    }
                }
            }

            // File to remote folder.
            if (remoteFolder != null) {
                byte[] content;
                try {
                    content = pm.getRawData();
                } catch (Exception e) {
                    throw ServiceException.FAILURE("Unable to get message content", e);
                }
                String msgId = remoteMbox.addMessage(remoteFolder.getId(),
                        com.zimbra.cs.mailbox.Flag.toString(flags), null, 0, content, false);
                return new ItemId(msgId, remoteAccountId);
            } else {
                String msg = String.format("Unable to find remote folder %s for mountpoint %s.",
                    remainingPath, mountpoint.getName());
                throw ServiceException.FAILURE(msg, null);
            }
        } else {
            if (!StringUtil.isNullOrEmpty(remainingPath)) {
                // Only part of the folder path matched.  Auto-create the remaining path.
                ZimbraLog.filter.info("Could not find folder %s.  Automatically creating it.",
                    folderPath);
                folder = mbox.createFolder(octxt, folderPath, new Folder.FolderOptions().setDefaultView(MailItem.Type.MESSAGE));
            }
            try {
                DeliveryOptions dopt = new DeliveryOptions().setFolderId(folder).setNoICal(noICal);
                dopt.setFlags(flags).setTags(tags).setConversationId(convId).setRecipientEmail(recipient);
                if (ctxt != null) {
                    dopt.setCallbackContext(ctxt);
                }
                Message msg = mbox.addMessage(octxt, pm, dopt, context);
                if (msg == null) {
                    return null;
                } else {
                    return new ItemId(msg);
                }
            } catch (IOException e) {
                throw ServiceException.FAILURE("Unable to add message", e);
            }
        }
    }

    /**
     * Returns a <tt>ZMailbox</tt> for the remote mailbox referenced by the given
     * <tt>Mountpoint</tt>.
     */
    public static ZMailbox getRemoteZMailbox(Mailbox localMbox, Mountpoint mountpoint)
    throws ServiceException {
        // Get auth token
        AuthToken authToken = null;
        OperationContext opCtxt = localMbox.getOperationContext();
        if (opCtxt != null) {
            authToken = AuthToken.getCsrfUnsecuredAuthToken(opCtxt.getAuthToken());
        }
        if (authToken == null) {
            authToken = AuthProvider.getAuthToken(localMbox.getAccount());
        }

        // Get ZMailbox
        Account account = Provisioning.getInstance().get(AccountBy.id, mountpoint.getOwnerId());
        ZMailbox.Options zoptions = new ZMailbox.Options(authToken.toZAuthToken(), AccountUtil.getSoapUri(account));
        zoptions.setNoSession(true);
        zoptions.setTargetAccount(account.getId());
        zoptions.setTargetAccountBy(AccountBy.id);
        ZMailbox zmbx = ZMailbox.getMailbox(zoptions);
        if (zmbx != null) {
            zmbx.setName(account.getName()); /* need this when logging in using another user's auth */
        }
        return zmbx;
    }

    public static final String HEADER_FORWARDED = "X-Zimbra-Forwarded";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    public static final String HEADER_RETURN_PATH = "Return-Path";
    public static final String HEADER_AUTO_SUBMITTED = "Auto-Submitted";

    public static void redirect(OperationContext octxt, Mailbox sourceMbox, MimeMessage msg, String destinationAddress)
    throws ServiceException {
        MimeMessage outgoingMsg;

        try {
            if (!isMailLoop(sourceMbox, msg, new String[]{HEADER_FORWARDED})) {
                outgoingMsg = new Mime.FixedMimeMessage(msg);
                Mime.recursiveRepairTransferEncoding(outgoingMsg);
                outgoingMsg.addHeader(HEADER_FORWARDED, sourceMbox.getAccount().getName());
                outgoingMsg.saveChanges();
            } else {
                String error = String.format("Detected a mail loop for message %s.", Mime.getMessageID(msg));
                throw ServiceException.FAILURE(error, null);
            }
        } catch (MessagingException e) {
            try {
                outgoingMsg = createRedirectMsgOnError(msg);
                ZimbraLog.filter.info("Message format error detected.  Wrapper class in use.  %s", e.toString());
            } catch (MessagingException again) {
                throw ServiceException.FAILURE("Message format error detected.  Workaround failed.", again);
            }
        } catch (IOException e) {
            try {
                outgoingMsg = createRedirectMsgOnError(msg);
                ZimbraLog.filter.info("Message format error detected.  Wrapper class in use.  %s", e.toString());
            } catch (MessagingException me) {
                throw ServiceException.FAILURE("Message format error detected.  Workaround failed.", me);
            }
        }

        MailSender sender = sourceMbox.getMailSender().setSaveToSent(false).setRedirectMode(true).setSkipHeaderUpdate(true);

        try {
            if (Provisioning.getInstance().getLocalServer().isMailRedirectSetEnvelopeSender()) {
                if (isDeliveryStatusNotification(msg) && LC.filter_null_env_sender_for_dsn_redirect.booleanValue()) {
                    sender.setEnvelopeFrom("<>");
                    sender.setDsnNotifyOptions(MailSender.DsnNotifyOption.NEVER);
                } else {
                    // Set envelope sender to the account name (bug 31309).
                    Account account = sourceMbox.getAccount();
                    sender.setEnvelopeFrom(account.getName());
                }
            } else {
                Address from = ArrayUtil.getFirstElement(outgoingMsg.getFrom());
                if (from != null) {
                    String address = ((InternetAddress) from).getAddress();
                    sender.setEnvelopeFrom(address);
                }
            }
            sender.setRecipients(destinationAddress);
            sender.sendMimeMessage(octxt, sourceMbox, outgoingMsg);
        } catch (MessagingException e) {
            ZimbraLog.filter.warn("Envelope sender will be set to the default value.", e);
        }
    }

    private static boolean isDeliveryStatusNotification(MimeMessage msg)
    throws MessagingException {
        String envelopeSender = msg.getHeader("Return-Path", null);
        String ct = Mime.getContentType(msg, "text/plain");
        ZimbraLog.filter.debug("isDeliveryStatusNotification(): Return-Path=%s, Auto-Submitted=%s, Content-Type=%s.",
            envelopeSender, msg.getHeader("Auto-Submitted", null), ct);

        if (StringUtil.isNullOrEmpty(envelopeSender) || envelopeSender.equals("<>")) {
            return true;
        }
        if (Mime.isAutoSubmitted(msg)) {
            return true;
        }
        if (ct.equals("multipart/report")) {
            return true;
        }
        return false;
    }

    public static void reply(OperationContext octxt, Mailbox mailbox, ParsedMessage parsedMessage, String bodyTemplate)
    throws MessagingException, ServiceException {
        MimeMessage mimeMessage = parsedMessage.getMimeMessage();
        if (isMailLoop(mailbox, mimeMessage, new String[]{HEADER_FORWARDED})) {
            String error = String.format("Detected a mail loop for message %s.", Mime.getMessageID(mimeMessage));
            throw ServiceException.FAILURE(error, null);
        }
        if (isDeliveryStatusNotification(mimeMessage)) {
            ZimbraLog.filter.debug("Not auto-replying to a DSN message");
            return;
        }

        Account account = mailbox.getAccount();
        MimeMessage replyMsg = new Mime.FixedMimeMessage(JMSession.getSmtpSession(account));
        // add the forwarded header account names to detect the mail loop between accounts
        for (String headerFwdAccountName : Mime.getHeaders(mimeMessage, HEADER_FORWARDED)) {
            replyMsg.addHeader(HEADER_FORWARDED, headerFwdAccountName);
        }
        replyMsg.addHeader(HEADER_FORWARDED, account.getName());

        String to = mimeMessage.getHeader("Reply-To", null);
        if (StringUtil.isNullOrEmpty(to))
            to = Mime.getSender(mimeMessage);
        if (StringUtil.isNullOrEmpty(to))
            throw new MessagingException("Can't locate the address to reply to");
        replyMsg.setRecipient(javax.mail.Message.RecipientType.TO, new JavaMailInternetAddress(to));

        String subject = mimeMessage.getSubject();
        if (subject == null) {
            subject = "";
        }
        String replySubjectPrefix = L10nUtil.getMessage(L10nUtil.MsgKey.replySubjectPrefix, account.getLocale());
        if (!subject.toLowerCase().startsWith(replySubjectPrefix.toLowerCase())) {
            subject = replySubjectPrefix + " " + subject;
        }
        replyMsg.setSubject(subject, getCharset(account, subject));

        //getVarsMap() result is now being passed to replaceVariables(), so following call is redundant
        //Map<String, String> vars = getVarsMap(mailbox, parsedMessage, mimeMessage);
        replyMsg.setText(bodyTemplate, getCharset(account, bodyTemplate));

        String origMsgId = mimeMessage.getMessageID();
        if (!StringUtil.isNullOrEmpty(origMsgId))
            replyMsg.setHeader("In-Reply-To", origMsgId);
        replyMsg.setSentDate(new Date());
        replyMsg.saveChanges();

        MailSender mailSender = mailbox.getMailSender();
        mailSender.setReplyType(MailSender.MSGTYPE_REPLY);
        mailSender.setDsnNotifyOptions(MailSender.DsnNotifyOption.NEVER);
        mailSender.sendMimeMessage(octxt, mailbox, replyMsg);
    }

    public static void notify(OperationContext octxt, Mailbox mailbox, ParsedMessage parsedMessage,
            String emailAddr, String subjectTemplate, String bodyTemplate, int maxBodyBytes, List<String> origHeaders)
    throws MessagingException, ServiceException {
        MimeMessage mimeMessage = parsedMessage.getMimeMessage();
        if (isMailLoop(mailbox, mimeMessage, new String[]{HEADER_FORWARDED})) {
            String error = String.format("Detected a mail loop for message %s.", Mime.getMessageID(mimeMessage));
            throw ServiceException.FAILURE(error, null);
        }

        Account account = mailbox.getAccount();
        MimeMessage notification = new Mime.FixedMimeMessage(JMSession.getSmtpSession(account));
        // add the forwarded header account names to detect the mail loop between accounts
        for (String headerFwdAccountName : Mime.getHeaders(mimeMessage, HEADER_FORWARDED)) {
            notification.addHeader(HEADER_FORWARDED, headerFwdAccountName);
        }

        notification.addHeader(HEADER_FORWARDED, account.getName());
        MailSender mailSender = mailbox.getMailSender().setSaveToSent(false);

        //getVarsMap() result is now being passed to replaceVariables(), so following call is redundant
        //Map<String, String> vars = getVarsMap(mailbox, parsedMessage, mimeMessage);
        if (origHeaders == null || origHeaders.isEmpty()) {
            // no headers need to be copied from the original message
            notification.setRecipient(javax.mail.Message.RecipientType.TO, new JavaMailInternetAddress(emailAddr));
            notification.setSentDate(new Date());
            if (!StringUtil.isNullOrEmpty(subjectTemplate)) {
                notification.setSubject(subjectTemplate, getCharset(account, subjectTemplate));
            }
        } else {
            if (origHeaders.size() == 1 && "*".equals(origHeaders.get(0))) {
                // all headers need to be copied from the original message
                Enumeration enumeration = mimeMessage.getAllHeaders();
                while (enumeration.hasMoreElements()) {
                    Header header = (Header) enumeration.nextElement();
                    if (StringUtil.equal(header.getName(), HEADER_FORWARDED)) {
                        continue;
                    }

                    if (StringUtil.equal(header.getName(), HEADER_CONTENT_TYPE)
                        || StringUtil.equal(header.getName(), HEADER_CONTENT_DISPOSITION))  {
                        // Zimbra Mime parser will add the correct Content Type if absent
                        continue;
                    }
                    notification.addHeader(header.getName(), header.getValue());
                }
            } else {
                // some headers need to be copied from the original message
                Set<String> headersToCopy = Sets.newHashSet(origHeaders);
                boolean copySubject = false;
                for (String header : headersToCopy) {
                    if ("Subject".equalsIgnoreCase(header)) {
                        copySubject = true;
                    }
                    if (StringUtil.equal(header, HEADER_FORWARDED)) {
                        continue;
                    }
                    String[] hdrVals = mimeMessage.getHeader(header);
                    if (hdrVals == null) {
                        continue;
                    }
                    for (String hdrVal : hdrVals) {
                        notification.addHeader(header, hdrVal);
                    }
                }
                if (!copySubject && !StringUtil.isNullOrEmpty(subjectTemplate)) {
                    notification.setSubject(subjectTemplate, getCharset(account, subjectTemplate));
                }
            }
            mailSender.setRedirectMode(true);
            mailSender.setRecipients(emailAddr);
        }

        String body = StringUtil.truncateIfRequired(bodyTemplate, maxBodyBytes);
        notification.setText(body, getCharset(account, body));
        notification.saveChanges();

        if (isDeliveryStatusNotification(mimeMessage)) {
            mailSender.setEnvelopeFrom("<>");
        } else {
            mailSender.setEnvelopeFrom(account.getName());
        }
        mailSender.setDsnNotifyOptions(MailSender.DsnNotifyOption.NEVER);
        mailSender.sendMimeMessage(octxt, mailbox, notification);
    }

    public static void reject(OperationContext octxt, Mailbox mailbox, ParsedMessage parsedMessage,
                              String reason, LmtpEnvelope envelope)
      throws MessagingException, ServiceException {
        MimeMessage mimeMessage = parsedMessage.getMimeMessage();
        if (isMailLoop(mailbox, mimeMessage, new String[]{HEADER_FORWARDED})) {
            // Detected a mail loop.  Do not send MDN, but just discard the message
            String error = String.format("Detected a mail loop for message %s. No MDN sent.",
                Mime.getMessageID(mimeMessage));
            ZimbraLog.filter.info(error);
            throw ServiceException.FAILURE(error, null);
        }
        String reportTo = null;
        if (envelope != null && envelope.hasSender()) {
            reportTo = envelope.getSender().getEmailAddress();
        }
        if (reportTo == null || reportTo.isEmpty()) {
            String [] returnPath = mimeMessage.getHeader(HEADER_RETURN_PATH);
            if (returnPath == null || returnPath.length == 0) {
                // RFC 5429 2.2.1.
                // >> Note that according to [MDN], Message Disposition Notifications MUST
                // >> NOT be generated if the MAIL FROM (or Return-Path) is empty.
                throw new MessagingException("Neither 'envelope from' nor 'Return-Path' specified. Can't locate the address to reject to (No MDN sent)");
            } else {
                // At least one 'return-path' should exist.
                reportTo = returnPath[0];
            }
        }

        Account owner = mailbox.getAccount();
        Locale locale = owner.getLocale();
        String charset = owner.getPrefMailDefaultCharset();
        if (charset == null) {
            charset = MimeConstants.P_CHARSET_UTF8;
        }

        SMTPMessage report = AccountUtil.getSmtpMessageObj(owner);
        // add the forwarded header account names to detect the mail loop between accounts
        for (String headerFwdAccountName : Mime.getHeaders(mimeMessage, HEADER_FORWARDED)) {
            report.addHeader(HEADER_FORWARDED, headerFwdAccountName);
        }
        report.addHeader(HEADER_FORWARDED, owner.getName());

        // MDN header
        report.setEnvelopeFrom("<>");
        report.setRecipient(javax.mail.Message.RecipientType.TO, new JavaMailInternetAddress(reportTo));
        String subject = L10nUtil.getMessage(MsgKey.seiveRejectMDNSubject, locale);
        report.setSubject(subject);
        report.setSentDate(new Date());
        InternetAddress address = new JavaMailInternetAddress(owner.getName());
        report.setFrom(address);

        MimeMultipart multi = new ZMimeMultipart("report");
        // part 1: human-readable notification
        String text = L10nUtil.getMessage(MsgKey.seiveRejectMDNErrorMsg, locale) + "\n" + reason;
        MimeBodyPart mpText = new ZMimeBodyPart();
        mpText.setText(text, CharsetUtil.checkCharset(text, charset));
        multi.addBodyPart(mpText);

        // part 2: disposition notification
        StringBuilder mdn = new StringBuilder();
        mdn.append("Final-Recipient: rfc822;").append(owner.getName()).append("\r\n");
        mdn.append("Disposition: automatic-action/MDN-sent-automatically");
        mdn.append("; deleted\r\n");

        MimeBodyPart mpMDN = new ZMimeBodyPart();
        mpMDN.setText(mdn.toString(), MimeConstants.P_CHARSET_UTF8);
        mpMDN.setHeader("Content-Type", "message/disposition-notification; charset=utf-8");
        multi.addBodyPart(mpMDN);

        // Assemble the MDN
        report.setContent(multi);
        report.setHeader("Content-Type", multi.getContentType() + "; report-type=disposition-notification");
        report.saveChanges();

        MailSender mailSender = mailbox.getMailSender().setSaveToSent(false);
        mailSender.setRecipients(reportTo);
        mailSender.setEnvelopeFrom("<>");
        mailSender.sendMimeMessage(octxt, mailbox, report);
    }


    public static void notifyMailto(LmtpEnvelope envelope, OperationContext octxt, Mailbox mailbox,
            ParsedMessage parsedMessage, String from, int importance, Map<String, String> options,
            String message, String mailto, Map<String, List<String>> mailtoParams)
                    throws MessagingException, ServiceException {
        // X-Zimbra-Forwarded
        MimeMessage mimeMessage = parsedMessage.getMimeMessage();
        if (isMailLoop(mailbox, mimeMessage, new String[]{HEADER_FORWARDED, HEADER_AUTO_SUBMITTED})) {
            String error = String.format("Detected a mail loop for message %s while notifying", Mime.getMessageID(mimeMessage));
            throw ServiceException.FAILURE(error, null);
        }

        Account account = mailbox.getAccount();
        MimeMessage notification = new Mime.FixedMimeMessage(JMSession.getSmtpSession(account));
        MailSender mailSender = mailbox.getMailSender().setSaveToSent(false);
        mailSender.setRedirectMode(true);

        // add the forwarded header account names to detect the mail loop between accounts
        for (String headerFwdAccountName : Mime.getHeaders(mimeMessage, HEADER_FORWARDED)) {
            notification.addHeader(HEADER_FORWARDED, headerFwdAccountName);
        }
        notification.addHeader(HEADER_FORWARDED, account.getName());

        // Envelope FROM
        // RFC 5436 2.7. (1st item of the 'guidelines')
        String originalEnvelopeFrom = envelope == null ? null : envelope.getSender().getEmailAddress();
        if (originalEnvelopeFrom == null) {
            // Whenever the envelope FROM of the original message is <>, set <> to the notification message too
            mailSender.setEnvelopeFrom("<>");
        } else if (!StringUtil.isNullOrEmpty(from)) {
            List<com.zimbra.common.mime.InternetAddress> addr = com.zimbra.common.mime.InternetAddress
                .parseHeader(from);
            String escapedFrom = StringEscapeUtils.escapeJava(addr.get(0).getAddress());
            boolean matches;
            do {
                // if address contains single backslash, don't escape it
                String patternString = ".*([\\p{ASCII}&&[^\\\\]])([\\\\][\\\\])([^\\\\])(.*)@.*";
                Pattern pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(escapedFrom);
                matches = matcher.matches();
                if (matches)
                    escapedFrom = new StringBuilder(escapedFrom)
                        .replace(matcher.start(2), matcher.end(2), "\\").toString();
            } while (matches);
            mailSender.setEnvelopeFrom(escapedFrom);
        } else {
            // System default value
            mailSender.setEnvelopeFrom("<>");
        }

        // Envelope TO & Header To/Cc
        // RFC 5436 2.7. (2nd and 5th item of the 'guidelines')
        Set<String> envelopeTos = new HashSet<String>();
        envelopeTos.add(mailto);
        notification.addRecipient(javax.mail.Message.RecipientType.TO, new JavaMailInternetAddress(mailto));

        List<String> tos = mailtoParams.get("to");
        if (tos != null && tos.size() > 0) {
            for (String to : tos) {
                envelopeTos.add(to);
                notification.addRecipient(javax.mail.Message.RecipientType.TO, new JavaMailInternetAddress(to));
            }
        }
        List<String> ccs = mailtoParams.get("cc");
        if (ccs != null && ccs.size() > 0) {
            for (String cc : ccs) {
                envelopeTos.add(cc);
                notification.addRecipient(javax.mail.Message.RecipientType.CC, new JavaMailInternetAddress(cc));
            }
        }
        List<String> bccs = mailtoParams.get("bcc");
        if (bccs != null && bccs.size() > 0) {
            for (String bcc : bccs) {
                envelopeTos.add(bcc);
                // No Bcc for the message header
            }
        }
        mailSender.setRecipients(envelopeTos.toArray(new String[envelopeTos.size()]));

        // Auto-Submitted
        // RFC 5436 2.7. (3rd item of the 'guidelines') and 2.7.1.
        StringBuilder autoSubmitted = new StringBuilder("auto-notified; owner-email=\"").append(account.getName()).append("\"");
        notification.addHeader(HEADER_AUTO_SUBMITTED, autoSubmitted.toString());

        // Header From
        // RFC 5436 2.7. (4th item of the 'guidelines')
        if (!StringUtil.isNullOrEmpty(from)) {
            // The "From:" header field of the notification message SHOULD be set
            // to the value of the ":from" tag to the notify action, if one is
            // specified, has email address syntax, and is valid according to the
            // implementation-specific security checks.
            notification.addHeader("from", from);
        } else {
            // If ":from" is not specified or is not valid, the
            // "From:" header field of the notification message SHOULD be set
            // either to the envelope "to" field from the triggering message, as
            // used by Sieve...
            // This MUST NOT be overridden by a "from" URI header, and any such
            // URI header MUST be ignored.
            notification.addHeader("from", mailbox.getAccount().getMail());
        }

        // Subject
        // RFC 5436 2.7. (6th item of the 'guidelines')
        if (StringUtil.isNullOrEmpty(message)) {
            List<String> subjectList = mailtoParams.get("subject");
            if (subjectList != null && subjectList.size() > 0) {
                message = subjectList.get(0);
            } else {
                String[] subjects = Mime.getHeaders(mimeMessage, "Subject");
                if (subjects.length > 0) {
                    message = subjects[0];
                }
            }
        }
        notification.setSubject(message, getCharset(account, message));

        // Body
        // RFC 5436 2.7. (8th item of the 'guidelines')
        List<String> bodys = mailtoParams.get("body");
        if (bodys != null && bodys.size() > 0) {
            String body = bodys.get(0);
            notification.setText(body, getCharset(account, body));
        } else {
            notification.setText("");
        }

        notification.saveChanges();

        // Misc.
        notification.setSentDate(new Date());
        for (String headerName : mailtoParams.keySet()) {
            if (!("to".equalsIgnoreCase(headerName) ||
                  "cc".equalsIgnoreCase(headerName) ||
                  "bcc".equalsIgnoreCase(headerName) ||
                  "from".equalsIgnoreCase(headerName) ||
                  "subject".equalsIgnoreCase(headerName) ||
                  "auto-submitted".equalsIgnoreCase(headerName) ||
                  "x-zimbra-forwarded".equalsIgnoreCase(headerName) ||
                  "message-id".equalsIgnoreCase(headerName) ||
                  "date".equalsIgnoreCase(headerName) ||
                  "body".equalsIgnoreCase(headerName))) {
                List<String> values = mailtoParams.get(headerName);
                for (String value : values) {
                    notification.addHeaderLine(headerName + ": " + value);
                }
            }
        }

        mailSender.setDsnNotifyOptions(MailSender.DsnNotifyOption.NEVER);
        mailSender.sendMimeMessage(octxt, mailbox, notification);
    }

    /**
     * Gets appropriate charset for the given data. The charset preference order is:
     *                `
     * 1. "us-ascii"
     * 2. Account's zimbraPrefMailDefaultCharset attr
     * 3. "utf-8"
     *
     * @param account
     * @param data
     * @return
     */
    private static String getCharset(Account account, String data) {
        if (MimeConstants.P_CHARSET_ASCII.equals(CharsetUtil.checkCharset(data, MimeConstants.P_CHARSET_ASCII))) {
            return MimeConstants.P_CHARSET_ASCII;
        } else {
            return CharsetUtil.checkCharset(data, account.getAttr(Provisioning.A_zimbraPrefMailDefaultCharset, MimeConstants.P_CHARSET_UTF8));
        }
    }

    @VisibleForTesting
    static Map<String, String> getVarsMap(Mailbox mailbox, ParsedMessage parsedMessage, MimeMessage mimeMessage)
            throws MessagingException, ServiceException {
        Map<String, String> vars = new HashMap<String, String>() {
            @Override
            public String get(Object key) {
                return super.get(((String) key).toLowerCase());
            }
        };
        Enumeration enumeration = mimeMessage.getAllHeaders();
        while (enumeration.hasMoreElements()) {
            Header header = (Header) enumeration.nextElement();
            vars.put(header.getName().toLowerCase(),
                    ZInternetHeader.decode(mimeMessage.getHeader(header.getName(), ",")));
        }
        // raw subject could be encoded, so get the parsed subject
        vars.put("subject", parsedMessage.getSubject());
        MPartInfo bodyPart = Mime.getTextBody(parsedMessage.getMessageParts(), false);
        if (bodyPart != null) {
            try {
                vars.put("body",
                         Mime.getStringContent(bodyPart.getMimePart(), mailbox.getAccount().getPrefMailDefaultCharset()));
            } catch (IOException e) {
                ZimbraLog.filter.warn("Error in reading text body", e);
            }
        }
        return vars;
    }

    private static MimeMessage createRedirectMsgOnError(final MimeMessage originalMsg) throws MessagingException {
        // If MimeMessage.saveChanges fails, create a copy of the message
        // that doesn't recursively call updateHeaders() upon saveChanges().
        // This uses double the memory on malformed messages, but it should
        // avoid having a deep-buried misparse throw off the whole forward.
        // TODO: With JavaMail 1.4.3, this workaround might not be needed any more.
        return new Mime.FixedMimeMessage(originalMsg) {
            @Override
            protected void updateHeaders() throws MessagingException {
                setHeader("MIME-Version", "1.0");
                updateMessageID();
            }
        };
    }

    /**
     * Returns <tt>true</tt> if the triggering message is an automatically
     * generated message.
     * @param sourceMbox owner's mailbox
     * @param msg triggering message
     * @param checkHeaders a list of header field names to be checked on the triggering message.
     *  For the reply and notify (legacy Zimbra version) action, checks X-Zimbra-Forwarded
     *  For the notify (RFC compliant) action, checks X-Zimbra-Forwarded and Auto-Submitted headers.
     */
    private static boolean isMailLoop(Mailbox sourceMbox, MimeMessage msg, String[] checkHeaders)
    throws ServiceException {
        String userName = sourceMbox.getAccount().getName();
        for (String header : checkHeaders) {
            if (HEADER_FORWARDED.equals(header)) {
                String[] forwards = Mime.getHeaders(msg, HEADER_FORWARDED);
                for (String forward : forwards) {
                    if (StringUtil.equal(userName, forward)) {
                        return true;
                    }
                }
            } else if (HEADER_AUTO_SUBMITTED.equals(header)) {
                String[] values = Mime.getHeaders(msg, HEADER_AUTO_SUBMITTED);
                for (String value : values) {
                    String[] tokens = value.split(";");
                    if (tokens.length > 1) {
                        if ("no".equalsIgnoreCase(tokens[0].trim())) {
                            return false;
                        } else {
                            // Sample header value
                            //   Auto-Submitted: auto-notified; owner-email="recipient@example.org"
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static int getFlagBitmask(Collection<ActionFlag> actions, int startingBitMask) {
        int bitmask = startingBitMask;
        for (ActionFlag action : actions) {
            if (action.isSet()) {
                bitmask |= action.getFlag().toBitmask();
            } else {
                bitmask &= ~action.getFlag().toBitmask();
            }
        }
        return bitmask;
    }

    public static String[] getTagsUnion(String[] tags1, String[] tags2) {
        if (tags2 == null) {
            return tags1;
        } else if (tags1 == null) {
            return tags2;
        }

        Set<String> tags = Sets.newHashSet(tags1);
        tags.addAll(Arrays.asList(tags2));
        return tags.toArray(new String[tags.size()]);
    }

    /**
     * Look-up the variable table to get the set value.
     * If the 'sourceStr' contains a variable-ref (formatted with ${variable-name}), this method looks up the
     * variable table with its variable-name and replaces the variable with the text value assigned by
     * the 'set' command.
     * According to the RFC 5229 Section 4., "Variable names are case insensitive."
     *
     * @param variables a list of variable name/value
     * @param matchedVariables a list of Matched Variables
     * @param sourceStr text string that may contain "variable-ref" (RFC 5229 Section 3.)
     * @return Replaced text string
     * @throws SyntaxException
     */
    public static String replaceVariables(ZimbraMailAdapter mailAdapter, String sourceStr) throws SyntaxException {
        if (null == mailAdapter) {
            return sourceStr;
        }
        if (sourceStr.indexOf("${") == -1) {
            return sourceStr;
        }
        validateVariableIndex(sourceStr);

        try {
            Require.checkCapability(mailAdapter, CAPABILITY_VARIABLES);
        } catch (SyntaxException e) {
            ZimbraLog.filter.info("\"variables\" capability is not declared. No variables will be replaced");
            return sourceStr;
        }

        Map<String, String> variables = mailAdapter.getVariables();
        List<String> matchedVariables = mailAdapter.getMatchedValues();
        ZimbraLog.filter.debug("Variable: %s " , sourceStr);
        ZimbraLog.filter.debug("Variable available: %s : %s", variables ,  matchedVariables);
        String resultStr = sourceStr;

        // (1) Resolve the Matched Variables (numeric variables; "${N}" (N=0,1,...9)
        int i = 0;
        for (; i < matchedVariables.size() && i < 10; i++) {
            String keyName = "(?i)" + "\\$\\{0*" + String.valueOf(i) + "\\}";
            resultStr = resultStr.replaceAll(keyName, Matcher.quoteReplacement(matchedVariables.get(i)));
        }
        // (2) Replace the empty string to Matched Variables whose index is out of range
        for (; i < 10; i++) {
            String keyName = "(?i)" + "\\$\\{0*" + String.valueOf(i) + "\\}";
            resultStr = resultStr.replaceAll(keyName, Matcher.quoteReplacement(""));
        }

        // (3) Resolve the named variables ("${xxx}")
        resultStr = leastGreedyReplace(variables, resultStr, mailAdapter.getMimeVariables());
        ZimbraLog.filter.debug("Sieve: variable value is: %s", resultStr);
        return resultStr;
    }

    public static String leastGreedyReplace(Map<String, String> variables, String sourceStr) {
        return leastGreedyReplace(variables, sourceStr, null);
    }

    /**
     * Replaces all ${variable name} variables within the 'sourceStr' into the defined text value.
     *
     * The variable name matches as short as possible (non-greedy matching).  Unknown variables are replaced
     * by the empty string (RFC 5229 Section 3.)
     *
     * @param variables map table of the variable name and value
     * @param sourceStr text string that may contain one or more than one "variable-ref"
     * @return Replaced text string
     */
    public static String leastGreedyReplace(Map<String, String> variables, String sourceStr, Map<String, String> mimeVars) {
        if (variables == null || sourceStr == null || sourceStr.length() == 0) {
            return sourceStr;
        }
        StringBuilder resultStr = new StringBuilder();
        int start1 = 0;
        int end = -1;
        while (start1 < sourceStr.length()) {
            int start2 = sourceStr.indexOf("${", start1);
            if (start2 >= 0) {
                resultStr.append(sourceStr.substring(start1, start2));
                end = sourceStr.indexOf("}", start2 + 2);
                if (end > 0) {
                    int start3 = sourceStr.indexOf("${", start2 + 2);
                    if (start3 > start2 && start3 < end) {
                        start1 = start3;
                        resultStr.append(sourceStr.substring(start2, start3));
                    } else {
                        // a variable name found
                        String key = sourceStr.substring(start2 + 2, end).toLowerCase();
                        key = FilterUtil.handleQuotedAndEncodedVar(key);
                        if (SetVariable.isValidIdentifier(key)) {
                            // the variable name is valid
                            String value = variables.get(key);
                            if (value != null) {
                                resultStr.append(value);
                            } else {
                                if (mimeVars != null) {
                                    if (mimeVars.containsKey(key)) {
                                        value = mimeVars.get(key);
                                        resultStr.append(value);
                                    }
                                }
                            }
                        } else {
                            // the variable name contains some invalid characters
                            resultStr.append(sourceStr.substring(start2, end + 1));
                        }
                        start1 = end + 1;
                    }
                } else {
                    // no corresponding }
                    resultStr.append(sourceStr.substring(start2, sourceStr.length()));
                    break;
                }
            } else {
                // no more ${
                resultStr.append(sourceStr.substring(end + 1, sourceStr.length()));
                break;
            }
        }
        return resultStr.toString();
    }

    /**
     * Remove the extra backslash. Any undefined escape sequences
     * specified as a string text in the sieve filter are supposed to be
     * removed before parsing the sieve filter. So, this method is mainly
     * called to verify the variable name whose label is generated at runtime
     * via another variable.
     *
     * @param varName
     * @return
     * "${fo\o}"  => ${foo}  => the expansion of variable foo. <br>
     * "${fo\\o}" => ${fo\o} => illegal identifier => left verbatim.<br>
     * "${foo\}"  => ${foo}  => the expansion of variable foo.
	 */
	public static String handleQuotedAndEncodedVar(String varName) {
		String processedStr  = varName;
		StringBuilder sb = new StringBuilder();
		char [] charArray = varName.toCharArray();
		for (int i = 0; i < charArray.length; ++i) {
			if (charArray[i] == '\\') {
                if (i == charArray.length -1) {
                    // remove the last backslash of the variable name because it doesn't escape anything.
                } else if (charArray[i + 1] == '\\') {
                    sb.append(charArray[++i]);
				}
			} else {
				sb.append(charArray[i]);
			}
		}
		processedStr = sb.toString();

		return processedStr;
	}

    /**
     * Converts a Sieve pattern in a java regex pattern
     */
    public static String sieveToJavaRegex(String pattern) {
        int ch;
        int starWildCardCount = StringUtils.countMatches(pattern, "*");

        StringBuffer buffer = new StringBuffer(2 * pattern.length());
        for (ch = 0; ch < pattern.length(); ch++) {
            final char nextChar = pattern.charAt(ch);
            switch (nextChar) {
            case '*':
                //If there are two or more wildcards,all wildcards should be non-greedy except the last wildcard.
                //If there is only one wildcard in the sieve pattern, it is set as a greedy wildcard.
                if (starWildCardCount > 1) {
                    buffer.append("(.*?)");
                } else {
                    buffer.append("(.*)");
                }
                starWildCardCount--;
                break;
            case '?':
                buffer.append("(.)");
                break;
            case '\\':
                buffer.append('\\');
                if (ch == pattern.length() - 1)
                    buffer.append('\\');
                else if (isSieveMatcherSpecialChar(pattern.charAt(ch + 1)))
                    buffer.append(pattern.charAt(++ch));
                else
                    buffer.append('\\');
                break;
            default:
                if (isRegexSpecialChar(nextChar))
                    buffer.append('\\');
                buffer.append(nextChar);
                break;
            }
        }
        return buffer.toString();
    }

    /**
     * Returns true if the char is a special char for regex
     */
    private static boolean isRegexSpecialChar(char ch) {
        return (ch == '*' || ch == '?' || ch == '+' || ch == '[' || ch == ']'
                || ch == '(' || ch == ')' || ch == '|' || ch == '^'
                || ch == '$' || ch == '.' || ch == '{' || ch == '}' || ch == '\\');
    }

    /**
     * Returns true if the char is a special char for sieve matching
     */
    private static boolean isSieveMatcherSpecialChar(char ch) {
        return (ch == '*' || ch == '?');
    }

    private static void validateVariableIndex(String srcStr) throws SyntaxException {
       boolean match = false;
       String negativeIndexPattern = "\\$\\{-\\d*\\}";
       String exceedsIndexPattern = "\\$\\{0*[1-9]{1,}\\d{1,}\\}";
       Pattern pattern = Pattern.compile(negativeIndexPattern);
       Matcher matcher = pattern.matcher(srcStr);
       match = matcher.find();
       if (!match) {
          pattern = Pattern.compile(exceedsIndexPattern);
          matcher = pattern.matcher(srcStr);
          match = matcher.find();
       }
       if (match) {
           ZimbraLog.filter.debug("Invalid variable index %s ", srcStr);
           throw new SyntaxException("Invalid variable index " + srcStr);
       }
    }

    public static void headerNameHasSpace(String headerName) throws SyntaxException {
        if (StringUtil.isNullOrEmpty(headerName)) {
            throw new SyntaxException("ZimbraComparatorUtils : Header name must not be null or empty");
        }
        if (headerName.contains(" ")) {
            throw new SyntaxException("ZimbraComparatorUtils : Header name must not have space(s) : \"" + headerName + "\"");
        }
    }
}

