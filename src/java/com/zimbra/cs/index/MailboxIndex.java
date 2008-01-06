/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jul 26, 2004
 */
package com.zimbra.cs.index;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.internet.MimeMessage;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraLog;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbSearch;
//import com.zimbra.cs.im.interop.Interop.ServiceName;
import com.zimbra.cs.index.queryparser.ParseException;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Note;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedDocument;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.redolog.op.IndexItem;
import com.zimbra.cs.store.Volume;
import com.zimbra.cs.util.JMSession;

/**
 * Encapsulates the Index for one particular mailbox
 */
public final class MailboxIndex 
{
    public static ZimbraQueryResults search(SoapProtocol proto, OperationContext octxt, Mailbox mbox, SearchParams params) 
    throws IOException, ParseException, ServiceException {
        
        if (ZimbraLog.index.isDebugEnabled()) {
            ZimbraLog.index.debug("SearchRequest: "+params.getQueryStr());
        }

        //
        // Testing hacks
        // 
        String qs = params.getQueryStr();
        if (qs.startsWith("$")) {
            String[] words = qs.split(" ");
//            if ("$im_reg".equals(words[0])) {
//                if (words.length < 4)
//                    throw ServiceException.FAILURE("USAGE: \"$im_reg service service_login_name service_login_password\"", null);
//                ServiceName service = ServiceName.valueOf(words[1]);
//                mbox.getPersona().gatewayRegister(service, words[2], words[3]);
//            } else if ("$im_unreg".equals(words[0])) {
//                if (words.length < 2)
//                    throw ServiceException.FAILURE("USAGE: \"$im_unreg service service_login_name service_login_password\"", null);
//                ServiceName service = ServiceName.valueOf(words[1]);
//                mbox.getPersona().gatewayUnRegister(service);
//            } else
                if ("$maint".equals(words[0])) {
                MailboxManager.MailboxLock lock = MailboxManager.getInstance().beginMaintenance(mbox.getAccountId(), mbox.getId());
                MailboxManager.getInstance().endMaintenance(lock, true, false);
            } else {
                throw ServiceException.FAILURE("Usage: \"$im_reg service name password\" or \"$im_unreg service\"", null); 
        }
                
            return new EmptyQueryResults(params.getTypes(), params.getSortBy(), params.getMode());
            }			
        
        //
        // calendar expansions
        //
        if ((params.getCalItemExpandStart() > 0) || (params.getCalItemExpandEnd() > 0)) {
            StringBuilder toAdd = new StringBuilder();
            toAdd.append('(').append(qs).append(')');
            if (params.getCalItemExpandStart() > 0) 
                toAdd.append(" appt-end:>=").append(params.getCalItemExpandStart());
            if (params.getCalItemExpandEnd() > 0)
                toAdd.append(" appt-start:<=").append(params.getCalItemExpandEnd());
            qs = toAdd.toString();
            params.setQueryStr(qs);
        }
        
        // handle special-case Task-only sorts: convert them to a "normal sort"
        //     and then re-sort them at the end
        boolean isTaskSort = false;
        SortBy originalSort = params.getSortBy();
        switch (originalSort) {
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
    }
        
        ZimbraQuery zq = new ZimbraQuery(mbox, params);

        if (ZimbraLog.searchstats.isDebugEnabled()) {
            int textCount = zq.countSearchTextParts();
            ZimbraLog.searchstats.debug("Executing search with ["+textCount+"] text parts");
        }
        
        try {
            ZimbraQueryResults results = zq.execute(octxt, proto);
            
            if (isTaskSort) {
                results = new TaskSortingQueryResults(results, originalSort);
            }			
            return results;
        } catch (IOException e) {
            zq.doneWithQuery();
            throw e;
        } catch (ServiceException e) {
            zq.doneWithQuery();
            throw e;
        } catch (Throwable t) {
            zq.doneWithQuery();
            throw ServiceException.FAILURE("Caught "+t.getMessage(), t);
        }
    }

    /**
     * @param fieldName - a lucene field (e.g. LuceneFields.L_H_CC)
     * @param collection - Strings which correspond to all of the domain terms stored in a given field.
     * @throws IOException
     */
    public void getDomainsForField(String fieldName, Collection<String> collection) throws IOException
    {
        mTextIndex.getDomainsForField(fieldName, collection);
    }

