/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.index.elasticsearch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.WildcardQuery;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.index.BrowseTerm;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.index.IndexStore;
import com.zimbra.cs.index.Indexer;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.ZimbraAnalyzer;
import com.zimbra.cs.index.ZimbraIndexDocumentID;
import com.zimbra.cs.index.ZimbraIndexReader;
import com.zimbra.cs.index.ZimbraIndexSearcher;
import com.zimbra.cs.index.ZimbraScoreDoc;
import com.zimbra.cs.index.ZimbraTermsFilter;
import com.zimbra.cs.index.ZimbraTopDocs;
import com.zimbra.cs.index.ZimbraTopFieldDocs;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * {@link IndexStore} implementation using ElasticSearch via the REST API.  There is a Java API but that ties to
 * a particular version.
 * Security might also be an issue with standard ElasticSearch
 * https://github.com/sonian/elasticsearch-jetty is a way to add SSL to the comms
 * Elasticsearch uses http.  If better network security is desired, most suggestions point towards hiding behind an
 * SSL proxy.  I think nginx can help with this - see http://wiki.nginx.org/HttpCoreModule#listen
 *
 * Elasticsearch URL:
 * http://localhost:9200/INDEX-NAME/INDEX-TYPE/INDEX-ID'
 */
public final class ElasticSearchIndex extends IndexStore {

    private final static String indexType = "zimbra";
    private final Mailbox mailbox;
    private final String key;
    private final String indexUrl;
    private boolean haveMappingInfo = false;

    private ElasticSearchIndex(Mailbox mbox) {
        this.mailbox = mbox;
        this.key = mailbox.getAccountId();
        this.indexUrl = String.format("%s%s/", LC.zimbra_index_elasticsearch_url_base.value(), key);
    }

    private void initializeIndex() {
        if (haveMappingInfo) {
            return;
        }
        if (!refreshIndexIfNecessary()) {
            try {
                ElasticSearchConnector connector = new ElasticSearchConnector();
                JSONObject mappingInfo = createMappingInfo();
                HttpPut putMethod = new HttpPut(ElasticSearchConnector.actualUrl(indexUrl));
                putMethod.setEntity(new StringEntity(mappingInfo.toString(),
                        MimeConstants.CT_APPLICATION_JSON, MimeConstants.P_CHARSET_UTF8));
                int statusCode = connector.executeMethod(putMethod);
                if (statusCode == HttpStatus.SC_OK) {
                    haveMappingInfo = true;
                    refreshIndexIfNecessary(); // Sometimes searches don't seem to honor mapping info.  Try to force it
                } else {
                    ZimbraLog.index.error("Problem Setting mapping information for index with key=%s httpstatus=%d",
                            key, statusCode);
                }
            } catch (IOException e) {
                ZimbraLog.index.error("Problem Getting mapping information for index with key=" + key, e);
            } catch (JSONException e) {
                ZimbraLog.index.error("Problem Setting mapping information for index with key=" + key, e);
            }
        }
    }

    /**
     * By default, ElasticSearch refreshes every second (configurable?).  Can force it using this.
     * TODO: Perhaps only do this if we've written something in the last second?
     */
    private boolean refreshIndexIfNecessary() {
        String url = String.format("%s_refresh", indexUrl);
        HttpGet method = new HttpGet(ElasticSearchConnector.actualUrl(url));
        try {
            ElasticSearchConnector connector = new ElasticSearchConnector();
            int statusCode = connector.executeMethod(method);
            if (statusCode == HttpStatus.SC_OK) {
                return true;
            } else if (statusCode == HttpStatus.SC_NOT_FOUND) {
                ZimbraLog.index.debug("Index not present on %s %d", url, statusCode);
                return false;
            }
            ZimbraLog.index.error("Problem refreshing index %s %d", url, statusCode);
        } catch (IOException e) {
            ZimbraLog.index.error("Problem refreshing index %s", url, e);
        }
        return false;
    }


