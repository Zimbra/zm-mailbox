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

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;

/**
 * Result set that does filtering.of the results.
 * <ul>
 *  <li>Supports filtering by the \DELETED tag (enable it by calling setFilterTagDeleted API)
 *  <li>Supports filtering by allowable Task-status
 *      (e.g. only show Completed tasks) see setAllowableTaskStatus(Set<TaskHit.Status>) API)
 *  <li>Supports cursor filtering for proxied hits
 * </ul>
 */
public final class FilteredQueryResults implements ZimbraQueryResults {
    private final ZimbraQueryResults results;
    private final SearchParams searchParams;

    private boolean filterTagDeleted = false;
    private boolean filterTagMuted = false;
    private Set<TaskHit.Status> allowedTaskStatuses = null;

    //enable cursor filtering for remote Contact sorting!!
    private ZimbraHit firstHit = null;
    private ZimbraHit endHit = null;
    private Comparator<ZimbraHit> comp = null;


    FilteredQueryResults(ZimbraQueryResults other, SearchParams params) {
        results = other;
        searchParams = params;

        boolean isContactSearch = false;
        if (searchParams.getTypes() != null && searchParams.getTypes().size() == 1 &&
                searchParams.getTypes().contains(MailItem.Type.CONTACT)) {
            if (results.getSortBy() != null) {
                switch (results.getSortBy()) {
                    case NAME_ASC:
                    case NAME_DESC:
                        isContactSearch = true; //localized sort will be taken care by ReSortingQueryResults!!
                        break;
                    default:
                        break;
                }   
            }
        }
        
        if (isContactSearch && searchParams != null && searchParams.getCursor() != null) {
            if (searchParams.getCursor().getSortValue() != null) {
                firstHit = new ResultsPager.CursorHit(results, searchParams.getCursor().getSortValue(),
                        searchParams.getCursor().getItemId().getId());
            }
            if (searchParams.getCursor().getEndSortValue() != null) {
                endHit = new ResultsPager.CursorHit(results, searchParams.getCursor().getEndSortValue(), 0);
            }
            // get the proper comparator
            comp = searchParams.getSortBy().getHitComparator(searchParams.getLocale());
        }
    }

    /** If set, then this class will filter out all items with the \Deleted tag set. */
    void setFilterTagDeleted(boolean value) {
        filterTagDeleted = value;
    }

    /** If set, then this class will filter out all items with the \Muted tag set. */
    void setFilterTagMuted(boolean value) {
        filterTagMuted = value;
    }

    void setAllowedTaskStatuses(Set<TaskHit.Status> allowed) {
        allowedTaskStatuses = allowed;
    }

    @Override
    public long getCursorOffset() {
        return results.getCursorOffset();
    }

    @Override
    public void close() throws IOException {
        results.close();
    }

    @Override
    public List<QueryInfo> getResultInfo() {
        return results.getResultInfo();
    }

    @Override
    public SortBy getSortBy() {
        return results.getSortBy();
    }

    @Override
    public void resetIterator() throws ServiceException {
        results.resetIterator();
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
        if (toRet != null) {
            results.getNext(); // skip the current hit
        }
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
        if (allowedTaskStatuses != null) {
            if (hit instanceof TaskHit) {
                if (!allowedTaskStatuses.contains(((TaskHit)hit).getStatus())) {
                    return true;
                }
            }
        }

        if (filterTagDeleted && hit.isLocal()) {
            MailItem item = hit.getMailItem();
            if ((item.getFlagBitmask() & Flag.BITMASK_DELETED) != 0) {
                return true; // filter it
            }
        }

        if (filterTagMuted && hit.isLocal()) {
            MailItem item = hit.getMailItem();
            if ((item.getFlagBitmask() & Flag.BITMASK_MUTED) != 0) {
                return true; // filter it
            }
        }

        if (hit instanceof ProxiedHit) {
            if (firstHit != null && comp != null && comp.compare(hit, firstHit) < 0) {
                return true;
            }
            if (endHit != null && comp != null && comp.compare(hit, endHit) >= 0) {
                return true;
            }
        }

        return false; // if we got here, include it
    }


    @Override
    public ZimbraHit peekNext() throws ServiceException {
        ZimbraHit cur = results.peekNext();
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

            if (!filterThisHit) {
                return cur;
            }
            results.getNext(); // skip next hit
            cur = results.peekNext();
        }
        return null;
    }

}