    /**
     * @param collection - Strings which correspond to all of the attachment types in the index
     * @throws IOException
     */
    public void getAttachments(Collection<String> collection) throws IOException
    {
        mTextIndex.getAttachments(collection);
    }

    public void getObjects(Collection<String> collection) throws IOException
    {
        mTextIndex.getObjects(collection);
    }


    /**
     * Force all outstanding index writes to go through.  
     * This API should be called when the system detects that it has free time.
     */
    public void flush() {
        mTextIndex.flush();
    }

    /**
     * @param itemIds array of itemIds to be deleted
     * 
     * @return an array of itemIds which HAVE BEEN PROCESSED.  If returned.length == 
     * itemIds.length then you can assume the operation was completely successful
     * 
     * @throws IOException on index open failure, nothing processed.
     */
    public int[] deleteDocuments(int itemIds[]) throws IOException {
        return mTextIndex.deleteDocuments(itemIds);
    }

    public String toString() {
        StringBuffer ret = new StringBuffer("MailboxIndex(");
        ret.append(mMailboxId);
        ret.append(")");
        return ret.toString();
    }

    public MailboxIndex(Mailbox mbox, String root) throws ServiceException {
        int mailboxId = mbox.getId();

        mMailboxId = mailboxId;
        mMailbox = mbox;

        Volume indexVol = Volume.getById(mbox.getIndexVolume());
        String idxParentDir = indexVol.getMailboxDir(mailboxId, Volume.TYPE_INDEX);

        mLucene = new LuceneIndex(this, idxParentDir, mMailboxId);
        mTextIndex = mLucene;

        String analyzerName = mbox.getAccount().getAttr(Provisioning.A_zimbraTextAnalyzer, null);

        if (analyzerName != null)
            mAnalyzer = ZimbraAnalyzer.getAnalyzer(analyzerName);
        else
            mAnalyzer = ZimbraAnalyzer.getDefaultAnalyzer();
        
        sLog.info("Initialized Index for mailbox " + mailboxId+" directory: "+mTextIndex.toString()+" Analyzer="+mAnalyzer.toString());
    }

    TextQueryOperation createTextQueryOperation() {
        return LuceneQueryOperation.doCreate();
        }
    
    LuceneIndex getLuceneIndex() {
        return mLucene;
    }

    private LuceneIndex mLucene;
    private ITextIndex mTextIndex;

    private int mMailboxId;
    private Mailbox mMailbox;
    private static Log sLog = LogFactory.getLog(MailboxIndex.class);

    public static void startup() {
        if (DebugConfig.disableIndexing)
            return;

        LuceneIndex.startup();
        }

    public static void shutdown() {
        if (DebugConfig.disableIndexing)
            return;

        LuceneIndex.shutdown();
    }
    
    public static void flushAllWriters() {
        if (DebugConfig.disableIndexing)
            return;
        
        LuceneIndex.flushAllWriters();
    }
        
    private Analyzer mAnalyzer = null;
        
    boolean curThreadHoldsLock() {
        return Thread.holdsLock(getLock());
    }

    /**
     * Load the Analyzer for this index, using the default Zimbra analyzer or a custom user-provided
     * analyzer specified by the key Provisioning.A_zimbraTextAnalyzer
     * 
     * @param mbox
     * @throws ServiceException
     */
    public void initAnalyzer(Mailbox mbox) throws ServiceException {
        // per bug 11052, must always lock the Mailbox before the MailboxIndex, and since
        // mbox.getAccount() is synchronized, we must lock here.
        synchronized (mbox) {
            synchronized (getLock()) {
                String analyzerName = mbox.getAccount().getAttr(Provisioning.A_zimbraTextAnalyzer, null);

                if (analyzerName != null)
                    mAnalyzer = ZimbraAnalyzer.getAnalyzer(analyzerName);
                else
                    mAnalyzer = ZimbraAnalyzer.getDefaultAnalyzer();
            }
        }
    }

    public Analyzer getAnalyzer() {
        synchronized(getLock()) {        
            return mAnalyzer;
        }
    }
    
