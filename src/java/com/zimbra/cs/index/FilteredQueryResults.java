/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2011 Zimbra, Inc.
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

import java.util.List;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;

/**
 * Result set that does filtering.of the results.
 * <ul>
 *   <li>Supports filtering by the \DELETED tag
 *       (enable it by calling setFilterTagDeleted API)</li>
 *   <li>Supports filtering by allowable Task-status
 *        (e.g. only show Completed tasks) see
 *        setAllowableTaskStatus(Set<TaskHit.Status>) API)</li>
 * </ul>
 */
public class FilteredQueryResults implements ZimbraQueryResults {
    ZimbraQueryResults mResults;

    private boolean mFilterTagDeleted = false;
    private Set<TaskHit.Status> mAllowedTaskStatuses = null;

    FilteredQueryResults(ZimbraQueryResults other) {
        mResults = other;
    }

    /**
     * If set, then this class will filter out all messages with the
     * \DELETED tag set
     *
     * @param truthiness
     */
    void setFilterTagDeleted(boolean truthiness) {
        mFilterTagDeleted = truthiness;
    }

    void setAllowedTaskStatuses(Set<TaskHit.Status> allowed) {
        mAllowedTaskStatuses = allowed;
    }

    @Override
    public void doneWithSearchResults() throws ServiceException {
        mResults.doneWithSearchResults();
    }

    @Override
    public ZimbraHit getFirstHit() throws ServiceException {
        resetIterator();
        return getNext();
    }

    @Override
    public List<QueryInfo> getResultInfo() {
        return mResults.getResultInfo();
    }

    @Override
    public SortBy getSortBy() {
        return mResults.getSortBy();
    }

    @Override
    public void resetIterator() throws ServiceException {
        mResults.resetIterator();
    }

    @Override
    public ZimbraHit skipToHit(int hitNo) throws ServiceException {
        resetIterator();
        for (int i = 0; i < hitNo; i++) {
            if (!hasNext()) {
                return null;
            }
            getNext();
        }
        return getNext();
    }

    @Override
    public ZimbraHit getNext() throws ServiceException {
        ZimbraHit toRet = peekNext();
        if (toRet != null)
            mResults.getNext(); // skip the current hit
        return toRet;
    }

    @Override
    public boolean hasNext() throws ServiceException {
        return peekNext() != null;
    }

    /**
     * @return TRUE if the passed-in hit should be filtered (removed) from
     * the result set
     */
    private boolean shouldFilter(ZimbraHit hit) throws ServiceException {
        if (mAllowedTaskStatuses != null) {
            if (hit instanceof TaskHit) {
                if (!mAllowedTaskStatuses.contains(((TaskHit)hit).getStatus()))
                    return true;
            }
        }

        if (mFilterTagDeleted) {
            if (hit.isLocal()) {
                MailItem item = hit.getMailItem();
                if (item == null) {
                    System.out.println("NULL!");
                }
                if ((item.getFlagBitmask() & Flag.BITMASK_DELETED) != 0)
                    return true; // filter it
            }
        }

        return false; // if we got here, include it
    }

    @Override
    public ZimbraHit peekNext() throws ServiceException {
        ZimbraHit cur = mResults.peekNext();
        while (cur != null) {
            boolean filterThisHit = false;
            if (cur instanceof ConversationHit) {
                ConversationHit ch = (ConversationHit)cur;
                for (MessageHit mh : ch.getMessageHits()) {
                    filterThisHit = shouldFilter(mh);
                    if (!filterThisHit)
                        break; // found at least one valid message hit in this conv
                }
            } else {
                filterThisHit = shouldFilter(cur);
            }

            if (!filterThisHit)
                return cur;

            mResults.getNext(); // skip next hit
            cur = mResults.peekNext();
        }
        return null;
    }


}
