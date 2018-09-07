/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.mailbox;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.regex.Pattern;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.zimbra.common.mailbox.MailboxLock;
import com.zimbra.common.mime.InternetAddress;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.AccessBoundedRegex;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbSearch;
import com.zimbra.cs.db.DbTag;
import com.zimbra.cs.index.BrowseTerm;
import com.zimbra.cs.index.DbSearchConstraints;
import com.zimbra.cs.index.IndexPendingDeleteException;
import com.zimbra.cs.index.IndexStore;
import com.zimbra.cs.index.Indexer;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.LuceneQueryOperation.LuceneResultsChunk;
import com.zimbra.cs.index.ReSortingQueryResults;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraIndexReader.TermFieldEnumeration;
import com.zimbra.cs.index.ZimbraIndexSearcher;
import com.zimbra.cs.index.ZimbraQuery;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.index.queue.AddToIndexTaskLocator;
import com.zimbra.cs.index.queue.DeleteFromIndexTaskLocator;
import com.zimbra.cs.index.queue.IndexingQueueAdapter;
import com.zimbra.cs.mailbox.MailItem.TemporaryIndexingException;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.util.Zimbra;

/**
 * Index related mailbox operations.
 *
 * @author tim
 * @author ysasaki
 */
public final class MailboxIndex {
    // Only one thread may run index at a time.
    final Semaphore indexLock = new Semaphore(1);
    final Mailbox mailbox;
    IndexStore indexStore;

    MailboxIndex(Mailbox mbox) {
        mailbox = mbox;
    }

    void open() throws ServiceException {
        indexStore = IndexStore.getFactory().getIndexStore(mailbox.getAccountId());
    }

    public final IndexStore getIndexStore() {
        assert(indexStore != null);
        return indexStore;
    }

    /**
     * This is the preferred form of the API call.
     *
     * In order to avoid deadlock, callers MUST NOT be holding the Mailbox lock when calling this API.
     *
     * You MUST call {@link ZimbraQueryResults#doneWithSearchResults()} when you are done with the search results,
     * otherwise resources will be leaked.
     *
     * @param octxt Operation Context
     * @param params Search Parameters
     * @return search result
     */
    public ZimbraQueryResults search(SoapProtocol proto, OperationContext octx, SearchParams params)
            throws ServiceException {
        assert(octx != null);

        ZimbraQuery query = new ZimbraQuery(octx, proto, mailbox, params);
        return search(query);
    }

    public ZimbraQueryResults search(OperationContext octxt, String queryString, Set<MailItem.Type> types,
                                     SortBy sortBy, int chunkSize, boolean inDumpster) throws ServiceException {
        SearchParams params = new SearchParams();
        params.setQueryString(queryString);
        params.setTimeZone(null);
        params.setLocale(null);
        params.setTypes(types);
        params.setSortBy(sortBy);
        params.setChunkSize(chunkSize);
        params.setPrefetch(true);
        params.setFetchMode(SearchParams.Fetch.NORMAL);
        params.setInDumpster(inDumpster);
        return search(SoapProtocol.Soap12, octxt, params);
    }

    public ZimbraQueryResults search(OperationContext octxt, SearchParams params) throws ServiceException {
        return search(SoapProtocol.Soap12, octxt, params);
    }

    public ZimbraQueryResults search(OperationContext octxt, String queryString, Set<MailItem.Type> types,
                                     SortBy sortBy, int chunkSize) throws ServiceException {
        return search(octxt, queryString, types, sortBy, chunkSize, false);
    }

