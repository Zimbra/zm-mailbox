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
    protected final ZimbraQueryResultsImpl results;
    protected long cachedDate = -1;
    protected long cachedSize = -1;
    protected String cachedName;
    protected String cachedRecipients;
    protected String cachedSubj;
    protected ImapMessage cachedImapMessage;
    protected int cachedModseq = -1;
    protected int cachedParentId = 0;

    public ZimbraHit(ZimbraQueryResultsImpl results, Mailbox mbx) {
        mailbox = mbx;
        this.results = results;
    }

    public abstract int getItemId() throws ServiceException;

    /**
     * Only guaranteed to return a useful value for sort-by-date, or for local hits.
     */
    abstract long getDate() throws ServiceException;


    /**
     * Only guaranteed to return a useful value for local hits.
     */
    abstract long getSize() throws ServiceException;

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
     * Returns the logical "subject" -- by which we mean the subject for sorting purposes. Only guaranteed to work if
     * the search is sort-by-subject.
     */
    abstract String getSubject() throws ServiceException;

    /**
     * Returns the "name" for sorting purposes. Only guaranteed to work if the search is sort-by-name.
     */
    abstract String getName() throws ServiceException;

    /**
     * Returns the recipients for sorting, or an empty string if the item has no value for it.
     */
    abstract String getRecipients() throws ServiceException;

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
                .add("subject", getSubject())
                .add("date", getDate())
                .toString();
        } catch (ServiceException e) {
            return e.toString();
        }
    }

    public Object getSortField(SortBy sortOrder) throws ServiceException {
        switch (sortOrder) {
            case NONE:
                return "";
            case DATE_ASC:
            case DATE_DESC: // 5...4...3...
                return getDate();
            case SUBJ_ASC:
            case SUBJ_DESC:
                return getSubject().toUpperCase();
            case NAME_ASC:
            case NAME_DESC:
            case NAME_LOCALIZED_ASC:
            case NAME_LOCALIZED_DESC:
                return getName().toUpperCase();
            case RCPT_ASC:
            case RCPT_DESC:
                return getRecipients();
            case SIZE_ASC:
            case SIZE_DESC: // 5K...4K...3K...
                return getSize();
            case SCORE_DESC:
                return 1.0F;
            case TASK_DUE_ASC:
            case TASK_DUE_DESC:
            case TASK_STATUS_ASC:
            case TASK_STATUS_DESC:
            case TASK_PERCENT_COMPLETE_ASC:
            case TASK_PERCENT_COMPLETE_DESC:
                throw new IllegalArgumentException("Wrong hit type for hit " + this + " with sort order: " + sortOrder);
            default:
                throw new IllegalArgumentException("Unknown sort order: " + sortOrder);
        }
    }

    public ItemId getParsedItemID() throws ServiceException {
        return new ItemId(mailbox, getItemId());
    }

    final Mailbox getMailbox() {
        return mailbox;
    }

    final ZimbraQueryResultsImpl getResults() {
        return results;
    }

    /**
     * Compare this hit to other using the sort field only
     *
     * @return {@code <0} if "this" is BEFORE other, {@code 0} if EQUAL, {@code >0} if this AFTER other
     */
    int compareBySortField(SortBy sort, ZimbraHit other) throws ServiceException {
        switch (sort) {
            case NONE:
                throw new IllegalArgumentException(SortBy.NONE.name());
            case DATE_ASC:
                return Long.signum(getDate() - other.getDate());
            case DATE_DESC: // 5...4...3...
                return Long.signum(other.getDate() - getDate());
            case SUBJ_ASC:
                 // We to compare(toUpper()) instead of compareIgnoreCase or using a collator because that's the only
                 // method that seems to give us the same results as the sorts from SQL server -- esp the [] characters
                return getSubject().toUpperCase().compareTo(other.getSubject().toUpperCase());
            case SUBJ_DESC:
                // We to compare(toUpper()) instead of compareIgnoreCase or using a collator because that's the only
                // method that seems to give us the same results as the sorts from SQL server -- esp the [] characters
                return other.getSubject().toUpperCase().compareTo(getSubject().toUpperCase());
            case NAME_ASC:
            case NAME_LOCALIZED_ASC:
                // We to compare(toUpper()) instead of compareIgnoreCase or using a collator because that's the only
                // method that seems to give us the same results as the sorts from SQL server -- esp the [] characters
                return getName().toUpperCase().compareTo(other.getName().toUpperCase());
            case NAME_DESC:
            case NAME_LOCALIZED_DESC:
                 // We to compare(toUpper()) instead of compareIgnoreCase or using a collator because that's the only
                 // method that seems to give us the same results as the sorts from SQL server -- esp the [] characters
                return other.getName().toUpperCase().compareTo(getName().toUpperCase());
            case RCPT_ASC:
                return getRecipients().toUpperCase().compareTo(other.getRecipients().toUpperCase());
            case RCPT_DESC:
                return other.getRecipients().toUpperCase().compareTo(getRecipients().toUpperCase());
            case SIZE_ASC:
                return Long.signum(getSize() - other.getSize());
            case SIZE_DESC:
                return Long.signum(other.getSize() - getSize());
            case SCORE_DESC:
                return 0;
            default:
                throw new IllegalArgumentException("Unknown sort order: " + sort);
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

    final void cacheSortField(SortBy sortType, Object sortKey) {
        switch(sortType) {
            case DATE_ASC:
            case DATE_DESC:
                cachedDate = ((Long) sortKey).longValue();
                break;
            case NAME_ASC:
            case NAME_LOCALIZED_ASC:
            case NAME_DESC:
            case NAME_LOCALIZED_DESC:
                cachedName = (String) sortKey;
                break;
            case SUBJ_ASC:
            case SUBJ_DESC:
                cachedSubj = (String) sortKey;
                break;
            case SIZE_ASC:
            case SIZE_DESC:
                cachedSize = ((Long) sortKey).longValue();
                break;
        }
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
                int result = lhs.compareBySortField(sort, rhs);
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
