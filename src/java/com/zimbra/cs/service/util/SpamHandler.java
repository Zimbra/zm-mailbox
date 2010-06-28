/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.service.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.StringUtil;

import com.sun.mail.smtp.SMTPMessage;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.MailboxBlobDataSource;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.util.JMSession;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.mime.MimeConstants;

public class SpamHandler {

    private static Log mLog = LogFactory.getLog(SpamHandler.class);

    private static SpamHandler sSpamHandler;
        
    public static synchronized SpamHandler getInstance() {
        if (sSpamHandler == null) {
            sSpamHandler = new SpamHandler();
        }
        return sSpamHandler;
    }

    private String mIsSpamAccount;

    private String mIsNotSpamAccount;
        
    private InternetAddress mIsSpamAddress;
        
    private InternetAddress mIsNotSpamAddress;
        
    private Thread mSpamHandlerThread;
    
    private String mEnvelopeFrom;
    private String mSenderHeader;
    private String mTypeHeader;
    private String mTypeSpam;
    private String mTypeHam;
    private boolean mAddOrigIp;
    
    public SpamHandler() {
        Config config;
        
        try {
            config = Provisioning.getInstance().getConfig();
        } catch (ServiceException se) {
            ZimbraLog.misc.warn("exception configuring spam/notspam", se);
            return;
        }

        mIsSpamAccount = config.getAttr(Provisioning.A_zimbraSpamIsSpamAccount, null);
        if (mIsSpamAccount == null) {
            if (mLog.isDebugEnabled()) {
                mLog.debug(Provisioning.A_zimbraSpamIsSpamAccount + " is not configured");
            }
        } else {
            try {
                mIsSpamAddress = new InternetAddress(mIsSpamAccount, true);
            } catch (AddressException ae) {
                ZimbraLog.misc.warn("exception parsing " + Provisioning.A_zimbraSpamIsSpamAccount + " " + mIsSpamAccount, ae); 
            }
        }

        mIsNotSpamAccount = config.getAttr(Provisioning.A_zimbraSpamIsNotSpamAccount, null);
        if (mIsNotSpamAccount == null) {
            if (mLog.isDebugEnabled()) {
                mLog.debug(Provisioning.A_zimbraSpamIsNotSpamAccount + " is not configured");
            }
        } else {
            try {
                mIsNotSpamAddress = new InternetAddress(mIsNotSpamAccount, true);
            } catch (AddressException ae) {
                ZimbraLog.misc.warn("exception parsing " + Provisioning.A_zimbraSpamIsNotSpamAccount + " " + mIsNotSpamAccount, ae); 
            }
        }

        mEnvelopeFrom = config.getAttr(Provisioning.A_zimbraSpamReportEnvelopeFrom, "<>");
        mSenderHeader = config.getAttr(Provisioning.A_zimbraSpamReportSenderHeader, "X-Zimbra-Spam-Report-Sender");
        mTypeHeader = config.getAttr(Provisioning.A_zimbraSpamReportTypeHeader, "X-Zimbra-Spam-Report-Type");
        mTypeSpam = config.getAttr(Provisioning.A_zimbraSpamReportTypeSpam, "spam");
        mTypeHam = config.getAttr(Provisioning.A_zimbraSpamReportTypeHam, "ham");
        mAddOrigIp = config.getBooleanAttr(Provisioning.A_zimbraSmtpSendAddOriginatingIP, true);
        
        if (mIsSpamAddress != null || mIsNotSpamAddress != null) {
            Runnable r = new Runnable() {
                public void run() {
                    reportLoop();
                }
            };
            mSpamHandlerThread = new Thread(r);
            mSpamHandlerThread.setName("Junk-NotJunk-Handler");
            mSpamHandlerThread.setDaemon(true);
            mSpamHandlerThread.start();
        }
    }
   
    private void sendReport(SpamReport sr) throws ServiceException, MessagingException {
        String isSpamString = sr.mIsSpam ? mTypeSpam : mTypeHam;
        InternetAddress toAddress = sr.mIsSpam ? mIsSpamAddress : mIsNotSpamAddress;
        
        SMTPMessage out = new SMTPMessage(JMSession.getSmtpSession());
        
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(sr.mMailboxId);
        Message msg = mbox.getMessageById(null, sr.mMessageId);
        
        MimeMultipart mmp = null;
        mmp = new MimeMultipart("mixed");
        out.setContent(mmp);
        
        MimeBodyPart infoPart = new MimeBodyPart();
        infoPart.setHeader("Content-Description", "Zimbra spam classification report");
        StringBuffer sb = new StringBuffer(128);
        sb.append("Classified-By: ").append(sr.mAccountName).append("\r\n");
        sb.append("Classified-As: ").append(isSpamString).append("\r\n");
        infoPart.setContent(sb.toString(), "text/plain");
        mmp.addBodyPart(infoPart);
        
        MailboxBlob blob = msg.getBlob();
        MimeBodyPart mbp = new MimeBodyPart();
        mbp.setDataHandler(new DataHandler(new MailboxBlobDataSource(blob)));
        mbp.setHeader("Content-Type", MimeConstants.CT_MESSAGE_RFC822);
        mbp.setHeader("Content-Disposition", Part.ATTACHMENT);
        mmp.addBodyPart(mbp);
        
        out.addHeader(mSenderHeader, sr.mAccountName);
        out.addHeader(mTypeHeader, isSpamString);
        
        if (mAddOrigIp && sr.mOrigIp != null)
            out.addHeader(MailSender.X_ORIGINATING_IP, MailSender.formatXOrigIpHeader(sr.mOrigIp));
        
        out.setRecipient(javax.mail.Message.RecipientType.TO, toAddress);
        out.setEnvelopeFrom(mEnvelopeFrom);
        out.setSubject("zimbra-spam-report: " + sr.mAccountName + ": " + isSpamString);
        Transport.send(out);
        
        ZimbraLog.misc.info("Sent " + sr);
    }

