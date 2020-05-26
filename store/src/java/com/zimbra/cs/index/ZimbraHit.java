/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.index;

import java.util.Comparator;

import com.google.common.base.MoreObjects;
import com.zimbra.common.mailbox.MailItemType;
import com.zimbra.common.mailbox.ZimbraQueryHit;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.imap.ImapMessage;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.util.ItemId;

/**
 * Base class for a search "hit". Generally one iterates over a {@link ZimbraQueryResults}
 * to get the hits for a given query.
 *
 * @since Oct 15, 2004
 */
public abstract class ZimbraHit implements ZimbraQueryHit {

    protected final Mailbox mailbox;
    protected final ZimbraQueryResults results;
    protected final Object sortValue;
    protected String cachedName;
    protected ImapMessage cachedImapMessage;
    protected int cachedModseq = -1;
    protected int cachedParentId = 0;

    public ZimbraHit(ZimbraQueryResults results, Mailbox mbx, Object sort) {
        mailbox = mbx;
        this.results = results;

        switch (results.getSortBy().getKey()) {
            case NONE:
                sortValue = "";
                break;
            case ID:
                sortValue = toInteger(sort);
                break;
            case UNREAD:
            case DATE:
            case SIZE:
                sortValue = toLong(sort);
                break;
            default:
                assert sort instanceof String : sort;
                sortValue = sort;
                break;
        }
    }

