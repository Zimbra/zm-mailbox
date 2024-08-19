/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.AuthProviderException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.cs.service.mail.SaveDraft;
import com.zimbra.cs.service.util.ItemId;

import java.util.Date;

/**
 * Auto-send-draft scheduled task.
 */
public class AutoSendDraftTask extends ScheduledTask<Object> {

    private static final String TASK_NAME_PREFIX = "autoSendDraftTask";
    private Server serverName;
    private static final String DRAFT_ID_PROP_NAME = "draftId";
    private String delegatorAccountId;
    private Element request;
    public static final String ZIMBRA_MAILBOX_APP = "ZIMBRA_MAILBOX_APP";
    public static final String ZIMBRA_CONSTANT_VERSION = "1.0";


    public Server getServerName() {
        return serverName;
    }

    public void setServerName(Server serverName) {
        this.serverName = serverName;
    }

    public String getDelegatorAccountId() {
        return delegatorAccountId;
    }

    public void setDelegatorAccountId(String delegatorAccountId) {
        this.delegatorAccountId = delegatorAccountId;
    }

    public Element getRequest() {
        return request;
    }

    public void setRequest(Element request) {
        this.request = request;
    }

    /**
     * Returns the task name.
     */
    @Override public String getName() {
        return TASK_NAME_PREFIX + getProperty(DRAFT_ID_PROP_NAME);
    }

    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @return computed result
     * @throws Exception if unable to compute a result
     */
    @Override public Void call() throws Exception {
        ZimbraLog.scheduler.debug("Running task %s", this);
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        Mailbox delegatorMbox = mbox;

        if (mbox == null) {
            ZimbraLog.scheduler.error("Mailbox for id %s does not exist", getMailboxId());
            return null;
        }
        Integer draftId = new Integer(getProperty(DRAFT_ID_PROP_NAME));
        Message msg;
        try {
            msg = (Message) mbox.getItemById(null, draftId, MailItem.Type.MESSAGE);
        } catch (MailServiceException.NoSuchItemException e) {
            // Draft might have been deleted
            ZimbraLog.scheduler.debug("Draft message with id %s no longer exists in mailbox", draftId);
            return null;
        }
        if (msg.getDraftAutoSendTime() == 0) {
            ZimbraLog.scheduler.warn("Message with id %s is not a Draft scheduled to be auto-sent", draftId);
            return null;
        }
        if (msg.isTagged(Flag.FlagInfo.DELETED) || msg.inTrash()) {
            ZimbraLog.scheduler.debug("Draft with id %s is deleted", draftId);
            return null;
        }
        // send draft
        MailSender mailSender = mbox.getMailSender();
        mailSender.setOriginalMessageId(StringUtil.isNullOrEmpty(msg.getDraftOrigId()) ? null : new ItemId(msg.getDraftOrigId(), mbox.getAccountId()));
        mailSender.setReplyType(StringUtil.isNullOrEmpty(msg.getDraftReplyType()) ? null : msg.getDraftReplyType());
        mailSender.setIdentity(StringUtil.isNullOrEmpty(msg.getDraftIdentityId()) ? null : mbox.getAccount().getIdentityById(msg.getDraftIdentityId()));
        Provisioning provisioning = Provisioning.getInstance();
        Account delegatorAccount = provisioning.getAccountById(getDelegatorAccountId());
        if (delegatorAccount != null && Provisioning.onLocalServer(delegatorAccount)) {
            delegatorMbox = MailboxManager.getInstance().getMailboxByAccount(delegatorAccount);
            /* if delegator is on same server, just fetch the delegatorMbox object
            and use it for sending the Mime. */
            mailSender.sendMimeMessage(new OperationContext(mbox), delegatorMbox, msg.getMimeMessage());
        } else {
            /* if delegator is on different server, invoke the saveDraft request on that server. */
            Element reqForDelegatorAccount = getRequest();
            AuthToken tokenForSetup = null;
            if (delegatorAccount != null) {
                try {
                    tokenForSetup = AuthProvider.getAuthToken(delegatorAccount, AuthToken.Usage.AUTH);
                } catch (AuthProviderException e) {
                    throw AuthProviderException.FAILURE("Error while getting AuthToken of Delegator Account");
                }
            }
            if (getServerName() != null && reqForDelegatorAccount != null) {
                String url = URLUtil.getSoapURL(getServerName(), true);
                SoapHttpTransport transport = new SoapHttpTransport(url);
                transport.setAuthToken(tokenForSetup.toZAuthToken());
                transport.setUserAgent(ZIMBRA_MAILBOX_APP, ZIMBRA_CONSTANT_VERSION);
                reqForDelegatorAccount.addAttribute(SaveDraft.IS_DELEGATED_REQUEST, true);
                reqForDelegatorAccount.addAttribute(SaveDraft.DELEGATEE_ACCOUNT_ID, mbox.getAccountId());
                // invoking the Soap call for SaveDraftRequest on a delegator's server.
                transport.invoke(reqForDelegatorAccount);
            }
        }
        // now delete the draft
        mbox.delete(null, draftId, MailItem.Type.MESSAGE);
        return null;
    }

    /**
     * Cancels any existing scheduled auto-send task for the given draft.
     *
     * @param draftId
     * @param mailboxId
     * @throws ServiceException
     */
    public static void cancelTask(int draftId, int mailboxId) throws ServiceException {
        ScheduledTaskManager.cancel(AutoSendDraftTask.class.getName(),
                                    TASK_NAME_PREFIX + Integer.toString(draftId),
                                    mailboxId,
                                    true);

    }

    /**
     * Schedules an auto-send task for the given draft at the specified time.
     *
     * @param draftId
     * @param mailboxId
     * @param autoSendTime
     * @throws ServiceException
     */

    public static void scheduleTask(int draftId, int mailboxId, String delegatorAccountId, long autoSendTime, Element request, Server serverName) throws ServiceException {
        AutoSendDraftTask autoSendDraftTask = new AutoSendDraftTask();
        autoSendDraftTask.setMailboxId(mailboxId);
        autoSendDraftTask.setDelegatorAccountId(delegatorAccountId);
        autoSendDraftTask.setExecTime(new Date(autoSendTime));
        autoSendDraftTask.setRequest(request);
        autoSendDraftTask.setServerName(serverName);
        autoSendDraftTask.setProperty(DRAFT_ID_PROP_NAME, Integer.toString(draftId));
        ScheduledTaskManager.schedule(autoSendDraftTask);
    }

    public static void scheduleTask(int draftId, int mailboxId, long autoSendTime) throws ServiceException {
        scheduleTask(draftId, mailboxId, null, autoSendTime, null, null);
    }
}