    private JSONObject createMappingInfo() throws JSONException {
        JSONObject topLevel = new JSONObject();
        JSONObject settings = new JSONObject();
        JSONObject mappings = new JSONObject();
        JSONObject zimbra = new JSONObject();
        JSONObject properties = new JSONObject();
        JSONObject source = new JSONObject();
        topLevel.put("settings", settings);
        JSONObject index = new JSONObject();
        settings.put("index", index);
        JSONObject analysis = new JSONObject();
        index.put("analysis", analysis);
        JSONObject analyzer = new JSONObject();
        analysis.put("analyzer", analyzer);
        JSONObject zimbraContent = new JSONObject();
        analyzer.put("zimbrastandard", zimbraContent);
        // We rely on ZimbraAnalyzer to pre-tokenize the input and then supply the tokens to Elasticsearch
        // with a " " between each token.  Implicit assumption is that " " is a valid token separator.
        zimbraContent.put("tokenizer", "whitespace");

        // Analog to NumberTokenizer used with reference to LuceneFields.L_SORT_SIZE for Lucene BUT
        // Not needed here because we don't analyze that field.
        // JSONObject positivenumber = new JSONObject();
        // analyzer.put("positivenumber", positivenumber);
        // positivenumber.put("type", "pattern");
        // positivenumber.put("pattern", "[^\\d]+");

        // Thought of using the "uax_url_email" tokenizer but that behaves differently.
        JSONObject emailaddress = new JSONObject();
        analyzer.put("emailaddress", emailaddress);
        emailaddress.put("type", "pattern");
        emailaddress.put("pattern", "(\\s+)|([<>,\\\'\\\"]+)|(\\)+)|(\\(+)|(\\]+)|(\\[+)");

        JSONObject contactdata = new JSONObject();
        analyzer.put("contactdata", contactdata);
        contactdata.put("type", "pattern");
        contactdata.put("pattern", "(\\s+)|([<>,\\\'\\\"]+)|(\\)+)|(\\(+)|(\\]+)|(\\[+)");
        JSONArray stopwords = new JSONArray();
        stopwords.put(".");
        contactdata.put("stopwords", stopwords);

        topLevel.put("mappings", mappings);
        mappings.put(indexType, zimbra);
        zimbra.put("_source", source);
        source.put("enabled", false); // save space
        source.put("_all", false); // save space
        zimbra.put("properties", properties);

        // See ZimbraAnalyzer.tokenStream(String field, Reader reader, Analyzer analyzer)
        // Only ever used via MimeTypeTokenStream
        properties.put(LuceneFields.L_MIMETYPE,
                new StringFieldProperty(false).analyzed().analyzer("whitespace").asJSON());
        properties.put(LuceneFields.L_PARTNAME, new StringFieldProperty(true).notAnalyzed().asJSON());
        // Should have been tokenized by FilenameTokenizer already
        properties.put(LuceneFields.L_FILENAME,
                new StringFieldProperty(true).analyzed().analyzer("whitespace").asJSON());
        properties.put(LuceneFields.L_SORT_SIZE, new FieldProperty("long", true).notAnalyzed().asJSON());
        properties.put(LuceneFields.L_SORT_ATTACH, new StringFieldProperty(true).notAnalyzed().asJSON());
        properties.put(LuceneFields.L_SORT_FLAG, new StringFieldProperty(true).notAnalyzed().asJSON());
        properties.put(LuceneFields.L_SORT_PRIORITY, new StringFieldProperty(true).notAnalyzed().asJSON());
        properties.put(LuceneFields.L_H_FROM,
                new StringFieldProperty(false).analyzed().analyzer("emailaddress").asJSON());
        properties.put(LuceneFields.L_H_TO,
                new StringFieldProperty(false).analyzed().analyzer("emailaddress").asJSON());
        properties.put(LuceneFields.L_H_CC,
                new StringFieldProperty(false).analyzed().analyzer("emailaddress").asJSON());
        properties.put(LuceneFields.L_H_X_ENV_FROM,
                new StringFieldProperty(false).analyzed().analyzer("emailaddress").asJSON());
        properties.put(LuceneFields.L_H_X_ENV_TO,
                new StringFieldProperty(false).analyzed().analyzer("emailaddress").asJSON());
        // For Lucene ZimbraAnalyzer "private TokenStream tokenStream(String field, Reader reader, Analyzer analyzer)"
        // uses KeywordTokenizer for this.  However, it is not stored and not analyzed when added
        properties.put(LuceneFields.L_H_MESSAGE_ID, new StringFieldProperty(false).notAnalyzed().asJSON());
        properties.put(LuceneFields.L_FIELD,
                new StringFieldProperty(false).analyzed().analyzer("zimbrastandard").asJSON());
        properties.put(LuceneFields.L_SORT_NAME, new StringFieldProperty(false).notAnalyzed().asJSON());
        properties.put(LuceneFields.L_H_SUBJECT,
                new StringFieldProperty(false).analyzed().analyzer("zimbrastandard").asJSON());
        properties.put(LuceneFields.L_SORT_SUBJECT, new StringFieldProperty(false).notAnalyzed().asJSON());
        properties.put(LuceneFields.L_CONTENT,
                new StringFieldProperty(false).analyzed().analyzer("zimbrastandard").asJSON());
        // Only ever used via MimeTypeTokenStream
        properties.put(LuceneFields.L_ATTACHMENTS,
                new StringFieldProperty(false).analyzed().analyzer("whitespace").asJSON());
        properties.put(LuceneFields.L_MAILBOX_BLOB_ID, new FieldProperty("integer", true).notAnalyzed().asJSON());
        properties.put(LuceneFields.L_SORT_DATE, new DateFieldProperty(true).notAnalyzed().asJSON());
        properties.put(LuceneFields.L_CONTACT_DATA,
                new StringFieldProperty(false).analyzed().analyzer("contactdata").asJSON());
        properties.put(LuceneFields.L_OBJECTS,
                new StringFieldProperty(false).analyzed().analyzer("zimbrastandard").asJSON());
        properties.put(LuceneFields.L_VERSION, new StringFieldProperty(true).notAnalyzed().asJSON());
        return topLevel;
    }

    private class FieldProperty {
        private static final String YES = "yes";
        private static final String NO = "no";
        private static final String ANALYZED = "analyzed";
        private static final String NOT_ANALYZED = "not_analyzed";

        private final JSONObject fieldInfo = new JSONObject();

        public FieldProperty(String type, boolean store) throws JSONException {
            if (type != null) {
                put("type", type);
            }
            put("store", store ? YES : NO);
        }
        public FieldProperty analyzed() throws JSONException {
            return index(ANALYZED);
        }
        public FieldProperty notAnalyzed() throws JSONException {
            return index(NOT_ANALYZED);
        }
        public FieldProperty index(String indexType) throws JSONException {
            put("index", indexType);
            return this;
        }
        public FieldProperty analyzer(String analyzer) throws JSONException {
            put("analyzer", analyzer);
            return this;
        }
        public FieldProperty put(String key, String value) throws JSONException {
            fieldInfo.put(key, value);
            return this;
        }

        public JSONObject asJSON() {
            return fieldInfo;
        }
    }

    private class DateFieldProperty extends FieldProperty {
        private static final String TYPE_DATE = "date";
        public DateFieldProperty(boolean store) throws JSONException {
            super(TYPE_DATE, store);
            put("format", "yyyyMMddHHmmssSSS");
            // Default value is 4.  Lower the value, larger the index, faster the searches
            // Suitable values are between 1 and 8
            // put("precision_step", "4");
        }
    }

