/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.zimbra.common.util.ZimbraLog;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.SortBy.SortDirection;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.store.file.Volume;

/**
 * Encapsulates the Index for one particular mailbox.
 *
 * @since Jul 26, 2004
 */
public final class MailboxIndex {

    private final LuceneIndex luceneIndex;
    private final int mMailboxId;
    final Mailbox mailbox;
    private Analyzer mAnalyzer = null;

    public MailboxIndex(Mailbox mbox) throws ServiceException {
        mMailboxId = mbox.getId();
        mailbox = mbox;
        Volume indexVol = Volume.getById(mbox.getIndexVolume());
        String idxParentDir = indexVol.getMailboxDir(mMailboxId, Volume.TYPE_INDEX);
        luceneIndex = new LuceneIndex(this, idxParentDir, mMailboxId);
        String analyzerName = mbox.getAccount().getTextAnalyzer();

        if (analyzerName != null) {
            mAnalyzer = ZimbraAnalyzer.getAnalyzer(analyzerName);
        } else {
            mAnalyzer = ZimbraAnalyzer.getInstance();
        }

        ZimbraLog.index.info("index opened mid=%d,dir=%s,analyzer=%s", mMailboxId, luceneIndex, mAnalyzer);
    }


    public static ZimbraQuery compileQuery(SoapProtocol proto, OperationContext octx, Mailbox mbox,
            SearchParams params) throws ServiceException {
        String qs = params.getQueryStr();

        // calendar expansions
        if (params.getCalItemExpandStart() > 0 || params.getCalItemExpandEnd() > 0) {
            StringBuilder toAdd = new StringBuilder();
            toAdd.append('(').append(qs).append(')');
            if (params.getCalItemExpandStart() > 0) {
                toAdd.append(" appt-end:>=").append(params.getCalItemExpandStart());
            }
            if (params.getCalItemExpandEnd() > 0) {
                toAdd.append(" appt-start:<=").append(params.getCalItemExpandEnd());
            }
            qs = toAdd.toString();
            params.setQueryStr(qs);
        }

        return new ZimbraQuery(octx, proto, mbox, params);
    }

    public static ZimbraQueryResults search(ZimbraQuery zq) throws ServiceException {
        SearchParams params = zq.getParams();
        String qs = params.getQueryStr();
        ZimbraLog.index_search.debug("query: %s",  qs);

        // handle special-case Task-only sorts: convert them to a "normal sort"
        //     and then re-sort them at the end
        // FIXME - this hack (converting the sort) should be able to go away w/ the new SortBy
        //         implementation, if the lower-level code was modified to use the SortBy.Criterion
        //         and SortBy.Direction data (instead of switching on the SortBy itself)
        //         We still will need this switch so that we can wrap the
        //         results in the ReSortingQueryResults
        boolean isTaskSort = false;
        boolean isLocalizedSort = false;
        SortBy originalSort = params.getSortBy();
        switch (originalSort.getType()) {
            case TASK_DUE_ASCENDING:
                isTaskSort = true;
                params.setSortBy(SortBy.DATE_DESCENDING);
                break;
            case TASK_DUE_DESCENDING:
                isTaskSort = true;
                params.setSortBy(SortBy.DATE_DESCENDING);
                break;
            case TASK_STATUS_ASCENDING:
                isTaskSort = true;
                params.setSortBy(SortBy.DATE_DESCENDING);
                break;
            case TASK_STATUS_DESCENDING:
                isTaskSort = true;
                params.setSortBy(SortBy.DATE_DESCENDING);
                break;
            case TASK_PERCENT_COMPLETE_ASCENDING:
                isTaskSort = true;
                params.setSortBy(SortBy.DATE_DESCENDING);
                break;
            case TASK_PERCENT_COMPLETE_DESCENDING:
                isTaskSort = true;
                params.setSortBy(SortBy.DATE_DESCENDING);
                break;
            case NAME_LOCALIZED_ASCENDING:
            case NAME_LOCALIZED_DESCENDING:
                isLocalizedSort = true;
                break;
        }

        if (ZimbraLog.searchstats.isDebugEnabled()) {
            int textCount = zq.countTextOperations();
            ZimbraLog.searchstats.debug("Executing search with [" + textCount + "] text parts");
        }

        try {
            ZimbraQueryResults results = zq.execute();

            if (isTaskSort) {
                results = new ReSortingQueryResults(results, originalSort, null);
            }
            if (isLocalizedSort) {
                results = new ReSortingQueryResults(results, originalSort, params);
            }
            return results;
        } catch (ServiceException e) {
            zq.doneWithQuery();
            throw e;
        } catch (OutOfMemoryError e) {
            // DON'T try to cleanup here, we're going to hard shutdown!!
            throw e;
        } catch (Throwable t) { // OOME handled by above
            zq.doneWithQuery();
            throw ServiceException.FAILURE("Caught " + t.getMessage(), t);
        }
    }

