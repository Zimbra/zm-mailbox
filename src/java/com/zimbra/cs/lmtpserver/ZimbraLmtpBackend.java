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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.lmtpserver;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Notification;
import com.zimbra.cs.mailbox.SharedDeliveryContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ThreadLocalData;
import com.zimbra.cs.stats.StatsFile;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.util.ZimbraLog;

public class ZimbraLmtpBackend implements LmtpBackend {
	private static Log mLog = LogFactory.getLog(ZimbraLmtpBackend.class);
	
	public LmtpStatus getAddressStatus(LmtpAddress address) {
        
        String addr = address.getEmailAddress();

        try {
		    Account acct = Provisioning.getInstance().getAccountByName(addr);
		    if (acct == null) {
		    	mLog.info("rejecting address " + addr + ": no account");
		    	return LmtpStatus.REJECT;
		    }
		    
		    String acctStatus = acct.getAccountStatus();
		    if (acctStatus == null) {
		    	mLog.warn("rejecting address " + addr + ": no account status");
		    	return LmtpStatus.REJECT;
		    }

            if (acctStatus.equals(Provisioning.ACCOUNT_STATUS_MAINTENANCE)) {
                mLog.info("try again for address " + addr + ": account status maintenance");
                return LmtpStatus.TRYAGAIN;
            }

            if (acctStatus.equals(Provisioning.ACCOUNT_STATUS_CLOSED)) {
                mLog.info("rejecting address " + addr + ": account status closed");
                return LmtpStatus.REJECT;
            }
            
		    if (acctStatus.equals(Provisioning.ACCOUNT_STATUS_ACTIVE) ||
                acctStatus.equals(Provisioning.ACCOUNT_STATUS_LOCKED)) 
            {
		        return LmtpStatus.ACCEPT;
		    }

		    mLog.info("rejecting address " + addr + ": unknown account status " + acctStatus);
		    return LmtpStatus.REJECT;
		
		} catch (ServiceException e) {
            if (e.isReceiversFault()) {
                mLog.warn("try again for address " + addr + ": exception occurred", e);
                return LmtpStatus.TRYAGAIN;
            } else {
                mLog.warn("rejecting address " + addr + ": exception occurred", e);
                return LmtpStatus.REJECT;
            }
		}
	}
	
	public void deliver(LmtpEnvelope env, byte[] data) {
        try {
            deliverMessageToLocalMailboxes(data, env.getRecipients(), env.getSender().getEmailAddress());
        } catch (MessagingException me) {
            mLog.warn("Exception delivering mail (permanent failure)", me);
            setDeliveryStatuses(env.getRecipients(), LmtpStatus.REJECT);
        } catch (ServiceException e) {
            mLog.warn("Exception delivering mail (temporary failure)", e);
            setDeliveryStatuses(env.getRecipients(), LmtpStatus.TRYAGAIN);
        }
    }

    private static class RecipientDetail {
    	public Account account;
        public Mailbox mbox;
        public ParsedMessage pm;
        public boolean skip;  // whether delivery to mailbox should be skipped
        public RecipientDetail(Account a, Mailbox m, ParsedMessage p, boolean skip) {
        	account = a;
            mbox = m;
            pm = p;
            this.skip = skip;
        }
    }

    private static final String STAT_NUM_RECIPIENTS = "num_recipients";
    private static final StatsFile STATS_FILE =
        new StatsFile("perf_lmtp.csv", new String[] { STAT_NUM_RECIPIENTS }, true);
    
