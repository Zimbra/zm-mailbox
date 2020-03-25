package com.zimbra.cs.index.solr;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.index.IndexStore;
import com.zimbra.cs.index.Indexer;
import com.zimbra.cs.index.ZimbraIndexDocumentID;
import com.zimbra.cs.index.ZimbraIndexReader;
import com.zimbra.cs.index.ZimbraIndexSearcher;
import com.zimbra.cs.index.ZimbraTermsFilter;
import com.zimbra.cs.index.ZimbraTopDocs;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxIndex.IndexType;
import com.zimbra.cs.mailbox.MailboxIndex.ItemIndexDeletionInfo;

public class MockSolrIndex extends SolrIndex {

    public MockSolrIndex() {
        super(null, null);
    }

    @Override
    public void warmup() {
        // TODO Auto-generated method stub
        super.warmup();
    }

    @Override
    public boolean isPendingDelete() {
        // TODO Auto-generated method stub
        return super.isPendingDelete();
    }

    @Override
    public void setPendingDelete(boolean pendingDelete) {
        // TODO Auto-generated method stub
        super.setPendingDelete(pendingDelete);
    }

    @Override
    public void optimize() {
        // TODO Auto-generated method stub
        super.optimize();
    }

    @Override
    public boolean verify(PrintStream out) throws IOException {
        // TODO Auto-generated method stub
        return super.verify(out);
    }

    @Override
    public List<Map<String, Object>> fetchFileList(long gen, String accountId)
            throws ServiceException {
        // TODO Auto-generated method stub
        return super.fetchFileList(gen, accountId);
    }

    @Override
    public synchronized Indexer openIndexer() throws IOException, ServiceException {
        return new SolrIndexer();
    }

    @Override
    public ZimbraIndexSearcher openSearcher() throws IOException,
            ServiceException {
        final SolrIndexReader reader = new SolrIndexReader();
        return new SolrIndexSearcher(reader);
    }

    @Override
    public void evict() {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteIndex() throws IOException, ServiceException {
        // TODO Auto-generated method stub

    }

    public class SolrIndexReader extends SolrIndex.SolrIndexReader {

        @Override
        public synchronized void close() throws IOException {
            // TODO Auto-generated method stub
            super.close();
        }

        @Override
        public synchronized int numDocs() throws ServiceException {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public TermFieldEnumeration getTermsForField(String field) throws IOException, ServiceException {
            return null;
        }
    }

    protected class SolrIndexSearcher implements ZimbraIndexSearcher {
        final SolrIndexReader reader;

        public SolrIndexSearcher(SolrIndexReader reader) {
            this.reader = reader;
        }
        @Override
        public void close() throws IOException {
            // TODO Auto-generated method stub

        }

        @Override
        public Document doc(ZimbraIndexDocumentID docID) throws IOException, ServiceException {
            return null;
        }

        @Override
        public int docFreq(Term term) throws IOException, ServiceException {
            return 0;
        }

        @Override
        public ZimbraIndexReader getIndexReader() {
            return reader;
        }

        @Override
        public ZimbraTopDocs search(Query query, int n, Collection<IndexType> types) throws IOException, ServiceException {
            return search(query,null, n, types);
        }

        @Override
        public ZimbraTopDocs search(Query query, ZimbraTermsFilter filter, int n, Collection<IndexType> types)
                throws IOException, ServiceException {
            return search(query, filter, n, null, types);
        }

        //TODO: return all fields that we need in topfielddocs so that we don't have to go back to index again
        @Override
        public ZimbraTopDocs search(Query query, ZimbraTermsFilter filter,
                int n, Sort sort, Collection<IndexType> types) throws IOException, ServiceException {
            return null;
        }
        @Override
        public ZimbraTopDocs search(Query query, ZimbraTermsFilter filter,
                int n, Sort sort, String idField, String[] fetchFields, Collection<IndexType> types)
                throws IOException, ServiceException {
            return null;
        }
    }

    public static final class Factory implements IndexStore.Factory {

        public Factory() {
            ZimbraLog.index.info("Created MockIndexStore\n");
        }

        @Override
        public SolrIndex getIndexStore(String accountId) {
            return new MockSolrIndex();
        }

        /**
         * Cleanup any caches etc associated with the IndexStore
         */
        @Override
        public synchronized void destroy() {

        }
    }

    private class SolrIndexer extends SolrIndex.SolrIndexer {

        @Override
        public synchronized void close() throws IOException {
        }

        @Override
        public void add(List<Mailbox.IndexItemEntry> entries) throws IOException, ServiceException {
            return;
        }

        @Override
        public void addDocument(MailItem item, List<IndexDocument> docs) throws ServiceException {
            return;
        }

        @Override
        public void deleteDocument(List<ItemIndexDeletionInfo> ids) throws IOException,ServiceException {
            return;
        }

        @Override
        public void deleteDocumentById(List<Integer> ids) throws IOException, ServiceException {
            return;
        }

        @Override
        public void deleteDocument(List<Integer> ids, String fieldName)
                throws IOException, ServiceException {
        }

        @Override
        public void addSearchHistoryDocument(IndexDocument doc)
                throws IOException, ServiceException {
        }
    }
}