    /**
     * @see LuceneIndex#getBytesWritten()
     */
    public long getBytesWritten() {
        return luceneIndex.getBytesWritten();
    }

    /**
     * @see LuceneIndex#getBytesRead()
     */
    public long getBytesRead() {
        return luceneIndex.getBytesRead();
    }

    /**
     * @see LuceneIndex#getDomainsForField(String, String, Collection)
     */
    public void getDomainsForField(String fieldName, String regex, Collection<BrowseTerm> collection)
            throws IOException {
        luceneIndex.getDomainsForField(fieldName, regex, collection);
    }

    /**
     * @see LuceneIndex#getAttachments(String, Collection)
     */
    public void getAttachments(String regex, Collection<BrowseTerm> collection) throws IOException {
        luceneIndex.getAttachments(regex, collection);
    }

    /**
     * @see LuceneIndex#getObjects(String, Collection)
     */
    public void getObjects(String regex, Collection<BrowseTerm> collection) throws IOException {
        luceneIndex.getObjects(regex, collection);
    }

    /**
     * A hint to the indexing system that we're doing a 'bulk' write to the index
     * -- writing multiple items to the index.
     * <ul>
     *  <li>Caller MUST hold call {@link #endWrite()} at the end.
     *  <li>Caller MUST the mailbox lock for the duration of the begin/end pair
     * </ul>
     */
    public void beginWrite() throws IOException {
        luceneIndex.beginWrite();
    }

    public void endWrite() throws IOException {
        luceneIndex.endWrite();
    }

    /**
     * Removes from cache.
     */
    public void evict() {
        luceneIndex.evict();
    }

    /**
     * Deletes index documents.
     *
     * @param ids list of Item IDs to delete
     */
    public void deleteDocuments(List<Integer> ids) throws IOException {
        luceneIndex.deleteDocuments(ids);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("id", mMailboxId).toString();
    }

    IndexSearcherRef getIndexSearcherRef(SortBy sortBy) throws IOException {
        IndexSearcherRef ref = luceneIndex.getIndexSearcherRef();
        ref.setSort(toSort(sortBy));
        return ref;
    }

    private Sort toSort(SortBy sortBy) {
        if (sortBy == null || sortBy == SortBy.NONE) {
            return null;
        }

        boolean reverse = false;;
        if (sortBy.getDirection() == SortDirection.DESCENDING) {
            reverse = true;
        }

        int type;
        String field;
        switch (sortBy.getCriterion()) {
        case NAME:
        case NAME_NATURAL_ORDER:
        case SENDER:
            field = LuceneFields.L_SORT_NAME;
            type = SortField.STRING;
            break;
        case SUBJECT:
            field = LuceneFields.L_SORT_SUBJECT;
            type = SortField.STRING;
            break;
        case SIZE:
            field = LuceneFields.L_SORT_SIZE;
            type = SortField.LONG;
            break;
        case DATE:
        default:
            // default to DATE_DESCENDING!
            field = LuceneFields.L_SORT_DATE;
            type = SortField.STRING;
            reverse = true;;
            break;
        }

        return new Sort(new SortField(field, type, reverse));
    }

    LuceneIndex getLuceneIndex() {
        return luceneIndex;
    }

    /**
     * @see LuceneIndex#startup()
     */
    public static void startup() {
        if (DebugConfig.disableIndexing) {
            return;
        }
        LuceneIndex.startup();
    }

    /**
     * @see LuceneIndex#shutdown()
     */
    public static void shutdown() {
        if (DebugConfig.disableIndexing) {
            return;
        }
        LuceneIndex.shutdown();
    }

    /**
     * Load the {@link Analyzer} for this index, using the default Zimbra
     * analyzer or a custom user-provided analyzer specified by
     * {@link Provisioning#A_zimbraTextAnalyzer}.
     */
    public void initAnalyzer(Mailbox mbox) throws ServiceException {
        // per bug 11052, must always lock the Mailbox before the MailboxIndex, and since
        // mbox.getAccount() is synchronized, we must lock here.
        synchronized (mbox) {
            synchronized (getLock()) {
                String analyzerName = mbox.getAccount().getTextAnalyzer();

                if (analyzerName != null) {
                    mAnalyzer = ZimbraAnalyzer.getAnalyzer(analyzerName);
                } else {
                    mAnalyzer = ZimbraAnalyzer.getInstance();
                }
            }
        }
    }

