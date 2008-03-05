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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;


import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraLog;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbSearch;
import com.zimbra.cs.index.queryparser.ParseException;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.redolog.op.IndexItem;
import com.zimbra.cs.service.admin.ReIndex;
import com.zimbra.cs.store.Volume;

/**
 * Encapsulates the Index for one particular mailbox
 */
public final class MailboxIndex 
{
    public static ZimbraQueryResults search(SoapProtocol proto, OperationContext octxt, Mailbox mbox, SearchParams params, boolean textIndexOutOfSync) 
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
            if ("$chkblobs".equals(words[0].toLowerCase())) {
                mbox.getMailboxIndex().mLucene.checkBlobIds();
            } else if ("$reindex_all".equals(words[0].toLowerCase())) {
                Thread t = new ReIndex.ReIndexThread(mbox, null, null, null);
                t.start();
            } else
//              if ("$im_reg".equals(words[0])) {
//              if (words.length < 4)
//              throw ServiceException.FAILURE("USAGE: \"$im_reg service service_login_name service_login_password\"", null);
//              ServiceName service = ServiceName.valueOf(words[1]);
//              mbox.getPersona().gatewayRegister(service, words[2], words[3]);
//              } else if ("$im_unreg".equals(words[0])) {
//              if (words.length < 2)
//              throw ServiceException.FAILURE("USAGE: \"$im_unreg service service_login_name service_login_password\"", null);
//              ServiceName service = ServiceName.valueOf(words[1]);
//              mbox.getPersona().gatewayUnRegister(service);
//              } else
//              if ("$maint".equals(words[0])) {
//              MailboxManager.MailboxLock lock = MailboxManager.getInstance().beginMaintenance(mbox.getAccountId(), mbox.getId());
//              MailboxManager.getInstance().endMaintenance(lock, true, false);
//              } else {
//              throw ServiceException.FAILURE("Usage: \"$im_reg service name password\" or \"$im_unreg service\"", null);
                throw ServiceException.FAILURE("Unknown $ command", null);

//          return new EmptyQueryResults(params.getTypes(), params.getSortBy(), params.getMode());
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

        if (zq.countSearchTextOperations() > 0 && textIndexOutOfSync) {
            throw MailServiceException.TEXT_INDEX_OUT_OF_SYNC();
        }

        if (ZimbraLog.searchstats.isDebugEnabled()) {
            int textCount = zq.countSearchTextOperations();
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

        mLucene = sLuceneFactory.create(this, idxParentDir, mMailboxId);
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

    ILuceneIndex getLuceneIndex() {
        return mLucene;
    }

    private static ILuceneFactory sLuceneFactory = null;

    static {
        try {
            Class serialMergeScheduler = Class.forName("org.apache.lucene.index.SerialMergeScheduler");
            if (serialMergeScheduler != null) {
                Class fact = Class.forName("com.zimbra.cs.index.Lucene23Factory");
                if (fact != null) {
                    sLuceneFactory = (ILuceneFactory)fact.newInstance();
                    ZimbraLog.index.info("Using Lucene Jar version 2.3 or higher");
                }
            }
        } catch (ClassNotFoundException e) {
        } catch (Exception e) {
            ZimbraLog.index.info("Caught exception initializing Lucene Factory: ", e);
        }

        if (sLuceneFactory == null) {
            // couldn't find the SerialMergeFactory class, must be older Lucene jar
            try {
                Class fact = Class.forName("com.zimbra.cs.index.Lucene21Factory");
                if (fact != null) {
                    sLuceneFactory = (ILuceneFactory)fact.newInstance();
                    ZimbraLog.index.info("Using Lucene version before 2.3");
                }
            } catch (ClassNotFoundException e) {
            } catch (Exception e) {
                ZimbraLog.index.info("Caught exception initializing Lucene Factory: ", e);
            }
        }

        if (sLuceneFactory == null) {
            ZimbraLog.index.fatal("Fatal error - could not find Lucene Index factory");
        }
    }


    private ILuceneIndex mLucene;
    private ITextIndex mTextIndex;

    private int mMailboxId;
    private Mailbox mMailbox;
    private static Log sLog = LogFactory.getLog(MailboxIndex.class);

    public static void startup() {
        if (DebugConfig.disableIndexing)
            return;

        sLuceneFactory.startup();
    }

    public static void shutdown() {
        if (DebugConfig.disableIndexing)
            return;

        sLuceneFactory.shutdown();
    }

    public static void flushAllWriters() {
        if (DebugConfig.disableIndexing)
            return;

        sLuceneFactory.flushAllWriters();
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

        NONE("none", (byte)0x10),
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

    public void indexMailItem(Mailbox mbox, IndexItem redo, boolean deleteFirst, List<Document> docList, MailItem mi) 
    throws ServiceException {
        initAnalyzer(mbox);
        synchronized(getLock()) {
            int indexId = mi.getIndexId();
            try {
                if (docList != null) {
                    Document[] docs = new Document[docList.size()];
                    docs = docList.toArray(docs);
                    mTextIndex.addDocument(redo, docs, indexId, mi.getDate(), mi.getSortSubject(), mi.getSortSender(), deleteFirst);
                }
            } catch (IOException e) {
                throw ServiceException.FAILURE("indexMailItem caught IOException", e);
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