    private static final class SpamReport {
        final String mAccountName;
        final long mMailboxId;
        final int mMessageId;
        final boolean mIsSpam;
        final String mOrigIp;
        private String mDescString;
        
        SpamReport(String accountName, long mailboxId, int messageId, boolean isSpam, String origIp) {
            mAccountName = accountName;
            mMailboxId = mailboxId;
            mMessageId = messageId;
            mIsSpam = isSpam;
            mOrigIp = origIp;
        }
        
        @Override public String toString() {
            if (mDescString == null) {
                mDescString = "spamreport: acct=" + mAccountName + 
                " mbox=" + mMailboxId + " id=" + mMessageId + 
                " report=" + (!mIsSpam ? "!" : "") + "spam" +
                " origIp=" + mOrigIp; 
            }
            return mDescString;
        }
    }
    
    private static final int mSpamReportQueueSize = LC.zimbra_spam_report_queue_size.intValue();
    
    private Object mSpamReportQueueLock = new Object();
    
    List<SpamReport> mSpamReportQueue = new ArrayList<SpamReport>(mSpamReportQueueSize);

    void reportLoop() {
        while (true) {
            List<SpamReport> workQueue = null; 
            synchronized (mSpamReportQueueLock) {
                while (mSpamReportQueue.size() == 0) {
                    try {
                        mSpamReportQueueLock.wait();
                    } catch (InterruptedException ie) {
                        ZimbraLog.misc.warn("SpamHandler interrupted", ie);
                    }
                }
                workQueue = mSpamReportQueue;
                mSpamReportQueue = new ArrayList<SpamReport>(mSpamReportQueueSize);
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

    private void enqueue(OperationContext octxt, String accountName, Mailbox mbox, List<Message> msgs, boolean isSpam) {
        
        String origIp = octxt == null ? null : octxt.getRequestIP();
        
        synchronized (mSpamReportQueueLock) {
            for (Message msg : msgs) {
                SpamReport sr = new SpamReport(accountName, mbox.getId(), msg.getId(), isSpam, origIp);
                if (mSpamReportQueue.size() > mSpamReportQueueSize) {
                    ZimbraLog.misc.warn("SpamHandler queue size " + mSpamReportQueue.size() + " too large, ignored " + sr);
                    continue;
                }
                mSpamReportQueue.add(sr);
                if (ZimbraLog.misc.isDebugEnabled()) ZimbraLog.misc.debug("SpamHandler enqueued " + sr);
            }
            mSpamReportQueueLock.notify();
        }
    }
        
    public void handle(OperationContext octxt, Mailbox mbox, int id, byte type, boolean isSpam) throws ServiceException {
        if (isSpam && mIsSpamAddress == null) {
            if (mLog.isDebugEnabled()) mLog.debug("isSpam, but isSpamAddress is null, nothing to do");
            return;
        }
        if ((!isSpam) && mIsNotSpamAddress == null) {
            if (mLog.isDebugEnabled()) mLog.debug("isNotSpam, but isNotSpamAddress is null, nothing to do");
            return;
        }

        String accountName = null;
        accountName = mbox.getAccount().getName();
        
        List<Message> msgs = null;
        if (type == MailItem.TYPE_MESSAGE) {
            (msgs = new ArrayList<Message>()).add(mbox.getMessageById(null, id));
        } else if (type == MailItem.TYPE_CONVERSATION) {
            msgs = mbox.getMessagesByConversation(null, id);
        } else {
            ZimbraLog.misc.warn("SpamHandler called on unhandled item type=" + MailItem.getNameForType(type) + " account=" + accountName +  " id=" + id);
            return;
        }

        enqueue(octxt, accountName, mbox, msgs, isSpam);
    }
    
    /**
     * Stores the last known value of <tt>zimbraSpamHeaderValue</tt>.  Used
     * for determining whether {@link #sSpamPattern} needs to be recompiled. 
     */
    private static String sSpamHeaderValue;
    
    /**
     * Compiled version of {@link #sSpamHeaderValue}.
     */
    private static Pattern sSpamPattern;

    /**
     * Returns <tt>true</tt> if the value of the header named <tt>zimbraSpamHeader</tt>
     * matches the pattern specified by <tt>zimbraSpamHeaderValue</tt>.
     */
    public static boolean isSpam(MimeMessage msg) {
        try {
            Provisioning prov = Provisioning.getInstance();
            String spamHeader = prov.getConfig().getAttr(Provisioning.A_zimbraSpamHeader, null);
            if (StringUtil.isNullOrEmpty(spamHeader)) {
                return false;
            }
            String spamHeaderValue = prov.getConfig().getAttr(Provisioning.A_zimbraSpamHeaderValue, null);
            if (StringUtil.isNullOrEmpty(spamHeaderValue)) {
                return false;
            }
            if (!spamHeaderValue.equals(sSpamHeaderValue)) {
                // Value has changed.  Recompile pattern.
                sSpamHeaderValue = spamHeaderValue;
                sSpamPattern = Pattern.compile(spamHeaderValue);
            }
            String val = Mime.getHeader(msg, spamHeader);
            if (val != null) {
                Matcher m = sSpamPattern.matcher(val);
                if (m.matches()) {
                    return true;
                }
            }
        } catch (Exception e) {
            ZimbraLog.mailbox.warn("Unable to determine whether the message is spam.", e);
        }
        return false;
    }
    
}