    public Analyzer getAnalyzer() {
        synchronized (getLock()) {
            return mAnalyzer;
        }
    }

    // Index Search Results
    public static final String GROUP_BY_CONVERSATION = "conversation";
    public static final String GROUP_BY_MESSAGE      = "message";
    public static final String GROUP_BY_NONE         = "none";

    public static final String SEARCH_FOR_APPOINTMENTS = "appointment";
    public static final String SEARCH_FOR_CHATS = "chat";
    public static final String SEARCH_FOR_CONTACTS = "contact";
    public static final String SEARCH_FOR_CONVERSATIONS = "conversation";
    public static final String SEARCH_FOR_DOCUMENTS = "document";
    public static final String SEARCH_FOR_BRIEFCASE = "briefcase";
    public static final String SEARCH_FOR_MESSAGES = "message";
    public static final String SEARCH_FOR_NOTES = "note";
    public static final String SEARCH_FOR_TAGS = "tag";
    public static final String SEARCH_FOR_TASKS = "task";
    public static final String SEARCH_FOR_WIKI = "wiki";

    public static final String SEARCH_FOR_EVERYTHING =
        SEARCH_FOR_APPOINTMENTS + ',' + SEARCH_FOR_CONTACTS + ',' +
        SEARCH_FOR_DOCUMENTS + ',' + SEARCH_FOR_BRIEFCASE + ',' +
        SEARCH_FOR_MESSAGES + ',' + SEARCH_FOR_NOTES + ',' +
        SEARCH_FOR_TASKS + ',' + SEARCH_FOR_WIKI;

    private static final Map<String, Byte> TYPE_MAP = new ImmutableMap.Builder<String, Byte>()
        .put(GROUP_BY_NONE, (byte) 0)
        .put(SEARCH_FOR_CONVERSATIONS, MailItem.TYPE_CONVERSATION)
        .put(SEARCH_FOR_MESSAGES, MailItem.TYPE_MESSAGE)
        .put(SEARCH_FOR_CHATS, MailItem.TYPE_CHAT)
        .put(SEARCH_FOR_CONTACTS, MailItem.TYPE_CONTACT)
        .put(SEARCH_FOR_DOCUMENTS, MailItem.TYPE_DOCUMENT)
        .put(SEARCH_FOR_BRIEFCASE, MailItem.TYPE_DOCUMENT)
        .put(SEARCH_FOR_APPOINTMENTS, MailItem.TYPE_APPOINTMENT)
        .put(SEARCH_FOR_NOTES, MailItem.TYPE_NOTE)
        .put(SEARCH_FOR_TAGS, MailItem.TYPE_TAG)
        .put(SEARCH_FOR_TASKS, MailItem.TYPE_TASK)
        .put(SEARCH_FOR_WIKI, MailItem.TYPE_WIKI)
        .build();

    public static Set<Byte> parseTypes(String types) throws ServiceException {
        Set<Byte> result = new HashSet<Byte>();
        for (String type : Splitter.on(',').trimResults().split(types)) {
            Byte b = TYPE_MAP.get(type);
            if (b == null) {
                throw ServiceException.INVALID_REQUEST("unknown groupBy: " + type, null);
            }
            result.add(b);
        }
        return result;
    }

    /**
     * @see LuceneIndex#deleteIndex()
     */
    public void deleteIndex() throws IOException {
        luceneIndex.deleteIndex();
    }

    public void indexMailItem(Mailbox mbox, boolean deleteFirst, List<IndexDocument> docs, MailItem item)
            throws ServiceException {

        if (docs == null || docs.isEmpty()) {
            return;
        }

        initAnalyzer(mbox);
        synchronized (mailbox) {
            try {
                luceneIndex.addDocument(item, docs, deleteFirst);
            } catch (IOException e) {
                throw ServiceException.FAILURE("Failed to index id=" + item.getId(), e);
            }
        }
    }

    /**
     * @see LuceneIndex#expandWildcardToken(Collection, String, String, int)
     */
    public boolean expandWildcardToken(Collection<String> toRet, String field,
            String token, int maxToReturn) throws IOException {
        return luceneIndex.expandWildcardToken(toRet, field, token, maxToReturn);
    }

    final Object getLock() {
        return mailbox;
    }

    int getMailboxId() {
        return mMailboxId;
    }

}