    private ZimbraQueryResults search(ZimbraQuery zq) throws ServiceException {
        SearchParams params = zq.getParams();
        ZimbraLog.search.debug("query: %s", params.getQueryString());
        ZimbraLog.searchstat.debug("query: %s", zq.toSanitizedtring());

        // handle special-case Task-only sorts: convert them to a "normal sort" and then re-sort them at the end
        // TODO: this hack (converting the sort) should be able to go away w/ the new SortBy implementation, if the
        // lower-level code was modified to use the SortBy.Criterion and SortBy.Direction data (instead of switching on
        // the SortBy itself). We still will need this switch so that we can wrap the results in ReSortingQueryResults.
        boolean isTaskSort = false;
        boolean isLocalizedSort = false;
        boolean isReadSort = false;
        SortBy originalSort = params.getSortBy();
        switch (originalSort) {
            case TASK_DUE_ASC:
                isTaskSort = true;
                params.setSortBy(SortBy.DATE_DESC);
                break;
            case TASK_DUE_DESC:
                isTaskSort = true;
                params.setSortBy(SortBy.DATE_DESC);
                break;
            case TASK_STATUS_ASC:
                isTaskSort = true;
                params.setSortBy(SortBy.DATE_DESC);
                break;
            case TASK_STATUS_DESC:
                isTaskSort = true;
                params.setSortBy(SortBy.DATE_DESC);
                break;
            case TASK_PERCENT_COMPLETE_ASC:
                isTaskSort = true;
                params.setSortBy(SortBy.DATE_DESC);
                break;
            case TASK_PERCENT_COMPLETE_DESC:
                isTaskSort = true;
                params.setSortBy(SortBy.DATE_DESC);
                break;
            case READ_ASC:
                isReadSort = true;
                params.setSortBy(SortBy.READ_ASC);
                break;
            case READ_DESC:
                isReadSort = true;
                params.setSortBy(SortBy.DATE_DESC);
                break;
            case NAME_LOCALIZED_ASC:
            case NAME_LOCALIZED_DESC:
                isLocalizedSort = true;
                break;
        }

        ZimbraQueryResults results = zq.execute();
        if (isTaskSort || isReadSort) {
            results = new ReSortingQueryResults(results, originalSort, null);
        }
        if (isLocalizedSort) {
            results = new ReSortingQueryResults(results, originalSort, params);
        }
        return results;
    }