    private class StringFieldProperty extends FieldProperty {
        private static final String TYPE_STRING = "string";
        public StringFieldProperty(boolean store) throws JSONException {
            super(TYPE_STRING, store);
        }
    }

    @Override
    public Indexer openIndexer() {
        return new ElasticSearchIndexer();
    }

    @Override
    public ZimbraIndexSearcher openSearcher() {
        final ElasticIndexReader reader = new ElasticIndexReader();
        return new ZimbraElasticIndexSearcher(reader);
    }

    @Override
    public void deleteIndex() {
        HttpRequestBase method = new HttpDelete(ElasticSearchConnector.actualUrl(indexUrl));
        try {
            ElasticSearchConnector connector = new ElasticSearchConnector();
            int statusCode = connector.executeMethod(method);
            if (statusCode == HttpStatus.SC_OK) {
                boolean ok = connector.getBooleanAtJsonPath(new String[] {"ok"}, false);
                boolean acknowledged = connector.getBooleanAtJsonPath(new String[] {"acknowledged"}, false);
                if (!ok || !acknowledged) {
                    ZimbraLog.index.debug("Delete index status ok=%b acknowledged=%b", ok, acknowledged);
                }
            } else {
                String error = connector.getStringAtJsonPath(new String[] {"error"});
                if (error != null && error.startsWith("IndexMissingException")) {
                    ZimbraLog.index.debug("Unable to delete index for key=%s.  Index is missing", key);
                } else {
                    ZimbraLog.index.error("Problem deleting index for key=%s error=%s", key, error);
                }
            }
        } catch (IOException e) {
            ZimbraLog.index.error("Problem Deleting index with key=" + key, e);
        }
        haveMappingInfo = false;
    }

    /**
     * TODO:  Do something similar to this from LuceneIndex?
     * Runs a common search query + common sort order (and throw away the result) to warm up the Lucene cache and OS
     * file system cache.
     */
    @Override
    public void warmup() {
        // See http://www.elasticsearch.org/guide/reference/api/admin-indices-warmers.html
    }

    /**
     * Removes from cache - if appropriate
     */
    @Override
    public void evict() {
    }

    @Override
    public boolean verify(PrintStream out) {
        return true;
    }

    public int getDocCount() {
        refreshIndexIfNecessary();
        String url = String.format("%s%s/docs/", indexUrl, "_stats");
        HttpGet method = new HttpGet(ElasticSearchConnector.actualUrl(url));
        try {
            ElasticSearchConnector connector = new ElasticSearchConnector();
            int statusCode = connector.executeMethod(method);
            if (statusCode == HttpStatus.SC_OK) {
                int cnt = connector.getIntAtJsonPath(new String[] {"_all", "total", "docs", "count"}, 0);
                return cnt;
            }
        } catch (IOException e) {
            ZimbraLog.index.error("Problem getting stats for index %s", url, e);
        }
        return 0;
    }

    public static final class Factory implements IndexStore.Factory {

        public Factory() {
            ZimbraLog.index.info("Created ElasticSearchIndex\n");
        }

        @Override
        public ElasticSearchIndex getIndexStore(Mailbox mbox) {
            return new ElasticSearchIndex(mbox);
        }

        /**
         * Cleanup any caches etc associated with the IndexStore
         */
        @Override
        public void destroy() {
        }

