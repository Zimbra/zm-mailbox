/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.util;

import java.util.ArrayList;
import java.util.List;

import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.mail.smtp.SMTPMessage;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.localconfig.LC;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailboxBlob;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.BlobDataSource;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.JMSession;
import com.zimbra.cs.util.ZimbraLog;

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
        String isSpamString = sr.mIsSpam ? "spam" : "!spam";
        InternetAddress toAddress = sr.mIsSpam ? mIsSpamAddress : mIsNotSpamAddress;
        
        SMTPMessage out = new SMTPMessage(JMSession.getSession());
        
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
        mbp.setDataHandler(new DataHandler(new BlobDataSource(blob)));
        mbp.setHeader("Content-Type", blob.getMimeType());
        mbp.setHeader("Content-Disposition", Part.ATTACHMENT);
        mmp.addBodyPart(mbp);
        
        out.setRecipient(javax.mail.Message.RecipientType.TO, toAddress);
        out.setEnvelopeFrom("<>");
        out.setSubject("zimbra-spam-report: " + sr.mAccountName + ": " + isSpamString);
        Transport.send(out);
        
        ZimbraLog.misc.info("Sent " + sr);
    }

    private static final class SpamReport {
        final String mAccountName;
        final int mMailboxId;
        final int mMessageId;
        final boolean mIsSpam;
        private String mDescString;
        
        SpamReport(String accountName, int mailboxId, int messageId, boolean isSpam) {
            mAccountName = accountName;
            mMailboxId = mailboxId;
            mMessageId = messageId;
            mIsSpam = isSpam;
            
        }
        
        public String toString() {
            if (mDescString == null) {
                mDescString = "spamreport: acct=" + mAccountName + " mbox=" + mMailboxId + " id=" + mMessageId + " report=" + (!mIsSpam ? "!" : "") + "spam"; 
            }
            return mDescString;
        }
    }
    
    private static final int mSpamReportQueueSize = LC.zimbra_spam_report_queue_size.intValue();
    
    private Object mSpamReportQueueLock = new Object();
    
    List<SpamReport> mSpamReportQueue = new ArrayList<SpamReport>(mSpamReportQueueSize);

    private void reportLoop() {
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

    private void enqueue(String accountName, Mailbox mbox, Message[] msgs, boolean isSpam) {
        synchronized (mSpamReportQueueLock) {
            for (int i = 0; i < msgs.length; i++) {
                SpamReport sr = new SpamReport(accountName, mbox.getId(), msgs[i].getId(), isSpam);
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
        
    public void handle(Mailbox mbox, int id, byte type, boolean isSpam) throws ServiceException {
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
        
        Message[] msgs = null;
        if (type == MailItem.TYPE_MESSAGE) {
            msgs = new Message[] { mbox.getMessageById(null, id) };
        } else if (type == MailItem.TYPE_CONVERSATION) {
            msgs = mbox.getMessagesByConversation(null, id);
        } else {
            if (type != MailItem.TYPE_MESSAGE && type != MailItem.TYPE_CONVERSATION) {
                ZimbraLog.misc.warn("SpamHandler called on unhandled item type=" + MailItem.getNameForType(type) + " account=" + accountName +  " id=" + id);
                return;
            }
        }

        enqueue(accountName, mbox, msgs, isSpam);
    }
    
}
