package com.zimbra.cs.service.util;

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
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailboxBlob;
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
        
    public SpamHandler() {
        reload();
    }

    public void reload() {
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
    }
        
    public void handle(Mailbox mbox, int id, byte type, boolean isSpam) {
        String accountName = null;
        try {
            accountName = mbox.getAccount().getName();
            handle0(accountName, mbox, id, type, isSpam);
        } catch (Throwable t1) {
            // We do not want failed spam training to get in the way of
            // anything, so we log errors and ignore them. This could end up
            // catching an OOM, but some other thread doing something else
            // will also catch the OOM and take appropriate action, so an OOM
            // swallowed here is probably OK. In the future, we should move the
            // spam training to it's own thread - in that case, ignoring any
            // errors would be perfectly OK.
            try {
                ZimbraLog.misc.warn("exception processing spam/notspam for" + 
                                    " account=" + accountName + " id=" + id + 
                                    " type=" + MailItem.getNameForType(type), t1);
            } catch (Throwable t2) {
            }
        } 
    }

    public void handle0(String accountName, Mailbox mbox, int id, byte type, boolean isSpam) throws ServiceException, MessagingException {
        if (isSpam && mIsSpamAddress == null) {
            if (mLog.isDebugEnabled()) {
                mLog.debug("isSpam, but isSpamAddress is null, nothing to do");
            }
            return;
        }
        if ((!isSpam) && mIsNotSpamAddress == null) {
            if (mLog.isDebugEnabled()) {
                mLog.debug("isNotSpam, but isNotSpamAddress is null, nothing to do");
            }
            return;
        }

        Message[] msgs = null;
        if      (type == MailItem.TYPE_MESSAGE) {
            msgs = new Message[1];
            msgs[0] = mbox.getMessageById(id);
        } else if (type == MailItem.TYPE_CONVERSATION) {
            msgs = mbox.getMessagesByConversation(id);
        } else {
            if (type != MailItem.TYPE_MESSAGE && type != MailItem.TYPE_CONVERSATION) {
                ZimbraLog.misc.warn("SpamHandler called on unhandleable item of type" + MailItem.getNameForType(type));
                return;
            }
        }

        String isSpamString = isSpam ? "spam" : "!spam";
        InternetAddress toAddress = isSpam ? mIsSpamAddress : mIsNotSpamAddress;
                
        for (int i = 0; i < msgs.length; i++) {
            SMTPMessage out = new SMTPMessage(JMSession.getSession());
                        
            MimeMultipart mmp = null;
            mmp = new MimeMultipart("mixed");
            out.setContent(mmp);

            MimeBodyPart infoPart = new MimeBodyPart();
            infoPart.setHeader("Content-Description", "Zimbra spam classification report");
            StringBuffer sb = new StringBuffer(128);
            sb.append("Classified-By: ").append(accountName).append("\r\n");
            sb.append("Classified-As: ").append(isSpamString).append("\r\n");
            infoPart.setContent(sb.toString(), "text/plain");
            mmp.addBodyPart(infoPart);
            
            MailboxBlob blob = msgs[i].getBlob();
            MimeBodyPart mbp = new MimeBodyPart();
            mbp.setDataHandler(new DataHandler(new BlobDataSource(blob)));
            mbp.setHeader("Content-Type", blob.getMimeType());
            mbp.setHeader("Content-Disposition", Part.ATTACHMENT);
            mmp.addBodyPart(mbp);

            out.setRecipient(javax.mail.Message.RecipientType.TO, toAddress);
            out.setEnvelopeFrom("<>");
            out.setSubject("zimbra-spam-report: " + accountName + ": " + isSpamString);
            Transport.send(out);
            
            ZimbraLog.misc.info("spam report sent flag=" + isSpamString + " to=" + toAddress + 
                                " for=" + accountName + " mid=" + msgs[i].getId());
        }
    }
}
