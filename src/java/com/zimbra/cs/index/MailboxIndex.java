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
import java.util.List;
import com.zimbra.common.util.ZimbraLog;
import org.apache.lucene.analysis.Analyzer;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.IndexHelper;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.util.SyncToken;
import com.zimbra.cs.store.file.Volume;

/**
 * Encapsulates the Index for one particular mailbox.
 *
 * @since Jul 26, 2004
 */
public final class MailboxIndex {

    private final LuceneIndex luceneIndex;
    private final long mMailboxId;
    private final Mailbox mMailbox;
    private Analyzer mAnalyzer = null;

    public MailboxIndex(Mailbox mbox) throws ServiceException {
        mMailboxId = mbox.getId();
        mMailbox = mbox;
        Volume indexVol = Volume.getById(mbox.getIndexVolume());
        String idxParentDir = indexVol.getMailboxDir(mMailboxId, Volume.TYPE_INDEX);
        luceneIndex = new LuceneIndex(this, idxParentDir, mMailboxId);
        String analyzerName = mbox.getAccount().getAttr(Provisioning.A_zimbraTextAnalyzer, null);

        if (analyzerName != null) {
            mAnalyzer = ZimbraAnalyzer.getAnalyzer(analyzerName);
        } else {
            mAnalyzer = ZimbraAnalyzer.getDefaultAnalyzer();
        }

        ZimbraLog.index.info("Initialized Index for mailbox " + mMailboxId +
                " directory: " + luceneIndex + " Analyzer=" + mAnalyzer);
    }