    /******************************************************************************
     *
     *  Index Search Results
     *  
     ********************************************************************************/
    // What level of result grouping do we want?  ConversationResult, MessageResult, or DocumentResult?
    public static final int FIRST_SEARCH_RETURN_NUM = 1;
    public static final int SEARCH_RETURN_CONVERSATIONS = 1;
    public static final int SEARCH_RETURN_MESSAGES      = 2;
    public static final int SEARCH_RETURN_DOCUMENTS     = 3;
    public static final int LAST_SEARCH_RETURN_NUM = 3;

    public static final String GROUP_BY_CONVERSATION = "conversation";
    public static final String GROUP_BY_MESSAGE      = "message";
    public static final String GROUP_BY_NONE         = "none";

    public static final String SEARCH_FOR_APPOINTMENTS = "appointment";
    public static final String SEARCH_FOR_CONTACTS = "contact";
    public static final String SEARCH_FOR_CONVERSATIONS = "conversation";
    public static final String SEARCH_FOR_DOCUMENTS = "document";
    public static final String SEARCH_FOR_MESSAGES = "message";
    public static final String SEARCH_FOR_NOTES = "note";
    public static final String SEARCH_FOR_TASKS = "task";
    public static final String SEARCH_FOR_WIKI = "wiki";

    public static final String SEARCH_FOR_EVERYTHING = SEARCH_FOR_APPOINTMENTS + ',' + SEARCH_FOR_CONTACTS + ',' +
                                                       SEARCH_FOR_DOCUMENTS + ',' + SEARCH_FOR_MESSAGES + ',' +
                                                       SEARCH_FOR_NOTES + ',' + SEARCH_FOR_TASKS + ',' +
                                                       SEARCH_FOR_WIKI;

    public static enum SortBy {
        DATE_ASCENDING  ("dateAsc",  (byte) (DbSearch.SORT_BY_DATE | DbSearch.SORT_ASCENDING)), 
        DATE_DESCENDING ("dateDesc", (byte) (DbSearch.SORT_BY_DATE | DbSearch.SORT_DESCENDING)),
        SUBJ_ASCENDING  ("subjAsc",  (byte) (DbSearch.SORT_BY_SUBJECT | DbSearch.SORT_ASCENDING)),
        SUBJ_DESCENDING ("subjDesc", (byte) (DbSearch.SORT_BY_SUBJECT | DbSearch.SORT_DESCENDING)),
        NAME_ASCENDING  ("nameAsc",  (byte) (DbSearch.SORT_BY_SENDER | DbSearch.SORT_ASCENDING)),
        NAME_DESCENDING ("nameDesc", (byte) (DbSearch.SORT_BY_SENDER | DbSearch.SORT_DESCENDING)),
        SCORE_DESCENDING("score",    (byte) 0),

        // special TASK-only sorts
        TASK_DUE_ASCENDING("taskDueAsc", (byte)0),
        TASK_DUE_DESCENDING("taskDueDesc", (byte)0),
        TASK_STATUS_ASCENDING("taskStatusAsc", (byte)0),
        TASK_STATUS_DESCENDING("taskStatusDesc", (byte)0),
        TASK_PERCENT_COMPLETE_ASCENDING("taskPercCompletedAsc", (byte)0),
        TASK_PERCENT_COMPLETE_DESCENDING("taskPercCompletedDesc", (byte)0),
        ;

        static HashMap<String, SortBy> sNameMap = new HashMap<String, SortBy>();

        static {
            for (SortBy s : SortBy.values()) 
                sNameMap.put(s.mName.toLowerCase(), s);
        }

        byte mSort;
        String mName;

        SortBy(String str, byte sort) {
            mName = str;
            mSort = sort;
        }

        public String toString() { return mName; }

        public byte getDbMailItemSortByte() {
            return mSort;
        }

        public boolean isDescending() {
            return (mSort & DbSearch.SORT_ASCENDING) == 0;
        }

        public static SortBy lookup(String str) {
            if (str != null)
                return sNameMap.get(str.toLowerCase());
            else
                return null;
        }
    }