    private Long toLong(Object value) {
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof String) {
            try {
                return new Long((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(String.format("Argument='%s' of class String", value));
            }
        } else {
            throw new IllegalArgumentException(String.format("Argument='%s' of class %s",
                    value, value != null ? value.getClass().getName() : "?"));
        }
    }

    private Integer toInteger(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return new Integer((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(String.format("Argument='%s' of class String", value));
            }
        } else {
            throw new IllegalArgumentException(String.format("Argument='%s' of class %s",
                    value, value != null ? value.getClass().getName() : "?"));
        }
    }

    @Override
    public abstract int getItemId() throws ServiceException;

    /**
     * Only guaranteed to return a useful value for local hits.
     *
     * @return the conversation ID, or 0 if there is not one
     */
    abstract int getConversationId() throws ServiceException;

    /**
     * Returns the {@link MailItem} corresponding to this hit, or NULL if one is not available (e.g. for a ProxiedHit).
     */
    public abstract MailItem getMailItem() throws ServiceException;

    /**
     * @param item which has been preloaded from the database or some other cache
     */
    abstract void setItem(MailItem item) throws ServiceException;

    /**
     * Returns TRUE if our associated {@link MailItem} is already loaded (or we don't have one, ie ProxiedHit).
     */
    abstract boolean itemIsLoaded() throws ServiceException;

    /**
     * Returns the "name" for sorting purposes. Only guaranteed to work if the search is sort-by-name.
     */
    abstract String getName() throws ServiceException;

    /**
     * Used for cross-mailbox-search, returns the AccountId of the hit.
     */
    public String getAcctIdStr() {
        return mailbox != null ? mailbox.getAccountId() : "NULL_ACCOUNTID";
    }

    @Override
    public String toString() {
        try {
            return MoreObjects.toStringHelper(this)
                .add("mbox", mailbox.getId())
                .add("item", getItemId())
                .add("name", getName())
                .toString();
        } catch (ServiceException e) {
            return e.toString();
        }
    }

    /**
     * Returns the sort value. Subclasses may override.
     *
     * @throws ServiceException failed to get the sort field
     */
    public Object getSortField(SortBy sort) throws ServiceException {
        switch (sort.getKey()) {
            case NONE:
                return "";
            case SUBJECT:
            case NAME:
            case RCPT:
                return ((String) sortValue).toUpperCase();
            case ID:
            case UNREAD:
            case DATE:
            case SIZE:
            default:
                return sortValue;
        }
    }

    public ItemId getParsedItemID() throws ServiceException {
        return new ItemId(mailbox, getItemId());
    }

    final Mailbox getMailbox() {
        return mailbox;
    }

    final ZimbraQueryResultsImpl getResults() {
        return (ZimbraQueryResultsImpl) results;
    }

    /**
     * Compare this hit to other using the sort field only.
     *
     * TODO: For string sort values, we compare(toUpper()) instead of compareIgnoreCase() or using a collator because
     * that's the only method that seems to give us the same results as the sorts from SQL server.
     *
     * @return {@code <0} if "this" is BEFORE other, {@code 0} if EQUAL, {@code >0} if this AFTER other
     * @throws ServiceException failed to compare
     */
    int compareTo(SortBy sort, ZimbraHit other) throws ServiceException {
        switch (sort) {
            case DATE_ASC:
            case SIZE_ASC:
            case READ_ASC:
                return Long.signum((Long) sortValue - (Long) other.sortValue);
            case ID_ASC:
                return Integer.signum((Integer) sortValue - (Integer) other.sortValue);
            case DATE_DESC:
            case SIZE_DESC:
            case READ_DESC:
                return Long.signum((Long) other.sortValue - (Long) sortValue);
            case ID_DESC:
                return Integer.signum((Integer) other.sortValue - (Integer) sortValue);
            case SUBJ_ASC:
            case NAME_ASC:
            case NAME_LOCALIZED_ASC:
            case RCPT_ASC:
                return ((String) sortValue).toUpperCase().compareTo(((String) other.sortValue).toUpperCase());
            case SUBJ_DESC:
            case NAME_DESC:
            case NAME_LOCALIZED_DESC:
            case RCPT_DESC:
                return ((String) other.sortValue).toUpperCase().compareTo(((String) sortValue).toUpperCase());
            case ATTACHMENT_ASC:
            case FLAG_ASC:
            case PRIORITY_ASC:
                return ((String) sortValue).compareTo((String) other.sortValue);
            case ATTACHMENT_DESC:
            case FLAG_DESC:
            case PRIORITY_DESC:
                return ((String) other.sortValue).compareTo((String) sortValue);
            case NONE:
            default:
                throw new IllegalArgumentException(sort.name());
        }
    }

    /**
     * Returns TRUE if this hit is local to this mailbox (ie not proxied).
     */
    boolean isLocal() {
        return true;
    }

    public ImapMessage getImapMessage() throws ServiceException {
        if (cachedImapMessage == null) {
            MailItem item = getMailItem();
            if (item == null) {
                return null;
            }
            cachedImapMessage = new ImapMessage(item);
        }
        return cachedImapMessage;
    }

    @Override
    public int getModifiedSequence() throws ServiceException {
        if (cachedModseq < 0) {
            MailItem item = getMailItem();
            cachedModseq = item != null ? item.getModifiedSequence() : 0;
        }
        return cachedModseq;
    }

    @Override
    public int getParentId() throws ServiceException {
        if (cachedParentId == 0) {
            MailItem item = getMailItem();
            cachedParentId = item != null ? item.getParentId() : -1;
        }
        return cachedParentId;
    }

    @Override
    public int getImapUid() throws ServiceException {
        return getMailItem().getImapUid();
    }

    @Override
    public int getFlagBitmask() throws ServiceException {
        return getMailItem().getFlagBitmask();
    }

    @Override
    public String[] getTags() throws ServiceException {
        return getMailItem().getTags();
    }

    @Override
    public MailItemType getMailItemType() throws ServiceException {
        return getMailItem().getType().toCommon();
    }

    final void cacheImapMessage(ImapMessage value) {
        cachedImapMessage = value;
    }

    final void cacheModifiedSequence(int value) {
        cachedModseq = value;
    }

    final void cacheParentId(int value) {
        cachedParentId = value;
    }

    /**
     * Return a comparator which sorts by the sort field, and THEN by the mail-item-id
     *
     * @param sortOrder
     */
    static Comparator<ZimbraHit> getSortAndIdComparator(SortBy sortOrder) {
        return new ZimbraHitSortAndIdComparator(sortOrder);
    }

    private static class ZimbraHitSortAndIdComparator implements Comparator<ZimbraHit> {
        private final SortBy sort;

        ZimbraHitSortAndIdComparator(SortBy sort){
            this.sort = sort;
        }

        @Override
        public int compare(ZimbraHit lhs, ZimbraHit rhs) {
            try {
                int result = lhs.compareTo(sort, rhs);
                if (result == 0) {
                    int lhsId = lhs.getItemId();
                    if (lhsId <= 0) {
                        lhsId = lhs.getConversationId();
                    }
                    int rhsId = rhs.getItemId();
                    if (rhsId <= 0) {
                        rhsId = rhs.getConversationId();
                    }

                    if (sort.getDirection() == SortBy.Direction.ASC) {
                        result = lhsId - rhsId;
                    } else {
                        result = rhsId - lhsId;
                    }
                }
                return result;
            } catch (ServiceException e) {
                ZimbraLog.search.error("Failed to compare %s and %s", lhs, rhs, e);
                return 0;
            }
        }
    }
    
    /**
     * 
     * @param ascending
     * @param lhs
     * @param rhs
     * @return
     */
    protected static final int compareByReadFlag(boolean ascending, ZimbraHit lhs, ZimbraHit rhs) {
        int retVal = 0;
        try {
            long left = getReadStatus(lhs);
            long right = getReadStatus(rhs);
            long result = right - left;
            if (result > 0)
                retVal = 1;
            else if (result < 0)
                retVal = -1;
            else
                retVal = 0;
        } catch (ServiceException e) {
            ZimbraLog.index.info("Caught ServiceException trying to compare ZimbraHit %s to ZimbraHit %s",
                lhs, rhs);
            ZimbraLog.index.debug(e);
        }
        if (ascending)
            return -1 * retVal;
        else
            return retVal;
    }
    
    /**
     * @param lhs
     * @return
     * @throws ServiceException 
     */
    public static int getReadStatus(ZimbraHit zh) throws ServiceException {
        if (zh instanceof ProxiedHit) {
            try {
                return ((ProxiedHit) zh).getElement().getAttributeInt(MailConstants.A_UNREAD);
            } catch (ServiceException e) {
                // This is  message hit
                Element msgHit = null;
                try {
                    msgHit = ((ProxiedHit) zh).getElement();
                    ZimbraLog.index.debug("Message hit element:%s " , msgHit.toString()) ;
                    String flagValue = msgHit.getAttribute(MailConstants.A_FLAGS);
                    return (flagValue.contains("u")) ? 1 : 0;
                } catch (ServiceException e2) {
                    ZimbraLog.index.info("Error reading unread flag. :%s for hit:%s", e2.getMessage(), msgHit) ;
                    return 1;
                }
            }
        }
        else {
            return zh.getMailItem().isUnread() ? 1 : 0;
        }
    }
}