    /**
     * Primary search API.
     *
     * @param proto
     * @param octxt
     * @param mbox
     * @param params
     * @param textIndexOutOfSync if set, then this API will throw {@link MailServiceException.TEXT_INDEX_OUT_OF_SYNC} if and only if the search
     *        contains a text part.  Searches without a text part can be run even if the text index is behind.
     * @return
     * @throws IOException
     * @throws ServiceException
     */
    public static ZimbraQueryResults search(SoapProtocol proto,
            OperationContext octxt, Mailbox mbox, SearchParams params,
            boolean textIndexOutOfSync) throws IOException, ServiceException {

        if (ZimbraLog.index_search.isDebugEnabled()) {
            ZimbraLog.index_search.debug("SearchRequest: " + params.getQueryStr());
        }

        String qs = params.getQueryStr();

        //
        // calendar expansions
        //
        if ((params.getCalItemExpandStart() > 0) || (params.getCalItemExpandEnd() > 0)) {
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
        }

        ZimbraQuery zq = new ZimbraQuery(octxt, proto, mbox, params);

        if (zq.countSearchTextOperations() > 0 && textIndexOutOfSync) {
            throw MailServiceException.TEXT_INDEX_OUT_OF_SYNC();
        }

        if (ZimbraLog.searchstats.isDebugEnabled()) {
            int textCount = zq.countSearchTextOperations();
            ZimbraLog.searchstats.debug("Executing search with [" + textCount + "] text parts");
        }

        try {
            ZimbraQueryResults results = zq.execute(/*octxt, proto*/);

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
     * This API should **ONLY** be used by the {@link IndexHelper} API. Don't
     * call this API directly.
     */
    public int getBatchedIndexingCount() {
        try {
            return mMailbox.getAccount().getIntAttr(Provisioning.A_zimbraBatchedIndexingSize, 0);
        } catch (ServiceException e) {
            ZimbraLog.index.debug("Eating ServiceException trying to lookup BatchedIndexSize", e);
        }
        return 0;
    }

    public boolean useBatchedIndexing() throws ServiceException {
        return mMailbox.getAccount().getIntAttr(Provisioning.A_zimbraBatchedIndexingSize, 0) > 0;
    }

    /**
     * @see LuceneIndex#generateIndexId(int)
     */
    public String generateIndexId(int itemId) {
        return luceneIndex.generateIndexId(itemId);
    }

    /**
     * @see LuceneIndex#getDomainsForField(String, String, Collection)
     */
    public void getDomainsForField(String fieldName, String regex,
            Collection<BrowseTerm> collection) throws IOException {
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
     *  <li>Caller MUST hold call endWriteOperation() at the end.
     *  <li>Caller MUST the mailbox lock for the duration of the begin/end pair
     * </ul>
     *
     * @see LuceneIndex#beginWriteOperation()
     */
    public void beginWriteOperation() throws IOException {
        luceneIndex.beginWriteOperation();
    }

    /**
     * @see LuceneIndex#endWriteOperation()
     */
    public void endWriteOperation() {
        luceneIndex.endWriteOperation();
    }

    /**
     * Force all outstanding index writes to go through.
     * <p>
     * This API should be called when the system detects that it has free time.
     *
     * @see LuceneIndex#flush()
     */
    public void flush() {
        luceneIndex.flush();
    }

    /**
     * @see LuceneIndex#deleteDocuments(List)
     * @param itemIds array of itemIds to be deleted
     * @return an array of itemIds which HAVE BEEN PROCESSED.
     *  If {@code returned.length == itemIds.length} then you can assume the
     *  operation was completely successful.
     * @throws IOException on index open failure, nothing processed.
     */
    public List<String> deleteDocuments(List<String> itemIds) throws IOException {
        return luceneIndex.deleteDocuments(itemIds);
    }

    @Override
    public String toString() {
        return "MailboxIndex(" + mMailboxId + ")";
    }

    IndexSearcherRef getIndexSearcherRef(SortBy sort) throws IOException {
        IndexSearcherRef toRet = luceneIndex.getIndexSearcherRef();
        toRet.setSort(luceneIndex.getSort(sort));
        return toRet;
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
     * @see LuceneIndex#flushAllWriters()
     */
    public static void flushAllWriters() {
        if (DebugConfig.disableIndexing) {
            return;
        }
        LuceneIndex.flushAllWriters();
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
                String analyzerName = mbox.getAccount().getAttr(Provisioning.A_zimbraTextAnalyzer, null);

                if (analyzerName != null) {
                    mAnalyzer = ZimbraAnalyzer.getAnalyzer(analyzerName);
                } else {
                    mAnalyzer = ZimbraAnalyzer.getDefaultAnalyzer();
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

    public static byte[] parseTypesString(String groupBy) throws ServiceException {
        String[] strs = groupBy.split("\\s*,\\s*");

        byte[] types = new byte[strs.length];
        for (int i = 0; i < strs.length; i++) {
            if (SEARCH_FOR_CONVERSATIONS.equals(strs[i])) {
                types[i] = MailItem.TYPE_CONVERSATION;
            } else if (SEARCH_FOR_MESSAGES.equals(strs[i])) {
                types[i] = MailItem.TYPE_MESSAGE;
            } else if (GROUP_BY_NONE.equals(strs[i])) {
                types[i] = 0;
            } else if (SEARCH_FOR_CHATS.equals(strs[i])) {
                types[i] = MailItem.TYPE_CHAT;
            } else if (SEARCH_FOR_CONTACTS.equals(strs[i])) {
                types[i] = MailItem.TYPE_CONTACT;
            } else if (SEARCH_FOR_DOCUMENTS.equals(strs[i])) {
                types[i] = MailItem.TYPE_DOCUMENT;
            } else if (SEARCH_FOR_BRIEFCASE.equals(strs[i])) {
                types[i] = MailItem.TYPE_DOCUMENT;
            } else if (SEARCH_FOR_APPOINTMENTS.equals(strs[i])) {
                types[i] = MailItem.TYPE_APPOINTMENT;
            } else if (SEARCH_FOR_NOTES.equals(strs[i])) {
                types[i] = MailItem.TYPE_NOTE;
            } else if (SEARCH_FOR_TAGS.equals(strs[i])) {
                types[i] = MailItem.TYPE_TAG;
            } else if (SEARCH_FOR_TASKS.equals(strs[i])) {
                types[i] = MailItem.TYPE_TASK;
            } else if (SEARCH_FOR_WIKI.equals(strs[i])) {
                types[i] = MailItem.TYPE_WIKI;
            } else {
                throw ServiceException.INVALID_REQUEST(
                        "unknown groupBy: " + strs[i], null);
            }
        }

        return types;
    }

    /**
     * @see LuceneIndex#deleteIndex()
     */
    public void deleteIndex() throws IOException {
        luceneIndex.deleteIndex();
    }

    /**
     * @param modContent passed-in, can't use the one from the MailItem because
     *  it could potentially change underneath us and we need to guarantee that
     *  we're submitting to the index in strict mod-content-order. Note that a
     *  modContent of -1 means that this is an out-of-sequence index add (IE a
     *  reindex of specific items or types).  Out-of-index adds SHOULD NOT BE
     *  TRACKED -- do not call indexingCompleted for them.
     */
    public void indexMailItem(Mailbox mbox, boolean deleteFirst,
            List<IndexDocument> docList, MailItem mi, int modContent)
        throws ServiceException {

        initAnalyzer(mbox);
        synchronized(getLock()) {
            String indexId = mi.getIndexId();
            try {
                if (docList != null) {
                    IndexDocument[] docs = new IndexDocument[docList.size()];
                    docs = docList.toArray(docs);
                    luceneIndex.addDocument(docs, mi.getId(), indexId,
                            modContent, mi.getDate(), mi.getSize(),
                            mi.getSortSubject(), mi.getSortSender(), deleteFirst);
                }
            } catch (IOException e) {
                throw ServiceException.FAILURE("indexMailItem caught IOException", e);
            }
        }
    }

    void indexingCompleted(int count, SyncToken highestToken, boolean succeeded) {
        if (count > 0) {
            if (ZimbraLog.index_add.isDebugEnabled()) {
                ZimbraLog.index_add.debug("indexingCompleted(" + count + "," +
                        highestToken + "," + (succeeded ? "SUCCEEDED)" : "FAILED)"));
            }

            mMailbox.indexingCompleted(count, highestToken, succeeded);
        }
    }

    /**
     * @see LuceneIndex#expandWildcardToken(Collection, String, String, int)
     */
    public boolean expandWildcardToken(Collection<String> toRet, String field,
            String token, int maxToReturn) throws ServiceException {
        return luceneIndex.expandWildcardToken(toRet, field, token, maxToReturn);
    }

    /**
     * @see LuceneIndex#suggestSpelling(String, String)
     */
    List<SpellSuggestQueryInfo.Suggestion> suggestSpelling(String field,
            String token) throws ServiceException {
        return luceneIndex.suggestSpelling(field, token);
    }

    final Object getLock() {
        return mMailbox;
    }

    long getMailboxId() {
        return mMailboxId;
    }

}