    public static byte[] parseTypesString(String groupBy) throws ServiceException
    {
        String[] strs = groupBy.split("\\s*,\\s*");

        byte[] types = new byte[strs.length]; 
        for (int i = 0; i < strs.length; i++) {
            if (SEARCH_FOR_CONVERSATIONS.equals(strs[i])) {
                types[i] = MailItem.TYPE_CONVERSATION;
            } else if (SEARCH_FOR_MESSAGES.equals(strs[i])) {
                types[i] = MailItem.TYPE_MESSAGE;
            } else if (GROUP_BY_NONE.equals(strs[i])) {
                types[i] = 0;
            } else if (SEARCH_FOR_CONTACTS.equals(strs[i])) {
                types[i] = MailItem.TYPE_CONTACT;
            } else if (SEARCH_FOR_APPOINTMENTS.equals(strs[i])) {
                types[i] = MailItem.TYPE_APPOINTMENT;
            } else if (SEARCH_FOR_TASKS.equals(strs[i])) {
                types[i] = MailItem.TYPE_TASK;
            } else if (SEARCH_FOR_NOTES.equals(strs[i])) {
                types[i] = MailItem.TYPE_NOTE;
            } else if (SEARCH_FOR_WIKI.equals(strs[i])) {
                types[i] = MailItem.TYPE_WIKI;
            } else if (SEARCH_FOR_DOCUMENTS.equals(strs[i])) {
                types[i] = MailItem.TYPE_DOCUMENT;
            } else 
                throw ServiceException.INVALID_REQUEST("unknown groupBy: "+strs[i], null);
        }

        return types;
    }

    public void deleteIndex() throws IOException
    {
        mTextIndex.deleteIndex();
            }

    /**
     * Entry point for Redo-logging system only.  Everybody else should use MailItem.reindex()
     * 
     * @throws ServiceException
     */
    public void redoIndexItem(Mailbox mbox, boolean deleteFirst, int itemId, byte itemType, long timestamp, boolean noRedo)
    throws IOException, ServiceException {
        MailItem item;
        try {
            item = mbox.getItemById(null, itemId, itemType);
        } catch (MailServiceException.NoSuchItemException e) {
            // Because index commits are batched, during mailbox restore
            // it's possible to see the commit record of indexing operation
            // after the delete operation on the item being indexed.
            // (delete followed by edit, for example)
            // We can't distinguish this legitimate case from a case of
            // really missing the item being indexed due to unexpected
            // problem.  So just ignore the NoSuchItemException.
            return;
        }

        IndexItem redo = null;
        if (!noRedo) {
            redo = new IndexItem(mbox.getId(), item.getId(), itemType, deleteFirst);
            redo.start(System.currentTimeMillis());
            redo.log();
            redo.allowCommit();
        }
        switch (itemType) {
            case MailItem.TYPE_APPOINTMENT:
            case MailItem.TYPE_TASK:
                CalendarItem ci = (CalendarItem)item;
                ci.reindex(redo, deleteFirst, null);
                break;
            case MailItem.TYPE_DOCUMENT:
            case MailItem.TYPE_WIKI:
                try {
                    com.zimbra.cs.mailbox.Document document = (com.zimbra.cs.mailbox.Document)item;
                    ParsedDocument pd = new ParsedDocument(document.getBlob(),
                                document.getName(), 
                                document.getContentType(),
                                timestamp,
                                document.getCreator());
                    indexDocument(mbox, redo, deleteFirst, pd, document);
                } catch (IOException ioe) {
                    throw ServiceException.FAILURE("indexDocument caught Exception", ioe);
                }
                break;
            case MailItem.TYPE_CHAT:
            case MailItem.TYPE_MESSAGE:
                Message msg = (Message) item;
                InputStream is = msg.getContentStream();
                MimeMessage mm;
                try {
                    mm = new Mime.FixedMimeMessage(JMSession.getSession(), is);
                    ParsedMessage pm = new ParsedMessage(mm, timestamp, mbox.attachmentsIndexingEnabled());
                    indexMessage(mbox, redo, deleteFirst, pm, msg);
                } catch (Throwable t) {
                    sLog.warn("Skipping indexing; Unable to parse message " + itemId + ": " + t.toString(), t);
                    // Eat up all errors during message analysis.  Throwing
                    // anything here will force server halt during crash
                    // recovery.  Because we can't possibly predict all
                    // data-dependent message parse problems, we opt to live
                    // with unindexed messages rather than P1 support calls.

                    // Write abort record for this item, to prevent repeat calls
                    // to index this unindexable item.
                    if (redo != null)
                        redo.abort();
                } finally {
                    is.close();
                }
                break;
            case MailItem.TYPE_CONTACT:
                indexContact(mbox, redo, deleteFirst, (Contact) item);
                break;
            case MailItem.TYPE_NOTE:
                indexNote(mbox, redo, deleteFirst, (Note) item);
                break;
            default:
                if (redo != null)
                    redo.abort();
            throw ServiceException.FAILURE("Invalid item type for indexing: type=" + itemType, null);
        }
    }

