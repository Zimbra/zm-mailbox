package com.zimbra.cs.mailbox.acl;

import com.google.common.collect.Sets;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.ScheduledTask;
import com.zimbra.cs.mailbox.ScheduledTaskManager;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.soap.mail.message.SendShareNotificationRequest;
import com.zimbra.soap.mail.type.EmailAddrInfo;
import com.zimbra.soap.type.Id;

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
        ZMailbox zMbox = getZMailbox(mbox);
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
                try {
                    String address = getGranteeAddress(grant);
                    if (address != null) {
                        sendGrantExpiryNotification(zMbox, itemId, address);
                    }
                } finally {
                    mbox.revokeAccess(null, true, itemId, granteeId);
                }
            }
        }
        return aGrantWithExpiryExists ? item : null;
    }

    private static void sendGrantExpiryNotification(ZMailbox zMbox, int itemId, String address)
            throws ServiceException {
        SendShareNotificationRequest req = new SendShareNotificationRequest();
        req.setEmailAddresses(Sets.newHashSet(new EmailAddrInfo(address)));
        req.setAction(SendShareNotificationRequest.Action.expire);
        req.setItem(new Id(Integer.toString(itemId)));
        zMbox.invokeJaxb(req);
    }

    private static String getGranteeAddress(ACL.Grant grant) throws ServiceException {
        switch (grant.getGranteeType()) {
            case ACL.GRANTEE_USER:
                Account granteeAcct = Provisioning.getInstance().get(Key.AccountBy.id, grant.getGranteeId());
                if (granteeAcct != null) {
                    return granteeAcct.getName();
                }
                break;
            case ACL.GRANTEE_GUEST:
                return grant.getGranteeId();
            default:
                return null;
        }
        return null;
    }

    private static ZMailbox getZMailbox(Mailbox mbox) throws ServiceException {
        Account account = mbox.getAccount();
        ZMailbox.Options options = new ZMailbox.Options();
        options.setNoSession(true);
        options.setAuthToken(AuthProvider.getAuthToken(account).toZAuthToken());
        options.setUri(AccountUtil.getSoapUri(account));
        return new ZMailbox(options);
    }

    static void cancel(int mailboxId, int itemId) throws ServiceException {
        ScheduledTaskManager.cancel(ExpireGrantsTask.class.getName(), getTaskName(itemId), mailboxId, false);
    }
}