    /**
     * Returns true if any of the specified email addresses exists in contacts, otherwise false.
     * @throws ServiceException
     */
    public boolean existsInContacts(Collection<InternetAddress> addrs) throws IOException, ServiceException {
        if (addrs.isEmpty()) {
            return false;
        }
        try (ZimbraIndexSearcher searcher = indexStore.openSearcher()) {
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            for (InternetAddress addr : addrs) {
                if (!Strings.isNullOrEmpty(addr.getAddress())) {
                    String lcAddr = addr.getAddress().toLowerCase();
                    builder.add(new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, lcAddr)), BooleanClause.Occur.SHOULD);
                }
            }
            BooleanQuery bq = builder.build();
            return searcher.search(bq, 1).getTotalHits() > 0;
        }
    }

    /**
     * Returns the maximum number of items to be batched in a single indexing pass. If a search comes in that requires
     * use of the index, all deferred unindexed items are immediately indexed regardless of batch size. If this number
     * is {@code 0}, all items are indexed immediately when they are added.
     */
    public int getBatchThreshold() {
        return 0; // disable batch indexing for non Lucene index stores
    }

    void evict() {
        indexStore.evict();
    }

    public void deleteIndex() throws IOException, ServiceException {
        if (isReIndexInProgress()) {
            abortReIndex();
        }
        indexStore.deleteIndex();
    }

    public synchronized void startReIndex(OperationContext ctxt) throws ServiceException {
        if(isReIndexInProgress()) {
            throw ServiceException.ALREADY_IN_PROGRESS("One or more active re-indexing tasks are already running for this mailbox. You need to cancel current tasks or wait for them to finish before starting a full reindex.");
        }
        startReIndexByType(EnumSet.of(MailItem.Type.APPOINTMENT, MailItem.Type.INVITE, MailItem.Type.CHAT,
                MailItem.Type.MESSAGE, MailItem.Type.NOTE, MailItem.Type.CONTACT, MailItem.Type.DOCUMENT,
                MailItem.Type.CONTACT, MailItem.Type.TASK), ctxt);
    }

    public void startCompactIndex() throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    /**
     * Add mail items with specified IDs to re-indexing queue
     * @param types
     * @throws ServiceException
     */
    public synchronized boolean startReIndexById(Collection<Integer> itemIds) throws ServiceException {
        boolean success = false;
        //Step 1: get items (does not matter whether we get these from cache or DB at this step
        MailItem[] items = mailbox.getItemById(null, itemIds, MailItem.Type.UNKNOWN_SEARCHABLE);

        //Step 2: set task counters for reporting
        IndexingQueueAdapter queueAdapter = IndexingQueueAdapter.getFactory().getAdapter();
        if(queueAdapter == null) {
            throw ServiceException.FAILURE("Indexing Queue Adapter is not properly configured", null);
        }
        queueAdapter.setTotalMailboxTaskCount(mailbox.getAccountId(), items.length);
        queueAdapter.setSucceededMailboxTaskCount(mailbox.getAccountId(), 0);
        queueAdapter.setFailedMailboxTaskCount(mailbox.getAccountId(), 0);

        if(items.length == 0) {
            //nothing to index
            queueAdapter.setTaskStatus(mailbox.getAccountId(), ReIndexStatus.STATUS_DONE);
            success = true;
        } else {
            queueAdapter.setTaskStatus(mailbox.getAccountId(), ReIndexStatus.STATUS_RUNNING);
            //Step 3: add items to re-indexing queue in batches
            int ix = 0;
            int numAdded = 0;
            int batchSize = Provisioning.getInstance().getLocalServer().getReindexBatchSize();
            if(batchSize > 0) {
                while(ix < items.length) {
                    List<MailItem> batch = new ArrayList<MailItem>();
                    for(int i=0; i<batchSize && ix < items.length; i++, ix++) {
                        batch.add(items[ix]);
                    }
                    success = queue(batch, true);
                    if(!success) {
                        queueAdapter.setTaskStatus(mailbox.getAccountId(), ReIndexStatus.STATUS_QUEUE_FULL);
                        queueAdapter.incrementFailedMailboxTaskCount(mailbox.getAccountId(), items.length-numAdded);
                        ZimbraLog.index.warn("Aborting reindexing for account %s,  because indexing queue is full. Added %d items out of %d.",mailbox.getAccount(),numAdded,items.length);
                        break;
                    }
                    numAdded+=batch.size();
                }
            }
        }
        return success;
    }

    /**
     * Add mail items of specified type to re-indexing queue
     * @param types
     * @throws ServiceException
     */
    public synchronized boolean startReIndexByType(Set<MailItem.Type> types, OperationContext ctxt) throws ServiceException {
        boolean success = false;
        //Step 1: get items (does not matter whether we get these from cache or DB at this step)
        List<MailItem> items = new ArrayList<MailItem>();
        for(MailItem.Type type : types) {
            List<MailItem> itemsOfType = mailbox.getItemList(ctxt, type);
            if(itemsOfType != null && !itemsOfType.isEmpty()) {
                items.addAll(itemsOfType);
            }
        }
        IndexingQueueAdapter queueAdapter = IndexingQueueAdapter.getFactory().getAdapter();
        if(queueAdapter == null) {
            throw ServiceException.FAILURE("Indexing Queue Adapter is not properly configured", null);
        }

        //Step 2: set task counters for reporting
        queueAdapter.setTotalMailboxTaskCount(mailbox.getAccountId(), items.size());
        queueAdapter.setSucceededMailboxTaskCount(mailbox.getAccountId(), 0);
        queueAdapter.setFailedMailboxTaskCount(mailbox.getAccountId(), 0);

        if(items.isEmpty()) {
            queueAdapter.setTaskStatus(mailbox.getAccountId(), ReIndexStatus.STATUS_DONE);
            success = true;
        } else {
            queueAdapter.setTaskStatus(mailbox.getAccountId(), ReIndexStatus.STATUS_RUNNING);

            //Step 3: add items to re-indexing queue in batches
            int ix = 0;
            int numAdded = 0;
            int batchSize = 1000; //Provisioning.getInstance().getLocalServer().getReindexBatchSize();
            if(batchSize > 0) {
                while(ix < items.size()) {
                    List<MailItem> batch = new ArrayList<MailItem>();
                    for(int i=0;i<batchSize && ix < items.size(); i++, ix++) {
                        batch.add(items.get(ix));
                    }
                    success = queue(batch, true);
                    if(!success) {
                        queueAdapter.setTaskStatus(mailbox.getAccountId(), ReIndexStatus.STATUS_QUEUE_FULL);
                        queueAdapter.incrementFailedMailboxTaskCount(mailbox.getAccountId(), items.size()-numAdded);
                        ZimbraLog.index.warn("Aborting reindexing for account %s,  because indexing queue is full. Added %d items out of %d.",mailbox.getAccount(),numAdded,items.size());
                        break;
                    }
                    numAdded+=batch.size();
                }
            }
        }
        return success;
    }

    public synchronized ReIndexStatus abortReIndex() throws ServiceException {
        IndexingQueueAdapter queueAdapter = IndexingQueueAdapter.getFactory().getAdapter();
        if(queueAdapter == null) {
            throw ServiceException.FAILURE("Indexing Queue Adapter is not properly configured", null);
        }
        queueAdapter.setTaskStatus(mailbox.getAccountId(), ReIndexStatus.STATUS_ABORTED);

        return new ReIndexStatus(queueAdapter.getTotalMailboxTaskCount(mailbox.getAccountId()),
                queueAdapter.getSucceededMailboxTaskCount(mailbox.getAccountId()),
                queueAdapter.getFailedMailboxTaskCount(mailbox.getAccountId()),
                ReIndexStatus.STATUS_ABORTED);
    }


    public boolean verify(PrintStream out) throws ServiceException {
        indexLock.acquireUninterruptibly(); // make sure no writers are opened
        try {
            return indexStore.verify(out);
        } catch (IOException e) {
            throw ServiceException.FAILURE("Failed to verify index", e);
        } finally {
            indexLock.release();
        }
    }

    /**
     * Migrate to mailbox version 1.5.
     */
    @SuppressWarnings("deprecation")
    void indexAllDeferredFlagItems() throws ServiceException {
        try (final MailboxLock l = mailbox.getWriteLockAndLockIt()) {
            try (final Mailbox.MailboxTransaction t =
                    mailbox.mailboxWriteTransaction("indexAllDeferredFlagItems", null)) {
                DbSearchConstraints.Leaf c = new DbSearchConstraints.Leaf();
                c.tags.add(mailbox.getFlagById(Flag.ID_INDEXING_DEFERRED));
                List<DbSearch.Result> list = new DbSearch(mailbox).search(mailbox.getOperationConnection(),
                        c, SortBy.NONE, -1, -1, DbSearch.FetchMode.MODCONTENT);

                List<Integer> deferredTagsToClear = new ArrayList<Integer>();

                Flag indexingDeferredFlag = mailbox.getFlagById(Flag.ID_INDEXING_DEFERRED);

                for (DbSearch.Result sr : list) {
                    MailItem item = mailbox.getItemById(sr.getId(), sr.getType());
                    deferredTagsToClear.add(sr.getId());
                    item.tagChanged(indexingDeferredFlag, false);
                }
                mailbox.getOperationConnection(); // we must call this before DbMailItem.alterTag
                DbTag.alterTag(indexingDeferredFlag, deferredTagsToClear, false);
                t.commit();
            }

            if (!mailbox.getVersion().atLeast(1, 5)) {
                try {
                    mailbox.updateVersion(new MailboxVersion((short) 1, (short) 5));
                } catch (ServiceException se) {
                    ZimbraLog.mailbox.warn("Failed to remove deprecated 'deferred' flag from mail items", se);
                }
            }
        } catch (ServiceException se) {
            ZimbraLog.mailbox.warn("Failed to clear deferred flag after " +
                    "reindex all deferred items during mailbox upgrade initialization.", se);
        }
    }

    /**
     * Mailbox version (1.0,1.1)->1.2 Re-Index all contacts.
     */
    void upgradeMailboxTo1_2() throws ServiceException {
        try (final MailboxLock l = mailbox.getWriteLockAndLockIt()) {
            if (!mailbox.getVersion().atLeast(1, 2)) {
                try {
                    mailbox.updateVersion(new MailboxVersion((short) 1, (short) 2));
                } catch (ServiceException e) {
                    ZimbraLog.mailbox.warn("Failed to update mbox version after " +
                            "reindexing contacts on mailbox upgrade initialization.", e);
                }
            }
        }
    }

    /**
     * Entry point for Redo-logging system only. Everybody else should use queueItemForIndexing inside a transaction.
     */
    public void redoIndexItem(MailItem item) {
        try (final MailboxLock l = mailbox.getWriteLockAndLockIt()) {
            add(item);
        } catch (Exception e) {
            ZimbraLog.index.warn("Redo logging is skipping indexing item %d for mailbox %s ",
                    item.getId(), mailbox.getAccountId(), e);
        }
    }

    /**
     * Deletes index documents. The caller doesn't necessarily hold the mailbox lock.
     */
    synchronized void delete(List<Integer> ids) {
        if (ids.isEmpty()) {
            return;
        }
        if(!Zimbra.started()) {
            ZimbraLog.index.debug("Application is not started yet. Queueing items for deleting from index instead of deleting immediately.");
            IndexingQueueAdapter queueAdapter = IndexingQueueAdapter.getFactory().getAdapter();
            DeleteFromIndexTaskLocator itemLocator = new DeleteFromIndexTaskLocator(ids, mailbox.getAccountId(), mailbox.getId(), mailbox.getSchemaGroupId());
            queueAdapter.put(itemLocator);
        } else {
            Indexer indexer;
            try {
                indexer = indexStore.openIndexer();
            } catch (IndexPendingDeleteException e) {
                ZimbraLog.index.debug("delete of ids from index aborted as it is pending delete");
                System.currentTimeMillis();
                return;
            } catch (IOException | ServiceException e) {
                ZimbraLog.index.warn("Failed to open Indexer", e);
                System.currentTimeMillis();
                return;
            }

            try {
                indexer.deleteDocument(ids);
            } catch (IOException | ServiceException e) {
                ZimbraLog.index.warn("Failed to delete index documents", e);
            } finally {
                try {
                    indexer.close();
                } catch (IOException e) {
                    ZimbraLog.index.error("Failed to close Indexer", e);
                    return;
                }
            }
        }
    }

    /**
     * Adds mail items to indexing queue. MailItems should already be in the database.
     * @throws ServiceException
     */
    @VisibleForTesting
    public synchronized boolean queue(List<MailItem> items, boolean isReindexing) throws ServiceException {
        if (items.isEmpty()) {
            return false;
        }
        ZimbraLog.index.debug("Queuing %d items for indexing", items.size());
        IndexingQueueAdapter queueAdapter = IndexingQueueAdapter.getFactory().getAdapter();
        AddToIndexTaskLocator itemLocator = new AddToIndexTaskLocator(items, mailbox.getAccountId(), mailbox.getId(), mailbox.getSchemaGroupId(), mailbox.attachmentsIndexingEnabled(), isReindexing);
        return queueAdapter.add(itemLocator);
    }


    /**
     * Adds mail items to indexing queue and increases attempts counter.
     * MailItems should already be in the database.
     * @throws ServiceException
     */
    public synchronized void retry(List<MailItem> items) throws ServiceException {
        //temporary implementation until indexing queue is in place
        for (MailItem item: items) {
            add(item);
        }
    }

    /**
     * Primes the index for the fastest available search if useful to the underlying IndexStore.
     * This is a very expensive operation especially on large index.
     */
    public void optimize() throws ServiceException {
        indexStore.optimize();
    }

    public synchronized ReIndexStatus getReIndexStatus() {
        IndexingQueueAdapter queueAdapter = IndexingQueueAdapter.getFactory().getAdapter();
        if(queueAdapter == null) {
            return new ReIndexStatus();
        }
        return new ReIndexStatus(queueAdapter.getTotalMailboxTaskCount(mailbox.getAccountId()),
                queueAdapter.getSucceededMailboxTaskCount(mailbox.getAccountId()),
                queueAdapter.getFailedMailboxTaskCount(mailbox.getAccountId()),
                queueAdapter.getTaskStatus(mailbox.getAccountId()));
    }

    public boolean isReIndexInProgress() {
        IndexingQueueAdapter queueAdapter = IndexingQueueAdapter.getFactory().getAdapter();
        if(queueAdapter == null) {
            return false;
        }
        int total = queueAdapter.getTotalMailboxTaskCount(mailbox.getAccountId());
        if(total <= 0) {
            return false;
        }
        int succeeded = queueAdapter.getSucceededMailboxTaskCount(mailbox.getAccountId());
        int failed = queueAdapter.getFailedMailboxTaskCount(mailbox.getAccountId());
        int status = queueAdapter.getTaskStatus(mailbox.getAccountId());
        return (status != ReIndexStatus.STATUS_ABORTED && (succeeded+failed) < total);
    }

    public boolean isCompactIndexInProgress() {
        return false;
    }

    /**
     * Executes a DB search in a mailbox transaction.
     */
    public List<DbSearch.Result> search(DbSearchConstraints constraints,
                                        DbSearch.FetchMode fetch, SortBy sort, int offset, int size, boolean inDumpster, LuceneResultsChunk luceneResults) throws ServiceException {
        List<DbSearch.Result> result;
        try (final Mailbox.MailboxTransaction t = mailbox.mailboxReadTransaction("search", null)) {
            result = new DbSearch(mailbox, inDumpster, luceneResults).search(mailbox.getOperationConnection(),
                    constraints, sort, offset, size, fetch);
            if (fetch == DbSearch.FetchMode.MAIL_ITEM) {
                // Convert UnderlyingData to MailItem
                ListIterator<DbSearch.Result> itr = result.listIterator();
                while (itr.hasNext()) {
                    DbSearch.Result sr = itr.next();
                    try {
                        MailItem item = mailbox.getItem(sr.getItemData());
                        itr.set(new ItemSearchResult(item, sr.getSortValue()));
                    } catch (ServiceException se) {
                        ZimbraLog.index.info(String.format(
                                "Problem constructing Result for folder=%s item=%s from UnderlyingData - dropping item",
                                sr.getItemData().folderId, sr.getItemData().id, sr.getId()), se);
                        itr.remove();
                    }
                }
            }
            t.commit();
        }
        return result;
    }

    /* These regexes really shouldn't be complicated - so this value should be way more than enough.
     * Leaving hard coded.  This is the number of accesses allowed to the underlying CharSequence before
     * deciding that too much resource has been used.
     */
    final private static int MAX_REGEX_ACCESSES = 100000;
    /**
     * Returns all domain names from the index.
     *
     * @param field Lucene field name (e.g. LuceneFields.L_H_CC)
     * @param regex matching pattern or null to match everything
     * @return {@link BrowseTerm}s which correspond to all of the domain terms stored in a given field
     */
    public List<BrowseTerm> getDomains(String field, String regex) throws IOException, ServiceException {
        Pattern pattern = Strings.isNullOrEmpty(regex) ? null : Pattern.compile(
                regex.startsWith("@") ? regex : "@" + regex);
        List<BrowseTerm> result = new ArrayList<BrowseTerm>();
        TermFieldEnumeration values = null;
        try (ZimbraIndexSearcher searcher = indexStore.openSearcher()) {
            values = searcher.getIndexReader().getTermsForField(field);
            while (values.hasMoreElements()) {
                BrowseTerm term = values.nextElement();
                if (term == null) {
                    break;
                }
                String text = term.getText();
                // Domains are tokenized with '@' prefix. Exclude partial domain tokens.
                if (text.startsWith("@") && text.contains(".")) {
                    if (pattern == null || AccessBoundedRegex.matches(text, pattern, MAX_REGEX_ACCESSES)) {
                        result.add(new BrowseTerm(text.substring(1), term.getFreq()));
                    }
                }
            }
        }
        return result;
    }

    /**
     * Returns all attachment types from the index.
     *
     * @param regex matching pattern or null to match everything
     * @return {@link BrowseTerm}s which correspond to all of the attachment types in the index
     */
    public List<BrowseTerm> getAttachmentTypes(String regex) throws IOException, ServiceException {
        Pattern pattern = Strings.isNullOrEmpty(regex) ? null : Pattern.compile(regex);
        List<BrowseTerm> result = new ArrayList<BrowseTerm>();
        TermFieldEnumeration values = null;
        try (ZimbraIndexSearcher searcher = indexStore.openSearcher()) {
            values = searcher.getIndexReader().getTermsForField(LuceneFields.L_ATTACHMENTS);
            while (values.hasMoreElements()) {
                BrowseTerm term = values.nextElement();
                if (pattern == null || AccessBoundedRegex.matches(term.getText(), pattern, MAX_REGEX_ACCESSES)) {
                    result.add(term);
                }
            }
        }
        return result;
    }

    /**
     * Returns all objects (e.g. PO, etc) from the index.
     *
     * @param regex matching pattern or null to match everything
     * @return {@link BrowseTerm}s which correspond to all of the objects in the index
     */
    public List<BrowseTerm> getObjects(String regex) throws IOException, ServiceException {
        Pattern pattern = Strings.isNullOrEmpty(regex) ? null : Pattern.compile(regex);
        List<BrowseTerm> result = new ArrayList<BrowseTerm>();
        try (ZimbraIndexSearcher searcher = indexStore.openSearcher();
             TermFieldEnumeration values = searcher.getIndexReader().getTermsForField(LuceneFields.L_OBJECTS)) {
            while (values.hasMoreElements()) {
                BrowseTerm term = values.nextElement();
                if (term == null) {
                    break;
                }
                if (pattern == null || AccessBoundedRegex.matches(term.getText(), pattern, MAX_REGEX_ACCESSES)) {
                    result.add(term);
                }
            }
        }
        return result;
    }

    /**
     * Adds the item to the index. This has to be called inside a transaction.
     *
     * @param item item to index
     * @throws ServiceException
     */
    synchronized boolean add(MailItem item) {

        if(!Zimbra.started()) {
            return false;
        } else {
            Indexer indexer = null;
            try {
                indexer = indexStore.openIndexer();
                indexer.addDocument(item,  item.generateIndexDataAsync(mailbox.attachmentsIndexingEnabled()));
                DbMailItem.setIndexId(mailbox.getOperationConnection(), mailbox, item.getId());
                item.state.setIndexId(item.getId());
            }  catch (IndexPendingDeleteException e) {
                ZimbraLog.index.debug("Adding of entries to index aborted as index is pending delete");
            } catch (TemporaryIndexingException | IOException | ServiceException e) {
                return false;
            } finally {
                try {
                    if(indexer != null) {
                        indexer.close();
                    }
                } catch (IOException e) {
                    ZimbraLog.index.error("Failed to close Indexer", e);
                }
            }
        }
        return true;
    }

    private static final class ItemSearchResult extends DbSearch.Result {
        private final MailItem item;

        ItemSearchResult(MailItem item, Object sortkey) {
            super(sortkey);
            this.item = item;
        }

        @Override
        public int getId() {
            return item.getId();
        }

        @Override
        public int getIndexId() {
            return item.getIndexId();
        }

        @Override
        public Type getType() {
            return item.getType();
        }

        @Override
        public MailItem getItem() {
            return item;
        }
    }
}
