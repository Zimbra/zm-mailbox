/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

package com.zimbra.cs.index;

import java.util.Comparator;

import com.google.common.base.Objects;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.imap.ImapMessage;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.util.ItemId;

/**
 * Base class for a search "hit". Generally one iterates over a {@link ZimbraQueryResults}
 * to get the hits for a given query.
 *
 * @since Oct 15, 2004
 */
public abstract class ZimbraHit {

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
                throw new IllegalArgumentException(value.toString());
            }
        } else {
            throw new IllegalArgumentException(value.toString());
        }
    }

    private Integer toInteger(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return new Integer((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(value.toString());
            }
        } else {
            throw new IllegalArgumentException(value.toString());
        }
    }

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
            return Objects.toStringHelper(this)
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
                return Long.signum((Long) sortValue - (Long) other.sortValue);
            case DATE_DESC:
            case SIZE_DESC:
                return Long.signum((Long) other.sortValue - (Long) sortValue);
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

    public int getModifiedSequence() throws ServiceException {
        if (cachedModseq < 0) {
            MailItem item = getMailItem();
            cachedModseq = item != null ? item.getModifiedSequence() : 0;
        }
        return cachedModseq;
    }

    public int getParentId() throws ServiceException {
        if (cachedParentId == 0) {
            MailItem item = getMailItem();
            cachedParentId = item != null ? item.getParentId() : -1;
        }
        return cachedParentId;
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
}