    private void deliverMessageToLocalMailboxes(byte[] data, List /*<LmtpAddress>*/ recipients, String envSender)
    throws MessagingException, ServiceException {

        boolean shared = recipients.size() > 1;
        SharedDeliveryContext sharedDeliveryCtxt = new SharedDeliveryContext(shared);

        Map /*<LmtpAddress, RecipientDetail>*/ rcptMap = new HashMap(recipients.size());

        try {
            // Examine attachments indexing option for all recipients and
            // prepare ParsedMessage versions needed.  Parsing is done before
            // attempting delivery to any recipient.  Therefore, parse error
            // will result in non-delivery to all recipients.

            // ParsedMessage for users with attachments indexing
            ParsedMessage pmAttachIndex = null;
            // ParsedMessage for users without attachments indexing
            ParsedMessage pmNoAttachIndex = null;

            for (Iterator iter = recipients.iterator(); iter.hasNext(); ) {
                LmtpAddress recipient = (LmtpAddress) iter.next();
                String rcptEmail = recipient.getEmailAddress();

                Account account = null;
                Mailbox mbox = null;
                boolean attachmentsIndexingEnabled = true;
                try {
                    account = Provisioning.getInstance().getAccountByName(rcptEmail);
                    if (account == null) {
                        ZimbraLog.mailbox.warn("No account found delivering mail to " + rcptEmail);
                        continue;
                    }
                    mbox = Mailbox.getMailboxByAccount(account);
                    if (mbox == null) {
                        ZimbraLog.mailbox.warn("No mailbox found delivering mail to " + rcptEmail);
                        continue;
                    }
                    attachmentsIndexingEnabled = mbox.attachmentsIndexingEnabled();
                } catch (ServiceException se) {
                    ZimbraLog.mailbox.warn("Exception delivering mail to " + rcptEmail, se);
                }

                if (account != null && mbox != null) {
                    ParsedMessage pm;
                    if (attachmentsIndexingEnabled) {
                        if (pmAttachIndex == null)
                            pmAttachIndex = new ParsedMessage(data, true).analyze();
                        pm = pmAttachIndex;
                    } else {
                        if (pmNoAttachIndex == null)
                            pmNoAttachIndex = new ParsedMessage(data, false).analyze();
                        pm = pmNoAttachIndex;
                    }
                    assert(pm != null);
                    boolean skip;
                    if (shared) {
                        // Skip delivery to mailboxes in backup mode.
                    	skip = !mbox.beginSharedDelivery();
                    } else {
                        // For non-shared delivery (i.e. only one recipient),
                        // always deliver regardless of backup mode.
                        skip = false;
                    }
                    rcptMap.put(recipient, new RecipientDetail(account, mbox, pm, skip));
                }
            }

            // We now know which addresses are valid and which ParsedMessage
            // version each recipient needs.  Deliver!
            try {
                // Performance
                if (ZimbraLog.perf.isDebugEnabled()) {
                    ThreadLocalData.reset();
                }

                for (Iterator iter = recipients.iterator(); iter.hasNext(); ) {
                    LmtpAddress recipient = (LmtpAddress) iter.next();
                    String rcptEmail = recipient.getEmailAddress();
                    LmtpStatus status = LmtpStatus.TRYAGAIN;
                    RecipientDetail rd = (RecipientDetail) rcptMap.get(recipient);
                    try {
                        if (rd != null) {
                            if (!rd.skip) {
                                Account account = rd.account;
                                Mailbox mbox = rd.mbox;
                                ParsedMessage pm = rd.pm;
                                Message msg = null;
                                
                                ZimbraLog.addAccountNameToContext(account.getName());
                                if (!DebugConfig.disableFilter) {
                                    msg = RuleManager.getInstance().applyRules(account, mbox, pm, pm.getRawData().length,
                                                                               rcptEmail, sharedDeliveryCtxt);
                                } else {
                                    msg = mbox.addMessage(null, pm, Mailbox.ID_FOLDER_INBOX, false, Flag.FLAG_UNREAD, null,
                                                          rcptEmail, sharedDeliveryCtxt);
                                }
                                if (msg != null) {
                                    notify(account, mbox, msg, rcptEmail, envSender, pm);
                                }
                                status = LmtpStatus.ACCEPT;
                            } else {
                                // Delivery to mailbox skipped.  Let MTA retry again later.
                                // This case happens for shared delivery to a mailbox in
                                // backup mode.
                                mLog.info("try again for message " + rcptEmail + ": mailbox skipped");
                                status = LmtpStatus.TRYAGAIN;
                            }
                        } else {
                            // Account or mailbox not found.
                            mLog.info("rejecting message " + rcptEmail + ": account or mailbox not found");
                        	status = LmtpStatus.REJECT;
                        }
                    } catch (IOException ioe) {
                        status = LmtpStatus.TRYAGAIN;
                        mLog.info("try again for " + rcptEmail + ": exception occurred", ioe);
                    } catch (ServiceException se) {
                        if (se.getCode().equals(MailServiceException.QUOTA_EXCEEDED)) {
                            mLog.info("rejecting message " + rcptEmail + ": overquota");
                            status = LmtpStatus.OVERQUOTA;
                        } else if (se.isReceiversFault()) {
                            mLog.info("try again for message " + rcptEmail + ": exception occurred", se);
                            status = LmtpStatus.TRYAGAIN;
                        } else {
                            mLog.info("rejecting message " + rcptEmail + ": exception occurred", se);
                            status = LmtpStatus.REJECT;
                        }
                    } catch (Exception e) {
                        status = LmtpStatus.TRYAGAIN;
                        mLog.info("try again for message " + rcptEmail + ": exception occurred", e);
                    } finally {
                        ZimbraLog.clearContext();
                        recipient.setDeliveryStatus(status);
                        if (shared && rd != null && !rd.skip) {
                            rd.mbox.endSharedDelivery();
                            rd.skip = true;
                        }
                    }
                }
                
                if (ZimbraLog.perf.isDebugEnabled()) {
                    ZimbraPerf.writeStats(STATS_FILE, recipients.size());
                }
                
            } finally {
                // Clean up blobs in incoming directory after delivery to all recipients.
                if (shared) {
                    Blob sharedBlob = sharedDeliveryCtxt.getBlob();
                    if (sharedBlob != null) {
                        try {
                            StoreManager.getInstance().delete(sharedBlob);
                        } catch (IOException e) {
                            mLog.warn("Unable to delete temporary incoming blob after delivery: " + sharedBlob.toString(), e);
                        }
                    }
                }
            }
        } finally {
            // Take mailboxes out of shared delivery mode.
            if (shared) {
                for (Iterator iter = rcptMap.values().iterator(); iter.hasNext(); ) {
                	RecipientDetail rd = (RecipientDetail) iter.next();
                    if (!rd.skip && rd.mbox != null)
                        rd.mbox.endSharedDelivery();
                }
            }
        }
    }

    /**
     * Sends any necessary notifications for the specified message.
     */
    private void notify(Account account, Mailbox mbox, Message msg, String rcptEmail, String envSender, ParsedMessage pm) {
        // If notification fails, log a warning and continue so that message delivery
        // isn't affected
        try {
            Notification.notifyIfNecessary(account, msg, rcptEmail, pm);
        } catch (MessagingException e) {
            ZimbraLog.mailbox.warn("Unable to send new mail notification", e);
        }
        
        try {
            Notification.outOfOfficeIfNecessary(account, mbox, msg, rcptEmail, envSender, pm);
        } catch (MessagingException e) {
            ZimbraLog.mailbox.warn("Unable to send out-of-office reply", e);
        } catch (ServiceException e) {
            ZimbraLog.mailbox.warn("Unable to send out-of-office reply", e);
        }
    }
    
    private void setDeliveryStatuses(List recipients, LmtpStatus status) {
        for (Iterator iter = recipients.iterator(); iter.hasNext();) {
            LmtpAddress recipient = (LmtpAddress)iter.next();
            recipient.setDeliveryStatus(status);
        }
    }
}