    public void indexCalendarItem(Mailbox mbox, IndexItem redo, boolean deleteFirst, 
        CalendarItem item, List<Document> docList, long date) throws ServiceException {
        
        initAnalyzer(mbox);
        synchronized(getLock()) {
            int indexId = item.getIndexId();
            
            try {
                if (docList != null) {
                    Document[] docs = new Document[docList.size()];
                    docs = docList.toArray(docs);
                    mTextIndex.addDocument(redo, docs, indexId, date, item, deleteFirst);
                }
            } catch (IOException e) {
                throw ServiceException.FAILURE("indexMessage caught IOException", e);
            }
        }
    }
    
    /**
     * Index a message in the specified mailbox.
     * @param mailboxId
     * @param messageId
     * @param pm
     * @throws ServiceException
     */
    public void indexMessage(Mailbox mbox, IndexItem redo, boolean deleteFirst, ParsedMessage pm, Message msg)
    throws ServiceException {
        initAnalyzer(mbox);
        synchronized(getLock()) {
            int indexId = msg.getIndexId();

            try {
                List<Document> docList = pm.getLuceneDocuments();
                if (docList != null) {
                    Document[] docs = new Document[docList.size()];
                    docs = docList.toArray(docs);
                    mTextIndex.addDocument(redo, docs, indexId, pm.getReceivedDate(), msg, deleteFirst);
                }
            } catch (IOException e) {
                throw ServiceException.FAILURE("indexMessage caught IOException", e);
            }
        }
    }

    private static void appendContactField(StringBuilder sb, Contact contact, String fieldName) {
        String s = contact.get(fieldName);
        if (s!= null) {
            sb.append(s).append(' ');
        }
    }

    /**
     * Index a Contact in the specified mailbox.
     * @param deleteFirst if TRUE then we must delete the existing index records before we index
     * @param mailItemId
     * @param contact
     * @throws ServiceException
     */
    public void indexContact(Mailbox mbox, IndexItem redo, boolean deleteFirst, Contact contact) throws ServiceException {
        initAnalyzer(mbox);
        synchronized(getLock()) {        
            if (sLog.isDebugEnabled()) {
                sLog.debug("indexContact("+contact+")");
            }
            try {
                int indexId = contact.getIndexId();
                
                StringBuffer contentText = new StringBuffer();
                StringBuffer fieldText = new StringBuffer();
                
                Map<String, String> m = contact.getFields();
                for (Map.Entry<String, String> entry : m.entrySet()) {
                    contentText.append(entry.getValue()).append(' ');

                    String fieldTextToAdd = entry.getKey() + ":" + entry.getValue() + "\n";
//                    fieldText.append(entry.getKey()).append(':').append(entry.getValue()).append('\n');
                    fieldText.append(fieldTextToAdd);
                }

                Document doc = new Document();

                StringBuilder searchText = new StringBuilder();
                appendContactField(searchText, contact, Contact.A_company);
                appendContactField(searchText, contact, Contact.A_firstName);
                appendContactField(searchText, contact, Contact.A_lastName);
                appendContactField(searchText, contact, Contact.A_nickname);

                StringBuilder emailStrBuf = new StringBuilder();
                List<String> emailList = contact.getEmailAddresses();
                for (String cur : emailList) {
                    emailStrBuf.append(cur).append(' ');
                }

                String emailStr = emailStrBuf.toString();

                contentText.append(ZimbraAnalyzer.getAllTokensConcatenated(LuceneFields.L_H_TO, emailStr));
                searchText.append(ZimbraAnalyzer.getAllTokensConcatenated(LuceneFields.L_H_TO, emailStr));
                
                /* put the email addresses in the "To" field so they can be more easily searched */
                doc.add(new Field(LuceneFields.L_H_TO, emailStr,  Field.Store.NO, Field.Index.TOKENIZED));

                /* put the name in the "From" field since the MailItem table uses 'Sender'*/
                doc.add(new Field(LuceneFields.L_H_FROM, contact.getSender(),  Field.Store.NO, Field.Index.TOKENIZED));
                /* bug 11831 - put contact searchable data in its own field so wildcard search works better  */
                doc.add(new Field(LuceneFields.L_CONTACT_DATA, searchText.toString(), Field.Store.NO, Field.Index.TOKENIZED));
                doc.add(new Field(LuceneFields.L_CONTENT, contentText.toString(), Field.Store.NO, Field.Index.TOKENIZED));
                doc.add(new Field(LuceneFields.L_H_SUBJECT, contact.getSubject(), Field.Store.NO, Field.Index.TOKENIZED));
                doc.add(new Field(LuceneFields.L_PARTNAME, LuceneFields.L_PARTNAME_CONTACT, Field.Store.YES, Field.Index.UN_TOKENIZED));

                /* add key:value pairs to the structured FIELD lucene field */
                doc.add(new Field(LuceneFields.L_FIELD, fieldText.toString(), Field.Store.NO, Field.Index.TOKENIZED));

                mTextIndex.addDocument(redo, doc, indexId, contact.getDate(), contact, deleteFirst);

            } catch (IOException ioe) {
                throw ServiceException.FAILURE("indexContact caught IOException", ioe);
            }
        }        
    }    

