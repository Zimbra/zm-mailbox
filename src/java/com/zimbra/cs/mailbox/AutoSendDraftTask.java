package com.zimbra.cs.mailbox;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.service.util.ItemId;

import java.util.Date;

/**
 * Auto-send-draft scheduled task.
 */
public class AutoSendDraftTask extends ScheduledTask<Object> {

    private static final String TASK_NAME_PREFIX = "autoSendDraftTask";
    private static final String DRAFT_ID_PROP_NAME = "draftId";

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
        if (ZimbraLog.scheduler.isDebugEnabled())
            ZimbraLog.scheduler.debug("Running task " + this);
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
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
        if (msg.isTagged(Flag.ID_DELETED) || msg.inTrash()) {
            ZimbraLog.scheduler.debug("Draft with id %s is deleted", draftId);
            return null;
        }
        // send draft
        MailSender mailSender = mbox.getMailSender();
        mailSender.setOriginalMessageId(StringUtil.isNullOrEmpty(msg.getDraftOrigId()) ? null : new ItemId(msg.getDraftOrigId(), mbox.getAccountId()));
        mailSender.setReplyType(StringUtil.isNullOrEmpty(msg.getDraftReplyType()) ? null : msg.getDraftReplyType());
        mailSender.setIdentity(StringUtil.isNullOrEmpty(msg.getDraftIdentityId()) ? null : mbox.getAccount().getIdentityById(msg.getDraftIdentityId()));
        mailSender.sendMimeMessage(null, mbox, msg.getMimeMessage());
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
    public static void scheduleTask(int draftId, int mailboxId, long autoSendTime) throws ServiceException {
        AutoSendDraftTask autoSendDraftTask = new AutoSendDraftTask();
        autoSendDraftTask.setMailboxId(mailboxId);
        autoSendDraftTask.setExecTime(new Date(autoSendTime));
        autoSendDraftTask.setProperty(DRAFT_ID_PROP_NAME, Integer.toString(draftId));
        ScheduledTaskManager.schedule(autoSendDraftTask);
    }
}