        public List<String> getIndexes() {
            List<String> indexNames = Lists.newArrayList();
            String url = String.format("%s%s", LC.zimbra_index_elasticsearch_url_base.value(), "_status");
            HttpGet method = new HttpGet(ElasticSearchConnector.actualUrl(url));
            try {
                ElasticSearchConnector connector = new ElasticSearchConnector();
                int statusCode = connector.executeMethod(method);
                if (statusCode != HttpStatus.SC_OK) {
                    ZimbraLog.index.error("Problem getting list of Elastic Search indexes httpstatus=%d", statusCode);
                }
                JSONObject indices =  connector.getObjectAtJsonPath(new String[] {"indices"});
                if (indices != null) {
                    JSONArray names = indices.names();
                    if (names != null) {
                        for (int index = 0; index < names.length(); index++) {
                            String name = names.optString(index);
                            if (name != null) {
                                indexNames.add(name);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                ZimbraLog.index.error("Problem getting list of Elastic Search indexes", e);
            }
            return indexNames;
        }
    }

    private final class ElasticSearchIndexer implements Indexer {

        @Override
        public void close() {
        }

        @Override
        public void compact() {
        }

        /**
         * <p>Used from SOAP GetIndexStatsRequest</p>
         * @return total number of documents in this index excluding documents marked for deletion
         */
        @Override
        public int maxDocs() {
            return getDocCount();
        }

        private String streamToString(TokenStream tokenStream) throws IOException {
            tokenStream.reset();
            List<String> toks = Lists.newArrayList();
            CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            while (tokenStream.incrementToken()) {
                toks.add(charTermAttribute.toString());
            }
            tokenStream.end();
            tokenStream.close();
            return Joiner.on(" ").join(toks);
        }

        /**
         * For Lucene, we either provide a token stream (i.e. Everything already analyzed)
         * @param fieldName
         * @return
         */
        private boolean useZimbraAnalyzer(String fieldName) {
            LuceneFields.IndexField field = LuceneFields.IndexField.fromFieldName(fieldName);
            if ( (field.getIndexSetting().equals(Field.Index.NOT_ANALYZED)) ||
                 (field.getIndexSetting().equals(Field.Index.NOT_ANALYZED_NO_NORMS)) ||
                 (field.getIndexSetting().equals(Field.Index.NO))) {
                return false;
            }
            return true;
        }

        private String readerToTokenString(String fieldName, Reader original) throws IOException {
            if (useZimbraAnalyzer(fieldName)) {
                Analyzer analyzer = ZimbraAnalyzer.getInstance();
                String tokens = streamToString(analyzer.tokenStream(fieldName, original));
                return tokens;
            } else {
                BufferedReader bufferedReader = new BufferedReader(original);
                String line = null;
                StringBuilder sb = new StringBuilder();
                String ls = System.getProperty("line.separator");
                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                    sb.append(ls);
                }
                return sb.toString();
            }
        }

        private String stringToTokenString(String fieldName, String original) throws IOException {
            if (useZimbraAnalyzer(fieldName)) {
                Analyzer analyzer = ZimbraAnalyzer.getInstance();
                String tokens = streamToString(analyzer.tokenStream(fieldName, new StringReader(original)));
                return tokens;
            } else {
                return original;
            }
        }

        private void addFieldToDocument(JSONObject jsonObj, Fieldable field) throws IOException {
            try {
                if (field.isTokenized()) {
                    TokenStream stream = field.tokenStreamValue();
                    if (stream != null) {
                        String tokens = streamToString(stream);
                        jsonObj.put(field.name(), tokens);
                    } else {
                        Reader reader = field.readerValue();
                        if (reader != null) {
                            jsonObj.put(field.name(), readerToTokenString(field.name(), reader));
                        } else {
                            String val = field.stringValue();
                            if (val != null) {
                                jsonObj.put(field.name(), stringToTokenString(field.name(), val));
                            } else {
                                ZimbraLog.index.debug("addFieldToDocument IGNORING tokenized field=%s", field.name());
                            }
                        }
                    }
                } else {
                    String val = field.stringValue();
                    if (val != null) {
                        jsonObj.put(field.name(), stringToTokenString(field.name(), val));
                    } else {
                        ZimbraLog.index.debug("addFieldToDocument IGNORING field=%s", field.name());
                    }
                }
            } catch (JSONException e) {
                ZimbraLog.index.error("Problem creating JSON for indexing document", e);
            }
        }

        /**
         * Adds the list of documents to the index.
         * <p>
         * If the index status is stale, delete the stale documents first, then add new documents. If the index status
         * is deferred, we are sure that this item is not already in the index, and so we can skip the check-update step.
         */
        @Override
        public void addDocument(Folder folder, MailItem item, List<IndexDocument> docs) throws IOException {
            if (docs == null || docs.isEmpty()) {
                return;
            }
            initializeIndex();

            // handle the partial re-index case here by simply deleting all the documents matching the index_id
            // so that we can simply add the documents to the index later!!
            switch (item.getIndexStatus()) {
                case STALE:
                case DONE: // for partial re-index
                    List<Integer> ids = Lists.newArrayListWithCapacity(1);
                    ids.add(new Integer(item.getId()));
                    deleteDocument(ids);
                    break;
                case DEFERRED:
                    break;
                default:
                    assert false : item.getIndexId();
            }

            for (IndexDocument doc : docs) {
                // Note: using automatic ID generation
                String url = String.format("%s%s/", indexUrl, indexType);
                HttpPost method = new HttpPost(ElasticSearchConnector.actualUrl(url));
                JSONObject jsonObj = new JSONObject();
                // doc can be shared by multiple threads if multiple mailboxes are referenced in a single email
                synchronized (doc) {
                    setFields(item, doc);
                    Document luceneDoc = doc.toDocument();
                    for (Fieldable field :luceneDoc.getFields()) {
                        addFieldToDocument(jsonObj, field);
                    }
                }
                try {
                    method.setEntity(new StringEntity(jsonObj.toString(),
                            MimeConstants.CT_APPLICATION_JSON, MimeConstants.P_CHARSET_UTF8));
                    ElasticSearchConnector connector = new ElasticSearchConnector();
                    int statusCode = connector.executeMethod(method);
                    if (statusCode != HttpStatus.SC_CREATED) {
                        ZimbraLog.index.error("Problem indexing document with id=%d httpstatus=%d",
                                item.getId(), statusCode);
                    }
                } catch (IOException e) {
                    ZimbraLog.index.error("Problem indexing document with id=%d", item.getId());
                }
            }
        }

        /**
         * Delete all documents associated with each mailbox blob ID in the provided list.
         */
        @Override
        public void deleteDocument(List<Integer> ids) {
            refreshIndexIfNecessary();
            String url = String.format("%s%s/_query", indexUrl, indexType);
            for (Integer id : ids) {

                String query = String.format("%s:%s", LuceneFields.L_MAILBOX_BLOB_ID, id.toString());
                StringBuilder urlBuilder = new StringBuilder()
                .append(ElasticSearchConnector.actualUrl(url)).append("q=")
                .append(query);
                HttpDelete method = new HttpDelete(ElasticSearchConnector.actualUrl(url));
                try {
                    ElasticSearchConnector connector = new ElasticSearchConnector();
                    int statusCode = connector.executeMethod(method);
                    if (statusCode == HttpStatus.SC_OK) {
                        ZimbraLog.index.debug("Deleted documents with id=%d", id);
                    } else {
                        ZimbraLog.index.error("Problem deleting documents with id=%d httpstatus=%d", id, statusCode);
                    }
                } catch (IOException e) {
                    ZimbraLog.index.error("Problem deleting documents with id=%d", id);
                }
            }
        }
    }

    private final class ElasticIndexReader implements ZimbraIndexReader {

        @Override
        public void close() throws IOException {
        }

        /**
         * Returns the number of documents in this index.
         */
        @Override
        public int numDocs() {
            return getDocCount();
        }

        /**
         * Number of documents marked for deletion but not yet fully removed from the index
         * @return number of deleted documents for this index
         */
        @Override
        public int numDeletedDocs() {
            refreshIndexIfNecessary();
            String url = String.format("%s%s/docs/", indexUrl, "_stats");
            HttpGet method = new HttpGet(ElasticSearchConnector.actualUrl(url));
            try {
                ElasticSearchConnector connector = new ElasticSearchConnector();
                int statusCode = connector.executeMethod(method);
                if (statusCode == HttpStatus.SC_OK) {
                    int cnt = connector.getIntAtJsonPath(new String[] {"_all", "total", "docs", "deleted"}, 0);
                    return cnt;
                }
            } catch (IOException e) {
                ZimbraLog.index.error("Problem getting stats for index %s", url, e);
            }
            return 0;
        }

        /**
         * Returns an enumeration of the String representations for values of terms with {@code field}
         * positioned to start at the first term with a value greater than {@code firstTermValue}.
         * The enumeration is ordered by String.compareTo().
         */
        @Override
        public TermFieldEnumeration getTermsForField(String field, String firstTermValue) throws IOException {
            return new ElasticSearchTermValueEnumeration(field, firstTermValue);
        }

        /**
         * Relies on ElasticSearch plugin elasticsearch-index-termlist being installed.
         * Note that that currently doesn't support doc frequency information
         * TODO:  Fork plugin to add doc frequency information?
         */
        private final class ElasticSearchTermValueEnumeration implements TermFieldEnumeration {
            private final Queue<BrowseTerm> termValues = Lists.newLinkedList();
            private ElasticSearchTermValueEnumeration(String field, String firstTermValue) {
                List<BrowseTerm> allValues = Lists.newArrayList();
                refreshIndexIfNecessary();
                String url = String.format("%s_termlist/%s", indexUrl, field);
                HttpGet method = new HttpGet(ElasticSearchConnector.actualUrl(url));
                try {
                    ElasticSearchConnector connector = new ElasticSearchConnector();
                    int statusCode = connector.executeMethod(method);
                    if (statusCode == HttpStatus.SC_OK) {
                        JSONArray terms =  connector.getArrayAtJsonPath(new String[] {"terms"});
                        if (terms != null) {
                            for (int index = 0; index < terms.length(); index++) {
                                String hit = terms.optString(index);
                                if ((hit != null) && (hit.compareTo(firstTermValue) >= 0)) {
                                    allValues.add(new BrowseTerm(hit, 1 /* TODO: want docFreq() */));
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    ZimbraLog.index.error("Problem getting stats for index %s", url, e);
                }
                Collections.sort(allValues, new Comparator<BrowseTerm>() {
                    @Override
                    public int compare(BrowseTerm o1, BrowseTerm o2) {
                        int retVal = o1.getText().compareTo(o2.getText());
                        if (retVal == 0) {
                            retVal = o2.getFreq() - o1.getFreq();
                        }
                        return retVal;
                    }
                });
                termValues.addAll(allValues);
            }

            @Override
            public boolean hasMoreElements() {
                return (termValues.peek() != null);
            }

            @Override
            public BrowseTerm nextElement() {
                BrowseTerm nextVal = termValues.poll();
                if (nextVal == null) {
                    throw new NoSuchElementException("No more values");
                }
                return nextVal;
            }

            @Override
            public void close() throws IOException {
            }
        }
    }

    @Override
    public boolean isPendingDelete() {
        return false;
    }

    @Override
    public void setPendingDelete(boolean pendingDelete) {
        // NO-OP
    }

    public final class ZimbraElasticIndexSearcher implements ZimbraIndexSearcher {
        final ElasticIndexReader reader;

        public ZimbraElasticIndexSearcher(ElasticIndexReader reader) {
            this.reader = reader;
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }

        /**
         * Returns the stored fields of document {@code docID} (an index store specific ID for the document)
         */
        @Override
        public Document doc(ZimbraIndexDocumentID docID) throws IOException {
            if (docID == null) {
                return null;
            }
            if (docID instanceof ZimbraElasticDocumentID) {
                ZimbraElasticDocumentID eDocID = (ZimbraElasticDocumentID) docID;
                String storedFields[] = { LuceneFields.L_PARTNAME, LuceneFields.L_FILENAME, LuceneFields.L_SORT_SIZE,
                        LuceneFields.L_SORT_ATTACH, LuceneFields.L_SORT_FLAG, LuceneFields.L_SORT_PRIORITY,
                        LuceneFields.L_MAILBOX_BLOB_ID, LuceneFields.L_SORT_DATE, LuceneFields.L_VERSION };
                String url = String.format("%s%s/%s?fields=%s", indexUrl, indexType, eDocID.getDocID(),
                        Joiner.on(',').join(storedFields));
                HttpGet method = new HttpGet(ElasticSearchConnector.actualUrl(url));
                try {
                    ElasticSearchConnector connector = new ElasticSearchConnector();
                    int statusCode = connector.executeMethod(method);
                    if (statusCode == HttpStatus.SC_OK) {
                        Document document = new Document();
                        // If _source is available, can use that but it is more space efficient to just get the
                        // fields that we store.
                        // JSONObject body =  connector.getObjectAtJsonPath(new String[] {"_source"});
                        JSONObject body =  connector.getObjectAtJsonPath(new String[] {"fields"});
                        if (body == null) {
                            return document;  // Rather unexpected - perhaps _source has been disabled?
                        }
                        Iterator iter = body.keys();
                        while (iter.hasNext()) {
                            String key = (String)iter.next();
                            document.add(new Field(key, body.getString(key), Field.Store.YES, Field.Index.NO));
                        }
                        return document;
                    }
                } catch (JSONException e) {
                    throw new IOException("Problem processing JSON representing " + url, e);
                }
                return null;
            }
            throw new IllegalArgumentException("Expected a ZimbraElasticDocumentID");
        }

        /**
         * Sometimes used to decide whether we think a query is best evaluated DB-FIRST or INDEX-FIRST.
         * @return the number of documents containing the term {@code term}.
         */
        @Override
        public int docFreq(Term term) throws IOException {
            try {
                JSONObject jsonobj = new JSONObject();
                JSONObject termObj = new JSONObject();
                termObj.put(term.field(), term.text());
                jsonobj.put("term", termObj);
                String url = String.format("%s%s/_count", indexUrl, indexType);
                HttpPost method = new HttpPost(ElasticSearchConnector.actualUrl(url));
                try {
                    method.setEntity(new StringEntity(jsonobj.toString(),
                                MimeConstants.CT_APPLICATION_JSON, MimeConstants.P_CHARSET_UTF8));
                    ElasticSearchConnector connector = new ElasticSearchConnector();
                    refreshIndexIfNecessary();
                    int statusCode = connector.executeMethod(method);
                    if (statusCode == HttpStatus.SC_OK) {
                        return connector.getIntAtJsonPath(new String[] {"count"}, 0);
                    }
                } catch (IOException e) {
                    ZimbraLog.index.error("Problem with docFreq %s", url, e);
                }
            } catch (JSONException e) {
                ZimbraLog.index.debug("ElasticSearchIndex docFreq - problem creating JSON", e);
                return 0;
            }
            return 0;
        }

        @Override
        public ZimbraIndexReader getIndexReader() {
            return reader;
        }

        /**
         * Finds the top n hits for query.
         */
        @Override
        public ZimbraTopDocs search(Query query, int n) throws IOException {
            return search(query, null, n);
        }
        /**
         * Finds the top n hits for query, applying filter if non-null.
         */
        @Override
        public ZimbraTopDocs search(Query query, ZimbraTermsFilter filter, int n) throws IOException {
            List<ZimbraScoreDoc>scoreDocs = Lists.newArrayList();
            JSONObject requestJson = null;
            try {
                requestJson = searchQueryToJSON(query, filter, null);
                if (requestJson == null) {
                    return ZimbraTopDocs.create(scoreDocs.size(), scoreDocs);
                }
            } catch (JSONException e) {
                ZimbraLog.index.debug("ElasticSearchIndex search - problem creating JSON for Query", e);
                return ZimbraTopDocs.create(scoreDocs.size(), scoreDocs);
            }
            if (requestJson != null) {
                // Can also specify timeout, from and search_type
                String url = String.format("%s%s/_search?size=%d", indexUrl, indexType, n);
                try {
                    refreshIndexIfNecessary();
                    // Both HTTP GET and HTTP POST can be used to execute search with body.
                    // Since not all clients support GET with body, POST is allowed as well.
                    HttpPost method = new HttpPost(ElasticSearchConnector.actualUrl(url));
                    method.setEntity(new StringEntity(requestJson.toString(),
                                MimeConstants.CT_APPLICATION_JSON, MimeConstants.P_CHARSET_UTF8));
                    ElasticSearchConnector connector = new ElasticSearchConnector();
                    int statusCode = connector.executeMethod(method);
                    if (statusCode == HttpStatus.SC_OK) {
                        JSONArray hits =  connector.getArrayAtJsonPath(new String[] {"hits", "hits"});
                        if (hits != null) {
                            for (int index = 0; index < hits.length(); index++) {
                                JSONObject hit = hits.optJSONObject(index);
                                if (hit != null) {
                                    String id = hit.getString("_id");
                                    if (id != null) {
                                        scoreDocs.add(ZimbraScoreDoc.create(new ZimbraElasticDocumentID(id)));
                                    }
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    ZimbraLog.index.error("Problem with query against index %s", url, e);
                } catch (JSONException e) {
                    ZimbraLog.index.debug("search - problem processing JSON Query response against index %s", url, e);
                }
            }
            return ZimbraTopDocs.create(scoreDocs.size(), scoreDocs);
        }

        /**
         * Search implementation with arbitrary sorting. Finds the top n hits for query, applying filter if non-null,
         * and sorting the hits by the criteria in sort.
         */
        @Override
        public ZimbraTopFieldDocs search(Query query, ZimbraTermsFilter filter, int n, Sort sort) throws IOException {
            List<ZimbraScoreDoc> scoreDocs = Lists.newArrayList();
            List<SortField> sortFields = Lists.newArrayList();
            if (sort != null) {
                Collections.addAll(sortFields, sort.getSort());
                for (SortField sortField: sortFields) {
                    sortField.getField();
                }
            }
            JSONObject requestJson = null;
            try {
                requestJson = searchQueryToJSON(query, filter, sortFields);
                if (requestJson == null) {
                    return ZimbraTopFieldDocs.create(0, scoreDocs, sortFields);
                }
            } catch (JSONException e) {
                ZimbraLog.index.debug("ElasticSearchIndex search - problem creating JSON for Query", e);
                return ZimbraTopFieldDocs.create(0, scoreDocs, sortFields);
            }
            if (requestJson != null) {
                // Can also specify timeout, from and search_type
                String url = String.format("%s%s/_search?size=%d", indexUrl, indexType, n);
                refreshIndexIfNecessary();
                try {
                    // Both HTTP GET and HTTP POST can be used to execute search with body.
                    // Since not all clients support GET with body, POST is allowed as well.
                    HttpPost method = new HttpPost(ElasticSearchConnector.actualUrl(url));
                    method.setEntity(new StringEntity(requestJson.toString(),
                                MimeConstants.CT_APPLICATION_JSON, MimeConstants.P_CHARSET_UTF8));
                    ElasticSearchConnector connector = new ElasticSearchConnector();
                    int statusCode = connector.executeMethod(method);
                    if (statusCode == HttpStatus.SC_OK) {
                        JSONArray hits =  connector.getArrayAtJsonPath(new String[] {"hits", "hits"});
                        if (hits != null) {
                            for (int index = 0; index < hits.length(); index++) {
                                JSONObject hit = hits.optJSONObject(index);
                                if (hit != null) {
                                    String id = hit.getString("_id");
                                    if (id != null) {
                                        scoreDocs.add(ZimbraScoreDoc.create(new ZimbraElasticDocumentID(id)));
                                    }
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    ZimbraLog.index.error("Problem with query against index %s", url, e);
                } catch (JSONException e) {
                    ZimbraLog.index.debug("search - problem processing JSON Query response against index %s", url, e);
                }
            }
            return ZimbraTopFieldDocs.create(scoreDocs.size(), scoreDocs, sortFields);
        }

        private JSONObject TermQueryToJSON(TermQuery query) throws JSONException {
            Term term = query.getTerm();
            JSONObject queryObj = new JSONObject();
            JSONObject termObj = new JSONObject();
            termObj.put(term.field(), term.text());
            queryObj.put("term", termObj);
            return queryObj;
        }

        private JSONObject PrefixQueryToJSON(PrefixQuery query) throws JSONException {
            Term term = query.getPrefix();
            JSONObject queryObj = new JSONObject();
            JSONObject termObj = new JSONObject();
            termObj.put(term.field(), term.text());
            queryObj.put("prefix", termObj);
            return queryObj;
        }

        private JSONObject WildcardQueryToJSON(WildcardQuery query) throws JSONException {
            Term term = query.getTerm();
            JSONObject queryObj = new JSONObject();
            JSONObject termObj = new JSONObject();
            termObj.put(term.field(), term.text());
            queryObj.put("wildcard", termObj);
            return queryObj;
        }

        /**
         *
             { "range" :
                 { "letter" :
                     { "from" : "beta", "to" : "omega", "include_lower" : true, "include_upper": false, "boost" : 2.0 }
                 }
             }
         */
        private JSONObject TermRangeQueryToJSON(TermRangeQuery query) throws JSONException {
            JSONObject queryObj = new JSONObject();
            JSONObject fieldObj = new JSONObject();
            JSONObject detailsObj = new JSONObject();
            if (null != query.getCollator()) {
                // Should not get here given current Zimbra functionality
                ZimbraLog.index.error("ElasticSearchIndex does not support TermRangeQueries with collators");
                return null;
            }
            detailsObj.put("from", query.getLowerTerm());
            detailsObj.put("to", query.getUpperTerm());
            detailsObj.put("include_lower", query.includesLower());
            detailsObj.put("include_upper", query.includesUpper());
            detailsObj.put("boost", query.getBoost());
            fieldObj.put(query.getField(), detailsObj);
            queryObj.put("range", fieldObj);
            return queryObj;
        }

        private JSONObject MultiTermQueryToJSON(MultiTermQuery query) throws JSONException {
            if (query instanceof PrefixQuery) {
                 return PrefixQueryToJSON((PrefixQuery) query);
            } else if (query instanceof WildcardQuery) {
                return WildcardQueryToJSON((WildcardQuery) query);
            } else if (query instanceof TermRangeQuery) {
                return TermRangeQueryToJSON((TermRangeQuery) query);
            } else {
                // Should not get here given current Zimbra functionality
                ZimbraLog.index.error("ElasticSearchIndex does not support search queries of type %s",
                        query.getClass().getName());
            }
            return null;
        }

        /**
             { "match_phrase" : { "message" : "this is a test" } }
         */
        private JSONObject PhraseQueryToJSON(PhraseQuery query) throws JSONException {
            String field = null;
            List<String> words = Lists.newArrayList();
            for (Term term :query.getTerms()) {
                field = term.field();  // Assumption, the same field is used in all terms!
                words.add(term.text());
            }
            JSONObject queryObj = new JSONObject();
            JSONObject fieldPhraseObj = new JSONObject();
            fieldPhraseObj.put(field, Joiner.on(' ').join(words));
            queryObj.put("match_phrase", fieldPhraseObj);
            return queryObj;
        }

        /**
         * e.g. :
         * "bool" : {
         *      "should" : [
         *           { "match_phrase" : { "l.content" : "from james hunt" } },
         *           { "match_phrase" : { "l.content" : "from jimmy hunt" } },
         *           { "match_phrase" : { "l.content" : "from jim hunt" } }
         *           ],
         *       "minimum_number_should_match" : 1,
         * }
         *
         */
        private JSONObject MultiPhraseQueryToJSON(MultiPhraseQuery query) throws JSONException {
            String field = null;
            List<List <String>> phrases = Lists.newArrayList();
            boolean firstWord = true;
            // e.g. (ignoring field) : [["from"], ["james","jimmy","jim"], ["hunt"]]
            for (Term[] wordsAtPos : query.getTermArrays()) {
                List<List <String>> phrasesSoFar = Lists.newArrayList();
                if (!firstWord) {
                    phrasesSoFar.addAll(phrases);
                    phrases = Lists.newArrayList();
                }
                for (Term term :wordsAtPos) {
                    if (firstWord) {
                        field = term.field();  // Assumption, the same field is used in all terms!
                        List<String> words = Lists.newArrayList();
                        words.add(term.text());
                        phrases.add(words);
                    } else {
                        for (List <String> phraseSoFar : phrasesSoFar) {
                            List<String> words = Lists.newArrayList();
                            words.addAll(phraseSoFar);
                            words.add(term.text());
                            phrases.add(words);
                        }
                    }
                }
                firstWord = false;
            }
            if (phrases.isEmpty()) {
                return null;
            }
            JSONObject queryObj = new JSONObject();
            JSONObject boolObj = new JSONObject();
            queryObj.put("bool", boolObj);
            JSONArray shoulds = new JSONArray();
            boolObj.put("should", shoulds);
            boolObj.put("minimum_number_should_match", "1");
            for (List <String> phrase : phrases) {
                JSONObject matchPhraseObj = new JSONObject();
                JSONObject fieldPhraseObj = new JSONObject();
                fieldPhraseObj.put(field, Joiner.on(' ').join(phrase));
                matchPhraseObj.put("match_phrase", fieldPhraseObj);
                shoulds.put(matchPhraseObj);
            }
            return queryObj;
        }

        /**
         * { "bool" : {
            "must" : { "term" : { "user" : "kimchy" } },
            "must_not" : { "range" : { "age" : { "from" : 10, "to" : 20 } } },
            "should" : [ { "term" : { "tag" : "wow" } }, { "term" : { "tag" : "elasticsearch" } } ],
            "minimum_number_should_match" : 1,
            "boost" : 1.0
            } }
         */
        private JSONObject BooleanQueryToJSON(BooleanQuery query) throws JSONException {
            JSONObject queryObj = new JSONObject();
            JSONObject boolObj = null;
            JSONArray musts = null;
            JSONArray mustNots = null;
            JSONArray shoulds = null;
            for (BooleanClause clause : query) {
                Query clauseQuery = clause.getQuery();
                JSONObject clauseObj = QueryToJSON(clauseQuery);
                if (clauseObj != null) {
                    Occur occur = clause.getOccur();
                    switch (occur) {
                    case MUST:
                        if (musts == null) {
                            musts = new JSONArray();
                        }
                        musts.put(clauseObj);
                        break;
                    case MUST_NOT:
                        if (mustNots == null) {
                            mustNots = new JSONArray();
                        }
                        mustNots.put(clauseObj);
                        break;
                    case SHOULD:
                        if (shoulds == null) {
                            shoulds = new JSONArray();
                        }
                        shoulds.put(clauseObj);
                        break;
                    }
                }
            }
            if ((musts != null) || (mustNots != null) || (shoulds != null)) {
                boolObj = new JSONObject();
                if (musts != null) {
                    boolObj.put("must", (musts.length() == 1) ? musts.get(0) : musts);
                }
                if (mustNots != null) {
                    boolObj.put("must_not", (mustNots.length() == 1) ? mustNots.get(0) : mustNots);
                }
                if (shoulds != null) {
                    boolObj.put("should", shoulds);
                    boolObj.put("should", (shoulds.length() == 1) ? shoulds.get(0) : shoulds);
                }
            }
            if (boolObj != null) {
                queryObj.put("bool", boolObj);
            }
            return queryObj;
        }

        private JSONObject QueryToJSON(Query query) throws JSONException {
            if (query instanceof TermQuery) {
                return TermQueryToJSON((TermQuery) query);
            } else if (query instanceof MultiTermQuery) {
                return MultiTermQueryToJSON((MultiTermQuery) query);
            } else if (query instanceof BooleanQuery) {
                return BooleanQueryToJSON((BooleanQuery) query);
            } else if (query instanceof MultiPhraseQuery) {
                return MultiPhraseQueryToJSON((MultiPhraseQuery) query);
            } else if (query instanceof PhraseQuery) {
                return PhraseQueryToJSON((PhraseQuery) query);
            } else {
                // Should not get here given current Zimbra functionality
                ZimbraLog.index.error("ElasticSearchIndex does not support search queries of type %s",
                        query.getClass().getName());
            }
            return null;
        }

        /**
         * e.g. { "terms" : { "l.mbox_blob_id" : ["258", "312"]}
         */
        private JSONObject FilterToJSON(ZimbraTermsFilter filter) throws JSONException {
            if (filter == null) {
                return null;
            }
            JSONObject filtersO = new JSONObject();
            for (Term term : filter.getTerms()) {
                filtersO.accumulate(term.field(), term.text());
            }
            return new JSONObject().put("terms", filtersO);
        }

        /**
         * Note:  Assumption is that only simple SortFields are used which specify a field name and an order of
         *        sorting.
         */
        private JSONArray SortToJSON(List<SortField> sortFields) throws JSONException {
            if ((sortFields == null) || sortFields.isEmpty()) {
                return null;
            }
            JSONArray sortJson = new JSONArray();
            for (SortField sortField : sortFields) {
                String field = sortField.getField();
                boolean reverse = sortField.getReverse();
                sortJson.put(new JSONObject().put(field, new JSONObject().put("order", reverse ? "desc" : "asc")));
            }
            return sortJson;
        }

        private JSONObject searchQueryToJSON(Query query, ZimbraTermsFilter filter, List<SortField> sortFields)
        throws JSONException {
            JSONObject requestJson = null;
            JSONObject queryJson = QueryToJSON(query);
            if (queryJson == null) {
                return null;
            }
            JSONObject filterJson = FilterToJSON(filter);
            if (filterJson == null) {
                requestJson = new JSONObject().put("query", queryJson);
            } else {
                 /* e.g.
                      {    "query" : {
                              "filtered" : {
                                  "query" : {"term":{"l.contactData":"zimbra.com"}},
                                  "filter" : { "terms" : { "l.mbox_blob_id" : ["258", "312"]} }
                              }
                          }
                      }
                  */
                JSONObject filteredJson = new JSONObject().put("query", queryJson).put("filter", filterJson);
                requestJson = new JSONObject().put("query", new JSONObject().put("filtered", filteredJson));
            }
            JSONArray sortJson = SortToJSON(sortFields);
            if (sortJson != null) {
                requestJson.put("sort", sortJson);
            }
            requestJson.put("fields", "*"); // Prevents _source in each hit.
            if (ZimbraLog.index.isTraceEnabled()) {
                requestJson.put("explain", true);
            }
            return requestJson;
        }
    }

    /**
     * Don't support Index optimization.  Rely on Elasticsearch to do appropriate house keeping itself.
     */
    @Override
    public void optimize() {
        return;
    }
}