    /**
     * Index a Note in the specified mailbox.
     * 
     * @throws ServiceException
     */
    public void indexNote(Mailbox mbox, IndexItem redo, boolean deleteFirst, Note note)
    throws ServiceException {
        initAnalyzer(mbox);
        synchronized(getLock()) {        
            if (sLog.isDebugEnabled()) {
                sLog.debug("indexNote("+note+")");
            }
            try {
                String toIndex = note.getText();
                int indexId = note.getIndexId(); 

                if (sLog.isDebugEnabled()) {
                    sLog.debug("Note value=\""+toIndex+"\"");
                }

                Document doc = new Document();
                doc.add(new Field(LuceneFields.L_CONTENT, toIndex, Field.Store.NO, Field.Index.TOKENIZED));
                doc.add(new Field(LuceneFields.L_H_SUBJECT, toIndex, Field.Store.NO, Field.Index.TOKENIZED));
                doc.add(new Field(LuceneFields.L_PARTNAME, LuceneFields.L_PARTNAME_NOTE, Field.Store.YES, Field.Index.UN_TOKENIZED));
                mTextIndex.addDocument(redo, doc, indexId, note.getDate(), note, deleteFirst);

            } catch (IOException e) {
                throw ServiceException.FAILURE("indexNote caught IOException", e);
            }
        }
    }    

    public void indexDocument(Mailbox mbox, IndexItem redo, boolean deleteFirst, 
                ParsedDocument pd, com.zimbra.cs.mailbox.Document doc)  throws ServiceException {
        initAnalyzer(mbox);
        synchronized(getLock()) {        
            try {
                int indexId = doc.getIndexId();
                mTextIndex.addDocument(redo, pd.getDocument(), indexId, pd.getCreatedDate(), doc, deleteFirst);
            } catch (IOException e) {
                throw ServiceException.FAILURE("indexDocument caught Exception", e);
            }
        }
    }
    
        /**
     * @return TRUE if all tokens were expanded or FALSE if no more tokens could be expanded
         */
    boolean expandWildcardToken(Collection<String> toRet, String field, String token, int maxToReturn) throws ServiceException 
    {
        return mTextIndex.expandWildcardToken(toRet, field, token, maxToReturn);
        }
        
    List<SpellSuggestQueryInfo.Suggestion> suggestSpelling(String field, String token) throws ServiceException {
        return mTextIndex.suggestSpelling(field, token);
                    }

                
    final Object getLock() {
            return mMailbox;
                        }
}
