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

package com.zimbra.cs.service.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.sun.mail.smtp.SMTPMessage;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.zmime.ZMimeBodyPart;
import com.zimbra.common.zmime.ZMimeMultipart;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.MailboxBlobDataSource;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.util.JMSession;

public class SpamHandler {

    private static Log log = LogFactory.getLog(SpamHandler.class);

    private static SpamHandler spamHandler;

    public static synchronized SpamHandler getInstance() {
        if (spamHandler == null) {
            spamHandler = new SpamHandler();
        }
        return spamHandler;
    }

    public SpamHandler() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                reportLoop();
            }
        };
        Thread spamHandlerThread = new Thread(r);
        spamHandlerThread.setName("Junk-NotJunk-Handler");
        spamHandlerThread.setDaemon(true);
        spamHandlerThread.start();
    }

    private void sendReport(SpamReport sr) throws ServiceException, MessagingException {
        Config config = Provisioning.getInstance().getConfig();
        String isSpamString = sr.isSpam ? config.getSpamReportTypeSpam() : config.getSpamReportTypeHam();

        Mailbox mbox = MailboxManager.getInstance().getMailboxById(sr.mailboxId);
        Domain domain = Provisioning.getInstance().getDomain(mbox.getAccount());
        SMTPMessage out = new SMTPMessage(JMSession.getSmtpSession(domain));

        Message msg = mbox.getMessageById(null, sr.messageId);

        MimeMultipart mmp = new ZMimeMultipart("mixed");

        MimeBodyPart infoPart = new ZMimeBodyPart();
        infoPart.setHeader("Content-Description", "Zimbra spam classification report");
        String body = String.format(
            "Classified-By: %s\r\n" +
            "Classified-As: %s\r\n" +
            "Action: %s\r\n" +
            "Source-Folder: %s\r\n" +
            "Destination-Folder: %s\r\n" +
            "Destination-Mailbox: %s\r\n",
            Strings.nullToEmpty(sr.accountName), isSpamString, Strings.nullToEmpty(sr.action),
            Strings.nullToEmpty(sr.sourceFolder), Strings.nullToEmpty(sr.destFolder),
            Strings.nullToEmpty(sr.destAccountName));
        infoPart.setText(body);
        mmp.addBodyPart(infoPart);

        MailboxBlob blob = msg.getBlob();
        MimeBodyPart mbp = new ZMimeBodyPart();
        mbp.setDataHandler(new DataHandler(new MailboxBlobDataSource(blob)));
        mbp.setHeader("Content-Type", MimeConstants.CT_MESSAGE_RFC822);
        mbp.setHeader("Content-Disposition", Part.ATTACHMENT);
        mmp.addBodyPart(mbp);

        out.setContent(mmp);

        out.addHeader(config.getSpamReportSenderHeader(), sr.accountName);
        out.addHeader(config.getSpamReportTypeHeader(), isSpamString);

        if (config.isSmtpSendAddOriginatingIP() && sr.origIp != null)
            out.addHeader(MailSender.X_ORIGINATING_IP, MailSender.formatXOrigIpHeader(sr.origIp));

        out.setRecipient(javax.mail.Message.RecipientType.TO, sr.reportRecipient);
        out.setEnvelopeFrom(config.getSpamReportEnvelopeFrom());
        out.setSubject(config.getSpamTrainingSubjectPrefix() + " " + sr.accountName + ": " + isSpamString);
        Transport.send(out);

        ZimbraLog.misc.info("Sent " + sr);
    }

    public static final class SpamReport {
        // These fields are set in the constructor.
        private final boolean isSpam;
        private final String action;
        private final String destFolder;

        // These fields are optionally set by the caller.
        private String sourceFolder;
        private String destAccountName;

        // These fields are set internally.
        private String accountName;
        private InternetAddress reportRecipient;
        private String origIp;
        private int messageId;
        private int mailboxId;

        public SpamReport(boolean isSpam, String action, String destFolder) {
            this.isSpam = isSpam;
            this.action = action;
            this.destFolder = destFolder;
        }

        SpamReport(SpamReport report) {
            this.isSpam = report.isSpam;
            this.action = report.action;
            this.destFolder = report.destFolder;
            this.sourceFolder = report.sourceFolder;
            this.destAccountName = report.destAccountName;
            this.accountName = report.accountName;
            this.reportRecipient = report.reportRecipient;
            this.origIp = report.origIp;
            this.messageId = report.messageId;
            this.mailboxId = report.mailboxId;
        }

        public void setSourceFolderPath(String path) {
            sourceFolder = path;
        }

        public void setDestAccountName(String name) {
            destAccountName = name;
        }

        @Override public String toString() {
            return MoreObjects.toStringHelper(this).
                add("account", accountName).
                add("mbox", mailboxId).
                add("msgId", messageId).
                add("isSpam", isSpam).
                add("origIp", origIp).
                add("action", action).
                add("srcFolder", sourceFolder).
                add("destFolder", destFolder).
                add("destAccount", destAccountName).
                add("reportRecipient", reportRecipient).toString();
        }
    }

    private static final int spamReportQueueSize = LC.zimbra_spam_report_queue_size.intValue();

    private final Object spamReportQueueLock = new Object();

    List<SpamReport> spamReportQueue = new ArrayList<SpamReport>(spamReportQueueSize);

    void reportLoop() {
        while (true) {
            List<SpamReport> workQueue;
            synchronized (spamReportQueueLock) {
                while (spamReportQueue.size() == 0) {
                    try {
                        spamReportQueueLock.wait();
                    } catch (InterruptedException ie) {
                        ZimbraLog.misc.warn("SpamHandler interrupted", ie);
                    }
                }
                workQueue = spamReportQueue;
                spamReportQueue = new ArrayList<SpamReport>(spamReportQueueSize);
            }

            if (workQueue == null) {
                if (ZimbraLog.misc.isDebugEnabled()) ZimbraLog.misc.debug("SpamHandler nothing to drain");
            } else {
                for (SpamReport sr : workQueue) {
                    try {
                        sendReport(sr);
                    } catch (Exception e) {
                        /* We don't care what errors occurred, we continue to try and send future reports */
                        ZimbraLog.misc.warn("exception occurred sending spam report " + sr, e);
                    }
                }
            }
        }
    }

    private void enqueue(List<SpamReport> reports) {
        synchronized (spamReportQueueLock) {
            for (SpamReport report : reports) {
                if (spamReportQueue.size() > spamReportQueueSize) {
                    ZimbraLog.misc.warn("SpamHandler queue size " + spamReportQueue.size() + " too large, ignored " + report);
                    continue;
                }
                spamReportQueue.add(report);
                ZimbraLog.misc.debug("SpamHandler enqueued %s", report);
            }
            spamReportQueueLock.notify();
        }
    }

    public void handle(OperationContext octxt, Mailbox mbox, int itemId, MailItem.Type type, SpamReport report)
    throws ServiceException {
        Config config = Provisioning.getInstance().getConfig();
        String address;
        if (report.isSpam) {
            address = config.getSpamIsSpamAccount();
            if (Strings.isNullOrEmpty(address)) {
                log.debug("Spam address is not set.  Nothing to do.");
                return;
            }
        } else {
            address = config.getSpamIsNotSpamAccount();
            if (Strings.isNullOrEmpty(address)) {
                log.debug("Ham address is not set.  Nothing to do.");
                return;
            }
        }

        try {
            report.reportRecipient = new JavaMailInternetAddress(address, true);
        } catch (MessagingException e) {
            throw ServiceException.INVALID_REQUEST("Invalid address: " + address, e);
        }

        report.accountName = mbox.getAccount().getName();
        report.mailboxId = mbox.getId();
        if (octxt != null) {
            report.origIp = octxt.getRequestIP();
        }

        List<SpamReport> reports = Lists.newArrayList();
        switch (type) {
        case MESSAGE:
            report.messageId = itemId;
            reports.add(report);
            break;
        case CONVERSATION:
            for (Message msg : mbox.getMessagesByConversation(null, itemId)) {
                SpamReport msgReport = new SpamReport(report);
                msgReport.messageId = msg.getId();
                reports.add(report);
            }
            break;
        default:
            ZimbraLog.misc.warn("SpamHandler called on unhandled item type=" + type +
                    " account=" + report.accountName + " id=" + itemId);
            return;
        }

        enqueue(reports);
    }

    /**
     * Stores the last known value of <tt>zimbraSpamHeaderValue</tt>.  Used
     * for determining whether {@link #spamPattern} needs to be recompiled.
     */
    private static String spamHeaderValue;

    /**
     * Compiled version of {@link #spamHeaderValue}.
     */
    private static Pattern spamPattern;

    /**
     * Stores the last known value of <tt>zimbraSpamWhitelistHeaderValue</tt>.  Used
     * for determining whether {@link #whitelistPattern} needs to be recompiled.
     */
    private static String whitelistHeaderValue;

    /**
     * Compiled version of {@link #whitelistHeaderValue}.
     */
    private static Pattern whitelistPattern;

    /**
     * Returns <tt>false</tt> if the value of the header named <tt>zimbraSpamWhitelistHeader</tt>
     * matches the pattern specified by <tt>zimbraSpamWhitelistHeaderValue</tt>.
     *
     * If <tt>zimbraSpamWhitelistHeader</tt> does not match, returns <tt>true</tt> if the value of the
     * header named <tt>zimbraSpamHeader</tt> matches the pattern specified by <tt>zimbraSpamHeaderValue</tt>.
     */
    public static boolean isSpam(MimeMessage msg) {
        try {
            Config config = Provisioning.getInstance().getConfig();

            String whitelistHeader = config.getSpamWhitelistHeader();
            if (whitelistHeader != null) {
                String whitelistHeaderValue = config.getSpamWhitelistHeaderValue();
                if (whitelistHeaderValue != null) {
                    if (!whitelistHeaderValue.equals(SpamHandler.whitelistHeaderValue)) {
                        // Value has changed.  Recompile pattern.
                        SpamHandler.whitelistHeaderValue = whitelistHeaderValue;
                        whitelistPattern = Pattern.compile(whitelistHeaderValue);
                    }

                    String[] values = Mime.getHeaders(msg, whitelistHeader);
                    boolean matched = false;
                    for (String val : values) {
                        Matcher m = whitelistPattern.matcher(val);
                        if (m.matches()) {
                            matched = true;
                        } else {
                            matched = false;
                            break;
                        }
                    }
                    if (matched) {
                        return false;
                    }
                }
            }

            String spamHeader = config.getSpamHeader();
            if (spamHeader != null) {
                String spamHeaderValue = config.getSpamHeaderValue();
                if (spamHeaderValue != null) {
                    if (!spamHeaderValue.equals(SpamHandler.spamHeaderValue)) {
                        // Value has changed.  Recompile pattern.
                        SpamHandler.spamHeaderValue = spamHeaderValue;
                        spamPattern = Pattern.compile(spamHeaderValue);
                    }

                    String[] values = Mime.getHeaders(msg, spamHeader);
                    for (String val : values) {
                        Matcher m = spamPattern.matcher(val);
                        if (m.matches()) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            ZimbraLog.mailbox.warn("Unable to determine whether the message is spam.", e);
        }
        return false;
    }

}
