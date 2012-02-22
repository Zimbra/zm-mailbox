package com.zimbra.cs.mailbox.acl;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.ScheduledTask;
import com.zimbra.cs.mailbox.ScheduledTaskManager;

import java.util.List;

public class ExpireGrantsTask extends ScheduledTask {

    static final String TASK_NAME_PREFIX = "expireGrantsTask";
    static final String ITEM_ID_PROP_NAME = "itemId";

    /**
     * Returns the task name.
     */
    @Override
    public String getName() {
        return getTaskName(getProperty(ITEM_ID_PROP_NAME));
    }

    private static String getTaskName(String itemIdStr) {
        return TASK_NAME_PREFIX + itemIdStr;
    }

    private static String getTaskName(int itemId) {
        return getTaskName(Integer.toString(itemId));
    }

    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @return returns the item for which this task was run if we may want to schedule another instance of this
     *         task (at the next grant expiry); o/w null
     * @throws Exception if unable to compute a result
     * @see ExpireGrantsTaskCallback
     */
    public MailItem call() throws Exception {
        int itemId = Integer.valueOf(getProperty(ITEM_ID_PROP_NAME));
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        MailItem item;
        try {
            item = mbox.getItemById(null, itemId, MailItem.Type.UNKNOWN);
        } catch (MailServiceException.NoSuchItemException e) {
            // item seems to have been deleted; no problem
            return null;
        }
        ACL acl = item.getACL();
        if (acl == null) {
            return null;
        }
        List<ACL.Grant> grants = acl.getGrants();
        long now = System.currentTimeMillis();
        boolean aGrantWithExpiryExists = false;
        for (ACL.Grant grant : grants) {
            long expiry = grant.getEffectiveExpiry(acl);
            if (expiry == 0) {
                continue;
            }
            aGrantWithExpiryExists = true;
            if (now > expiry) {
                String granteeId;
                switch (grant.getGranteeType()) {
                    case ACL.GRANTEE_PUBLIC:
                        granteeId = GuestAccount.GUID_PUBLIC;
                        break;
                    case ACL.GRANTEE_AUTHUSER:
                        granteeId = GuestAccount.GUID_AUTHUSER;
                        break;
                    default:
                        granteeId = grant.getGranteeId();
                }
                mbox.revokeAccess(null, true, itemId, granteeId);
            }
        }
        return aGrantWithExpiryExists ? item : null;
    }
    
    static void cancel(int mailboxId, int itemId) throws ServiceException {
        ScheduledTaskManager.cancel(ExpireGrantsTask.class.getName(), getTaskName(itemId), mailboxId, false);
    }
}
