package com.zimbra.common.mailbox;

import com.zimbra.common.service.ServiceException;

public interface BaseItemInfo {
    public MailItemType getMailItemType();
    /** @return item's ID.  IDs are unique within a Mailbox and are assigned in increasing
     * (though not necessarily gap-free) order. */
    public int getIdInMailbox() throws ServiceException;
    /**
     * @return the UID the item is referenced by in the IMAP server.  Returns <tt>0</tt> for items that require
     * renumbering because of moves.
     * The "IMAP UID" will be the same as the item ID unless the item has been moved after the mailbox owner's first
     * IMAP session. */
    public int getImapUid();
    /** Returns the "external" flag bitmask, which includes {@link Flag#BITMASK_UNREAD} when the item is unread. */
    public int getFlagBitmask();
    public String[] getTags();
    /** String representation of the item's folder ID */
    public int getFolderIdInMailbox() throws ServiceException;
    /** ID of the account containing this item */
    public String getAccountId() throws ServiceException;
}
