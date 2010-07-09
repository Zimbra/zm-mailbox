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

/*
 * Created on Jul 20, 2004
 */
package com.zimbra.cs.index;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.apache.lucene.document.DateField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.index.queryparser.ParseException;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Mailbox.SearchResultMode;
import com.zimbra.cs.tcpserver.ProtocolHandler;
import com.zimbra.cs.tcpserver.TcpServer;
import com.zimbra.cs.tcpserver.TcpServerInputStream;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.*;

public class IndexEditor {

    static final int SEARCH_RETURN_CONVERSATIONS = 1;
    static final int SEARCH_RETURN_MESSAGES = 2;
    static final int SEARCH_RETURN_DOCUMENTS = 3;
    
    private static SortBy sortOrder = SortBy.DATE_DESCENDING;
    private BufferedReader inputReader = null;
    private PrintStream outputStream = null;

    private static Log mLog = LogFactory.getLog(IndexEditor.class);

    public void deleteIndex(long mailboxId) throws ServiceException {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mailboxId);
        MailboxIndex mi = mbox.getMailboxIndex();
        try {
            mi.deleteIndex();
        } catch (IOException e) {
            throw ServiceException.FAILURE("Caught IOException", e);
        }
    }

    public void reIndexAll() {
        MailboxManager mmgr;
        try {
            mmgr = MailboxManager.getInstance();
        } catch (ServiceException e) {
            ZimbraLog.index.error("could not retrieve mailbox manager; aborting reindex", e);
            return;
        }
        long ids[] = mmgr.getMailboxIds();
        for (int i = 0; i < ids.length; i++) {
            mLog.info("Mailbox "+ids[i]+"\n");
            try {
                Mailbox mbx = mmgr.getMailboxById(ids[i]);
                mbx.reIndex(null, null, null, false);
            } catch (ServiceException e) {
                mLog.info("Exception ReIndexing " + ids[i], e);
            }
        }
    }

    public void reIndex(long mailboxId) {
        MailboxIndex midx = null;
        try {
            Mailbox mbx = MailboxManager.getInstance().getMailboxById(mailboxId);
            mbx.reIndex(null, null, null, false);
        } catch(Exception e) {
            outputStream.println("Re-index FAILED with " + ExceptionToString.ToString(e));
        } finally {
            if (midx != null) {
                midx.flush();
            }
        }
    }

    public void checkIndex(long mailboxId, boolean repair) {
    }

    public interface QueryRunner
    {
        public ZimbraQueryResults runQuery(String qstr, byte[] types, SortBy sortBy) throws IOException, ParseException, MailServiceException, ServiceException;
    }

    public class SingleQueryRunner implements QueryRunner
    {
        long mMailboxId;

        SingleQueryRunner(long mailboxId) throws ServiceException {
            mMailboxId = mailboxId;
        }

        public ZimbraQueryResults runQuery(String qstr, byte[] types, SortBy sortBy) throws IOException, MailServiceException, ParseException, ServiceException
        {
            Mailbox mbox = MailboxManager.getInstance().getMailboxById(mMailboxId);
            SearchParams params = new SearchParams();
            params.setQueryStr(qstr);
            params.setTypes(types);
            params.setSortBy(sortBy);
            params.setOffset(0);
            params.setLimit(100);
            params.setPrefetch(true);
            params.setMode(SearchResultMode.NORMAL);
            ZimbraQuery zq = new ZimbraQuery(null, SoapProtocol.Soap12, mbox, params);
            return zq.execute(/*null, SoapProtocol.Soap12*/);
        }
    }

    public class MultiQueryRunner implements QueryRunner
    {
        long mMailboxId[];

        MultiQueryRunner(long[] mailboxId) throws ServiceException {
            mMailboxId = new long[mailboxId.length];
            for (int i = 0; i < mailboxId.length; i++) {
                mMailboxId[i] = mailboxId[i];
            }
        }

        MultiQueryRunner(ArrayList /* Long */ mailboxId) throws ServiceException {
            mMailboxId = new long[mailboxId.size()];
            for (int i = 0; i < mailboxId.size(); i++) {
                mMailboxId[i] = ((Long) mailboxId.get(i)).longValue();
            }
        }


        public ZimbraQueryResults runQuery(String qstr, byte[] types, SortBy sortBy) throws IOException, MailServiceException, ParseException, ServiceException
        {
            ZimbraQueryResults[] res = new ZimbraQueryResults[mMailboxId.length];
            for (int i = 0; i < mMailboxId.length; i++) {
                Mailbox mbox = MailboxManager.getInstance().getMailboxById(mMailboxId[i]);
                SearchParams params = new SearchParams();
                params.setQueryStr(qstr);
                params.setTypes(types);
                params.setSortBy(sortBy);
                params.setOffset(0);
                params.setLimit(100);
                params.setPrefetch(true);
                params.setMode(SearchResultMode.NORMAL);
                ZimbraQuery zq = new ZimbraQuery(null, SoapProtocol.Soap12, mbox, params);
                res[i] = zq.execute(/*null, SoapProtocol.Soap12*/);
            }
            return HitIdGrouper.Create(new MultiQueryResults(res, sortBy), sortBy);
        }
    }


    public void doQuery(QueryRunner runner, boolean dump, int groupBy) throws MailServiceException, IOException, ParseException, ServiceException
    {
        //        try {
        while(true) {
            outputStream.print("Query> ");
            String qstr = inputReader.readLine();
            if (qstr.equals("")) {
                return;
            }
            outputStream.println("\n\nTest 1: "+qstr);
            long startTime = System.currentTimeMillis();

            //ZimbraQuery zq = new ZimbraQuery(qstr, Mailbox.getMailboxById(mailboxId));
            //        		Hits hits = searcher.search(zq);

//          int groupBy = MailboxIndex.SEARCH_RETURN_MESSAGES;
//          if (conv) {
//          groupBy = MailboxIndex.SEARCH_RETURN_CONVERSATIONS;
//          }

            byte[] types = new byte[1];
            switch(groupBy) {
                case SEARCH_RETURN_CONVERSATIONS:
                    types[0]=MailItem.TYPE_CONVERSATION;
                    break;
                case SEARCH_RETURN_MESSAGES:
                    types[0]=MailItem.TYPE_MESSAGE;
                    break;
                default:
                    types[0]=0;
                break;
            }
            ZimbraQueryResults res = runner.runQuery(qstr, types, sortOrder);
            try {

                long endTime = System.currentTimeMillis();

                // compute numMessages the slow way, so we get a true count...for testing only!
                int numMessages = 0;
                if (false){
                    res.resetIterator();
                    ZimbraHit hit = res.getNext();
                    while (hit != null) {
                        numMessages++;
                        hit=res.getNext();
                    }
                }

                int HITS_PER_PAGE = 20;
//              if (conv) {
//              HITS_PER_PAGE = 20;
//              }

                int totalShown = 0;

                res.resetIterator();
                ZimbraHit hit = res.getNext();
                while (hit != null) {
                    for (int i = 0; (hit != null) && (i < HITS_PER_PAGE); i++) {
                        displayHit(hit, groupBy);
                        totalShown++;
                        hit = res.getNext();
                    }
                    if (hit != null) {
                        outputStream.print("more (y/n) ? ");
                        String line = inputReader.readLine();
                        if (line.length() == 0 || line.charAt(0) == 'n')
                            break;
                    }
                }
                outputStream.println("Query ran in " + (endTime-startTime) + " ms");
                outputStream.println("Displayed a total of " + totalShown + " Hits");
            } finally {
                res.doneWithSearchResults();
            }
        }
    }

    public void displayHit(ZimbraHit hit, int groupBy) {
        outputStream.print("HIT: ");
        if (groupBy == SEARCH_RETURN_CONVERSATIONS) {
            ConversationHit ch = (ConversationHit) hit;
            outputStream.println(ch.toString() + " \"  (" + ch.getNumMessageHits() + ")");
            Collection mhs = ch.getMessageHits();
            for (Iterator iter = mhs.iterator(); iter.hasNext(); )
            {
                MessageHit mh = (MessageHit)iter.next();
                outputStream.println("\t" + mh.toString());
            }
        } else {
            if (hit instanceof MessageHit) {
                MessageHit mh = (MessageHit)hit;
                outputStream.println(mh.toString());
            } else if (hit instanceof MessagePartHit){
                MessagePartHit mph = (MessagePartHit)hit;
                outputStream.println(mph.toString());
            } else {
                outputStream.println(hit.toString());
            }
        }
    }


//  public static void newQuery(long mailboxId) throws IOException, ParseException
//  {
//  ZimbraSearcher searcher = Indexer.GetInstance().getSearcher(mailboxId);
//  try {
//  while(true) {
//  outputStream.print("Query> ");
//  String qstr = in.readLine();
//  if (qstr.equals("")) {
//  return;
//  }
//  outputStream.println("\n\nTest 1: "+qstr);
//  ZimbraQuery zq = new ZimbraQuery(qstr);
//  SimpleQueryResults hits = searcher.search(zq,1,101);
//  try {

//  if (hits==null) {
//  continue;
//  }
//  outputStream.println(hits.numHits() + " total matching documents\n");

//  final int HITS_PER_PAGE = 10;
//  for (int start = 0; start < hits.numHits(); start += HITS_PER_PAGE) {
//  int end = Math.min(hits.numHits(), start + HITS_PER_PAGE);
//  for (int i = start; i < end; i++) {
//  //        				Document doc = hits.doc(i);
//  //        				String path = doc.get(LuceneFields.L_MAILBOX_BLOB_ID) + " ";
//  //        				String dateStr = doc.get(LuceneFields.L_DATE);
//  //        				if (dateStr != null) { 
//  //        					path += DateField.stringToDate(dateStr)+" ";
//  //        				}
//  //        				String sizeStr = doc.get(LuceneFields.L_SIZE);
//  //        				if (sizeStr != null) {
//  //        					path+=sizeStr+" ";
//  //        				}
//  //        				path += doc.get(LuceneFields.L_H_SUBJECT);
//  //        				int hitNum = i + 1;
//  //        				if (path != null) {
//  //        					outputStream.println(hitNum + ". " + path);
//  //        				} else {
//  //        					outputStream.println(hitNum + ". " + "No path for this document");
//  //        				}
//  }

//  if (hits.numHits() > end) {
//  outputStream.print("more (y/n) ? ");
//  String line = in.readLine();
//  if (line.length() == 0 || line.charAt(0) == 'n')
//  break;
//  }
//  }
//  } finally {
//  searcher.doneWithSearchResults(hits);
//  }
//  }
//  } catch(Exception e) {
//  e.printStackTrace();
//  } finally {
//  searcher.close();
//  }
//  }


    public void dumpFields(long mailboxId) throws IOException, ServiceException
    {
////      MailboxIndex searcher = Indexer.GetInstance().getMailboxIndex(mailboxId);
//        MailboxIndex searcher = MailboxManager.getInstance().getMailboxById(mailboxId).getMailboxIndex();
//
//        if (searcher != null) {
//    //      try {
//            outputStream.println("\nFields\n------");
//            Collection c = searcher.getFieldNames();
//            Iterator iterator = c.iterator();
//            while (iterator.hasNext()) {
//                String str = iterator.next().toString();
//                if (str.length() > 0 && !str.equals("")) {
//                    outputStream.println(str);
//                }
//            }
//    //      } finally {
//    //      searcher.close();
//    //      }
//        }
    }

    public boolean confirm(String confirmString) {
        outputStream.println(confirmString);
        outputStream.print("Type YES to confirm: ");
        try {
            String s = inputReader.readLine();
            if (s.equals("YES")) {
                return true;
            }
        } catch(Exception e) {
            outputStream.print("Caught exception: "+ExceptionToString.ToString(e));
            // nothing
        }
        return false;
    }

    public static String Format(String s, int len) {
        StringBuffer toRet = new StringBuffer(len+1);
        int curOff = 0;
        if (s.length() < len) {
            for (curOff = 0; curOff < (len-s.length()); curOff++) {
                toRet.append(" ");
            }
        }

        int sOff = 0;
        for (; curOff < len; curOff++) {
            toRet.append(s.charAt(sOff));
            sOff++;
        }
        toRet.append("  ");
        return toRet.toString();
    }

    public void dumpDocument(Document d, boolean isDeleted)
    {
        if (isDeleted) {
            outputStream.print("DELETED ");
        }
        String subj, blobId;
        Field f;
        f = d.getField(LuceneFields.L_H_SUBJECT);
        if (f!=null) {
            subj = f.stringValue();
        } else {
            subj = "MISSING_SUBJECT";
        }
        f = d.getField(LuceneFields.L_MAILBOX_BLOB_ID);
        if (f!=null) {
            blobId = f.stringValue();
        } else {
            blobId = "MISSING";
        }
        String dateStr = d.get(LuceneFields.L_SORT_DATE);
        if (dateStr == null) {
            dateStr = "";
        } else {
            Date dt = DateField.stringToDate(dateStr);
            dateStr = dt.toString()+" ("+dt.getTime()+")";
        }
        String sizeStr = d.get(LuceneFields.L_SORT_SIZE);
        if (sizeStr == null) {
            sizeStr = "";
        }
        String part = d.get(LuceneFields.L_PARTNAME);
        if (part == null) {
            part = "NO_PART";
        }

        outputStream.println(Format(blobId,10) + Format(dateStr,45) + Format(part,10) + Format(sizeStr,10) + "\"" + subj + "\"");

        Field content = d.getField(LuceneFields.L_CONTENT);
        if (content != null) {
            outputStream.println("\t"+content.toString());
        }
    }

//    private class DocCallback extends MailboxIndex.DocEnumInterface
//    {
//        public void maxDocNo(int num)
//        {
//            outputStream.println("There are "+num+" documents in this index.");
//            outputStream.println("MB-BLOB-ID    DATE                                PART      SIZE  SUBJECT");
//            outputStream.println("----------------------------------------------------------------------------------------------------------------");
//        }
//
//        public boolean onDocument(Document d, boolean isDeleted) {
//            if (isDeleted) {
//                outputStream.print("DELETED ");
//            }
//            String subj, blobId;
//            Field f;
//            f = d.getField(LuceneFields.L_H_SUBJECT);
//            if (f!=null) {
//                subj = f.stringValue();
//            } else {
//                subj = "MISSING_SUBJECT";
//            }
//            f = d.getField(LuceneFields.L_MAILBOX_BLOB_ID);
//            if (f!=null) {
//                blobId = f.stringValue();
//            } else {
//                blobId = "MISSING";
//            }
//            String part = d.get(LuceneFields.L_PARTNAME);
//            if (part == null) {
//                part = "NULL_PART";
//            }
//
//            String dateStr = d.get(LuceneFields.L_SORT_DATE);
//            if (dateStr == null) {
//                dateStr = "";
//            } else {
//                dateStr = DateField.stringToDate(dateStr).toString();
//            }
//            String sizeStr = d.get(LuceneFields.L_SIZE);
//            if (sizeStr == null) {
//                sizeStr = "";
//            }
//
//            outputStream.println(Format(blobId,10) + Format(dateStr,30) + Format(part,10) + Format(sizeStr,10) + "\"" + subj + "\"");
//
//            return true;
//        }
//
//    }

    public void dumpAll(long mailboxId) throws IOException, ServiceException
    {
////      MailboxIndex searcher = Indexer.GetInstance().getMailboxIndex(mailboxId);
//        MailboxIndex searcher = MailboxManager.getInstance().getMailboxById(mailboxId).getMailboxIndex();
//        if (searcher != null) {
//    //      try {
//    //      int maxDoc = reader.maxDoc();
//    //      outputStream.println("There are "+maxDoc+" documents in this index.");
//            searcher.enumerateDocuments(new DocCallback());
//    //      } finally {
//    //      searcher.close();
//    //      }
//        }
    }

    public void dumpDocumentByMailItemId(long mailboxId, int mailItemId) throws ServiceException, IOException
    {
//        Term term = new Term(LuceneFields.L_MAILBOX_BLOB_ID, Integer.toString(mailItemId));
//        MailboxIndex idx = MailboxManager.getInstance().getMailboxById(mailboxId).getMailboxIndex();
//        RefCountedIndexSearcher searcher = null;
//
//        if (idx != null) {
//            try {
//                // Digression here -- find ALL documents for this blob, make sure
//                // that they all have the same sort field value
//                TermQuery q = new TermQuery(term);
//                searcher = idx.getCountedIndexSearcher();
//                Hits luceneHits = searcher.getSearcher().search(q);
//    
//                for (int i = 0; i < luceneHits.length(); i++) {
//                    Document curDoc = luceneHits.doc(i);
//                    dumpDocument(curDoc, false);
//                }
//            } finally {
//                if (searcher != null) {
//                    searcher.release();
//                }
//            }
//        }
    }

    private static int NumDigits(String s)
    {
        int ret = 0;
        char[] array = s.toCharArray();
        for (int i = 0; i < array.length; i++) {
            if (Character.isDigit(array[i])) {
                ret++;
            }
        }
        return ret;
    }


    public void dumpTerms(long mailboxId) throws IOException, ServiceException
    {
//        outputStream.print("Field Name> ");
//        String field = inputReader.readLine();
//        if (field.equals("")) {
//            return;
//        }
//
//        outputStream.print("Min Frequency> ");
//        String min = inputReader.readLine();
//        outputStream.print("Max Frequency> ");
//        String max = inputReader.readLine();
//        int minNum=0,maxNum=100;
//        boolean constrain = false;
//        if (!min.equals("")) {
//            minNum = Integer.parseInt(min);
//            constrain = true;
//        }
//        if (!max.equals("")) {
//            constrain = true;
//            maxNum = Integer.parseInt(max);
//        }
//        if (constrain) {
//            outputStream.println("Showing all terms with frequencies between "+minNum+" and "+maxNum);
//        }
//
////      MailboxIndex searcher = Indexer.GetInstance().getMailboxIndex(mailboxId);
////      MailboxIndex searcher = Mailbox.getMailboxById(mailboxId).getMailboxIndex();
//
//
//        MailboxIndex.AdminInterface admin = null;
//
//        try {
////          Collection c = new ArrayList();
//            Collection c = new TreeSet(new MailboxIndex.AdminInterface.TermInfo.FreqComparator());
//
//            Mailbox mbox = MailboxManager.getInstance().getMailboxById(mailboxId);
//            MailboxIndex mi = mbox.getMailboxIndex();
//            admin = mi != null ? mi.getAdminInterface() : null;
//            if (admin != null)
//                admin.enumerateTerms(c, field);
//
//            int numDocs = admin.numDocs();
//
//            double minFlt = minNum / 10000.0;
//            double maxFlt = maxNum / 10000.0;
//            outputStream.println("minFlt = "+minFlt+" max="+maxFlt);
//
//            double scaledMin = minFlt * numDocs;
//            double scaledMax = maxFlt * numDocs;
//            outputStream.println("Showing all terms with frequencies between "+scaledMin+" and "+scaledMax);
//
////          IndexReader reader = admin.getIndexReader();
//
//            Iterator iterator = c.iterator();
//            int tot = 0;
//            int totalTerms = 0;
//            ArrayList ats = new ArrayList();
//            while (iterator.hasNext()) {
//                MailboxIndex.AdminInterface.TermInfo info =
//                    (MailboxIndex.AdminInterface.TermInfo)iterator.next();
//                String termText = info.mTerm.text();
//
//                if (termText.indexOf('@') > -1) {
//                    ats.add(termText);
//                }
//
//                if (NumDigits(termText) <= 2) {
//                    if (termText.length() > 2 && !termText.equals("")) {
//
//                        totalTerms++;
//
//                        //				if ((info.mFreq >= scaledMin) && (info.mFreq<=scaledMax)) {
//                        if (info.mFreq >= minNum) {
//                            if (tot < maxNum && admin != null) {
//                                tot++;
////                              TermEnum e = reader.terms(new Term(LuceneFields.L_CONTENT, termText));
////                              int occurences = e.docFreq() - info.mFreq;
//                                int occurences = admin.countTermOccurences(LuceneFields.L_CONTENT, termText);
//
////                              int low0 = 0; // 5
////                              int high0 = 99999; // 40
////                              int low = 0; // 5
////                              int high = 99999; // 75
//
////                              if (info.mFreq > low0 && info.mFreq < high0 && occurences > low && occurences < high) {
//                                outputStream.println("" + info.mFreq + ": " + termText+" ("+occurences+")");
////                              }
//
//                            }
//                        }
//                    }
//                }
//            }
//
////          outputStream.println("Ats list:");
////          for (int i = 0; i <ats.size(); i++) {
////          outputStream.println(ats.get(i).toString());
////          }
//            outputStream.println("Displayed "+tot+" out of "+totalTerms+" terms in the index");
//        } catch (Exception e) {
//            outputStream.println("Caught "+ExceptionToString.ToString(e));
//        } finally {
//            if (admin!=null) {
//                admin.close();
//            }
//        }
    }

    public void getTerms(long mailboxId, String field, int minNum, int maxNum, Collection ret) throws IOException, ServiceException
    {
////      MailboxIndex searcher = Indexer.GetInstance().getMailboxIndex(mailboxId);
////      MailboxIndex searcher = Mailbox.getMailboxById(mailboxId).getMailboxIndex();
//
//
//        MailboxIndex.AdminInterface admin = null;
//
//        try {
////          Collection c = new ArrayList();
//            Collection c = new TreeSet(new MailboxIndex.AdminInterface.TermInfo.FreqComparator());
//
//            Mailbox mbox = MailboxManager.getInstance().getMailboxById(mailboxId);
//            MailboxIndex mi = mbox.getMailboxIndex();
//            admin = mi != null ? mi.getAdminInterface() : null;
//            if (admin != null)
//                admin.enumerateTerms(c, field);
//
////          int numDocs = admin.numDocs();
//
////          double minFlt = minNum / 100000.0;
////          double maxFlt = maxNum / 100000.0;
////          outputStream.println("minFlt = "+minFlt+" max="+maxFlt);
//
////          double scaledMin = minFlt * numDocs;
////          double scaledMax = maxFlt * numDocs;
////          outputStream.println("Showing all terms with frequencies between "+scaledMin+" and "+scaledMax);
//
//            int tot = 0;
//
//            Iterator iterator = c.iterator();
//            while (iterator.hasNext()) {
//                MailboxIndex.AdminInterface.TermInfo info =
//                    (MailboxIndex.AdminInterface.TermInfo)iterator.next();
//
//                if (tot > maxNum) {
//                    return;
//                }
//
////              if ((info.mFreq >= scaledMin) && (info.mFreq<=scaledMax)) {
//                if (info.mFreq >= minNum) {
//                    String termText = info.mTerm.text();
//                    if (NumDigits(termText) <= 1) {
//                        if (termText.length() > 3 && !termText.equals("")) {
//                            outputStream.println("" + info.mFreq + ": " + termText);
//                            ret.add(termText);
//                            tot++;
//                        }
//                    }
//                }
//            }
//        } catch (Exception e) {
//            outputStream.println("Caught "+ExceptionToString.ToString(e));
//        } finally {
//            if (admin!=null) {
//                admin.close();
//            }
//        }
    }

//    public int CountNear(MailboxIndex.AdminInterface admin, Term[] terms, int slop, boolean inOrder) throws IOException
//    {
//        SpanQuery q[] = new SpanQuery[terms.length];
//        for (int i = 0; i < terms.length; i++) {
//            q[i] = new SpanTermQuery(terms[i]);
//        }
//        SpanQuery near = new SpanNearQuery(q, slop, inOrder);
////      Spans results = near.getSpans(reader);
//        Spans results = admin.getSpans(near);
//
//        int ret = 0;
////      do {
//        while(results.next()) {
////          int docNo = results.doc();
////          outputStream.print("SPAN(doc="+docNo+","+results.start()+","+results.end()+"):\n\t");
////          if (docNo >= 0) {
////          Document doc = reader.document(docNo);
////          DumpDocument(doc, false);
////          }
////          outputStream.println("");
//            ret++;
//        }// while(results.next());
//
//        return ret;
//    }

    public static class TwoTerms implements Comparable {
        /* (non-Javadoc)
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo(Object o) {
            TwoTerms other = (TwoTerms)o;
            if (other.mCount == mCount) {
                if (other.s1.equals(s1)) {
                    return -(other.s2.compareTo(s2));
                }
                return -(other.s1.compareTo(s1));
            }
            return -(other.mCount-mCount);
        }
        public int mCount;
        public String s1;
        public String s2;



    }

    public void spanTest(long mailboxId) throws IOException, ServiceException
    {
//        outputStream.println("SpanTest!\n");
//        outputStream.print("Field1 Name> ");
//        String field = inputReader.readLine();
//        if (field.equals("")) {
//            return;
//        }
//
//        outputStream.print("Min Frequency> ");
//        String min = inputReader.readLine();
//        outputStream.print("Max Frequency> ");
//        String max = inputReader.readLine();
//        int minNum=0,maxNum=100;
//        boolean constrain = false;
//        if (!min.equals("")) {
//            minNum = Integer.parseInt(min);
//            constrain = true;
//        }
//        if (!max.equals("")) {
//            constrain = true;
//            maxNum = Integer.parseInt(max);
//        }
//
//        int slopNum = 10;
//
//        outputStream.print("Slop> ");
//        String sSlop = inputReader.readLine();
//        if (!sSlop.equals("")) {
//            constrain = true;
//            slopNum = Integer.parseInt(sSlop);
//        }
//
//        ArrayList c = new ArrayList();
//        getTerms(mailboxId, field, minNum, maxNum, c);
//
//
//        if (constrain) {
//            outputStream.println("Terms with pctages between "+minNum+" and "+maxNum+" with slop "+slopNum);
//        }
//
//
//
//
//
//        /*		outputStream.print("Text1> ");
//		String text1 = in.readLine();
//		if (text1.equals("")) {
//			return;
//		}
//
//		outputStream.print("Field2 Name> ");
//		String field2 = in.readLine();
//		if (field2.equals("")) {
//			return;
//		}
//		outputStream.print("Text> ");
//		String text = in.readLine();
//		if (field.equals("")) {
//			return;
//		}*/
//
//        outputStream.println("-------------------------------");
//
//        MailboxIndex.AdminInterface admin = null;
//        try {
//            Mailbox mbox = MailboxManager.getInstance().getMailboxById(mailboxId);
//            MailboxIndex mi = mbox.getMailboxIndex();
//            admin = mi != null ? mi.getAdminInterface() : null;
////          IndexReader reader = admin.getIndexReader();
//            int printNum = 1; //c.size() / 10;
//            if (printNum < 1) {
//                printNum = 1;
//            }
//
//            Collection matches = new TreeSet();
//
//            if (admin != null) {
//                for (int i = 0; i < (c.size()-1); i++) {
//                    if (i % printNum == 0) {
//                        outputStream.println("Iter "+i+" out of "+c.size());
//                    }
//                    String s1 = (String)c.get(i);
//                    if (s1.length()>2) {
//                        for (int j = 0; j < c.size(); j++){
//                            String s2 = (String)c.get(j);
//                            //					String s1 = "my";
//                            //					String s2 = "birthday";
//                            if (s2.length() > 2) {
//    
//                                Term t1 = new Term(field, s1);
//                                Term t2 = new Term(field, s2);
//                                int near = CountNear(admin, new Term[] {t1, t2}, slopNum, true);
//                                TwoTerms tt = new TwoTerms();
//                                tt.mCount = near;
//                                tt.s1 = s1;
//                                tt.s2 = s2;
//                                matches.add(tt);
//    //                          if (tt.mCount > 10) {
//    //                          outputStream.println(""+tt.mCount+" - "+tt.s1+","+tt.s2);
//    //                          }
//                            }
//                        }
//                    }
//                }
//            }
//
//            Iterator i = matches.iterator();
//            while (i.hasNext()) {
//                TwoTerms t = (TwoTerms)i.next();
//                if (t.mCount > 0) {
//                    outputStream.println(""+t.mCount+": "+t.s1+","+t.s2);
//                }
//            }
//
////          SpanQuery q1 = new SpanTermQuery(new Term(field, text));
////          SpanQuery first = new SpanFirstQuery(q1, 100);
////          Spans results = first.getSpans(reader);
//
////          do {
////          int docNo = results.doc();
////          outputStream.print("SPAN(doc="+docNo+","+results.start()+","+results.end()+"):\n\t");
////          if (docNo >= 0) {
////          Document doc = reader.document(docNo);
////          DumpDocument(doc, false);
////          }
////          outputStream.println("");
////          } while(results.next());
//
//
//        } catch (Exception e) {
//            outputStream.println("Caught "+ExceptionToString.ToString(e));
//        } finally {
//            if (admin!=null) {
//                admin.close();
//            }
//        }
    }


//  private static class MemoryThread extends Thread
//  {
//  public boolean mRun = true;
//  public void run() {
////while(mRun) {
////long totalMem = Runtime.getRuntime().totalMemory();
////long freeMem = Runtime.getRuntime().freeMemory();
////long maxMem = Runtime.getRuntime().maxMemory();
////totalMem = totalMem / (1024);
////freeMem = freeMem / (1024);
////maxMem = maxMem / (1024);
////long myGuess = totalMem - freeMem;
////outputStream.println("MT1> GuessUsed: " + myGuess + "K  (System says: total "+ totalMem + "K and claims to have " + freeMem + "K free out of " + maxMem + "K)");
////try {
////Thread.sleep(1000);
////} catch (InterruptedException e) {};
////}
//  }
//  public void stopMemThread() {
//  mRun = false;
//  }
//  }

    static IndexEditorTcpServer sTcpServer = null;
    static IndexEditorProtocolhandler sIndexEditorProtocolHandler;
    static int sPortNo = 7035;
    static IndexEditorTcpThread tcpServerThread;
    static Thread sThread;

    public static void StartTcpEditor() throws ServiceException
    {
        ServerSocket serverSocket = NetUtil.getTcpServerSocket(null, sPortNo);
        sTcpServer = new IndexEditorTcpServer("IndexEditorTcpServer", 3, Thread.NORM_PRIORITY, serverSocket);
        sIndexEditorProtocolHandler = new IndexEditorProtocolhandler(sTcpServer);
        sTcpServer.addActiveHandler(sIndexEditorProtocolHandler);
        sThread = new Thread(new IndexEditorTcpThread(), "IndexEditor-TcpServer");
        sThread.start();
    }

    public static void EndTcpEditor() {

        for (Iterator iter = inputs.iterator(); iter.hasNext();) {
            Object cur = iter.next();
            try {
                if (cur instanceof InputStream) {
                    ((InputStream)cur).close();
                } else {
                    ((OutputStream)cur).close();
                }
            } catch (IOException e) {
                mLog.error("Caught "+ExceptionToString.ToString(e));
            }
        }

        if (sTcpServer != null) {
            try {
                sTcpServer.removeActiveHandler(sIndexEditorProtocolHandler);
            } finally {
                sTcpServer.stop(0);
                sTcpServer = null;
            }
        }
    }

    public static ArrayList inputs = new ArrayList();

    private static class IndexEditorTcpThread implements Runnable {
        public void run() {
            sTcpServer.run();
        }
    }

    private static class IndexEditorTcpServer extends TcpServer {

        IndexEditorTcpServer(String name, int numThreads, int threadPriority, ServerSocket serverSocket) {
            super(name, numThreads, threadPriority, serverSocket);
        }
        protected ProtocolHandler newProtocolHandler() {
            return new IndexEditorProtocolhandler(this);
        }
        public int getConfigMaxIdleMilliSeconds(){
            return 0;
        }

    }


    private static class IndexEditorProtocolhandler extends ProtocolHandler
    {
        private InputStream mInputStream;
        private OutputStream mOutputStream;
//      private String mRemoteAddress;
        private IndexEditor mEditor = null;

        private String logLayoutPattern = "%d %-5p [%t] [%x] %c{1} - %m%n";


        public IndexEditorProtocolhandler(TcpServer server) {
            super(server);
        }



        /**
         * Performs any necessary setup steps upon connection from client.
         * @throws IOException
         */
        protected boolean setupConnection(Socket connection) throws IOException
        {
//          mRemoteAddress = connection.getInetAddress().getHostAddress();

            mInputStream = new TcpServerInputStream(connection.getInputStream());
            mOutputStream = new BufferedOutputStream(connection.getOutputStream());

            inputs.add(mInputStream);
            inputs.add(mOutputStream);
            return true;
        }

        /**
         * Authenticates the client.
         * @return true if authenticated, false if not authenticated
         * @throws IOException
         */
        protected boolean authenticate() throws IOException {
            return true;
        }

        private WriterAppender mAppender;
        public boolean enableLogging() {
            if (mAppender == null) {
                Layout layout = new PatternLayout(logLayoutPattern );
                mAppender = new WriterAppender(layout, mOutputStream);
                Logger root = Logger.getRootLogger();

                root.addAppender(mAppender);
                return true;
            } else {
                return false;
            }
        }
        public boolean disableLogging() {
            if (mAppender != null) {
                Logger root = Logger.getRootLogger();
                root.removeAppender(mAppender);
                mAppender = null;
                return true;
            }
            return false;
        }

        /**
         * Reads and processes one command sent by client.
         * @return true if expecting more commands, false if QUIT command was
         *         received and server disconnected the connection
         * @throws Exception
         */
        protected boolean processCommand() throws Exception
        {
            mAppender = null;
            try {
                mEditor = new IndexEditor(this);
                mEditor.run(mInputStream, mOutputStream);
            } finally {
                disableLogging();
            }
            return false;
        }

        /**
         * Closes any input/output streams with the client.  May get called
         * multiple times.
         */
        protected void dropConnection() {
            if (mInputStream != null) {
                try {
                    mInputStream.close();
                } catch(IOException e) {
                    mLog.warn("While closing output stream, Caught "+ExceptionToString.ToString(e));
                }
                mInputStream = null;
            }
            if (mOutputStream != null) {
                try {
                    mOutputStream.close();
                } catch(IOException e) {
                    mLog.warn("While closing output stream, Caught "+ExceptionToString.ToString(e));
                }
                mOutputStream = null;
            }
        }

        /**
         * Called when a connection has been idle for too long.  Sends
         * protocol-specific message to client notifying it that the
         * connection is being dropped due to idle timeout.
         */
        protected void notifyIdleConnection() {
        }

    }

    public void run(InputStream input, OutputStream output)
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        PrintStream printer = new PrintStream(output, true);
        run(reader, printer);
    }

    public void run() {
        run(new BufferedReader(new InputStreamReader(System.in)), System.out);
    }

    public void run(BufferedReader _inputReader, PrintStream _outputStream)
    {
        inputReader = _inputReader;
        outputStream = _outputStream;

        String mailboxIdStr = null;
        long mailboxId = 0;
//      try {
//      outputStream.print("Enter Mbox ID> ");

//      mailboxIdStr = inputReader.readLine();
//      } catch(Exception e) {
//      outputStream.print("Caught exception: "+e.toString());
//      e.printStackTrace();
//      }
//      if (mailboxIdStr == null || mailboxIdStr.equals("")) {
//      mailboxIdStr = "1";
//      mailboxId = 1;
//      } else {
//      mailboxId = Integer.parseInt(mailboxIdStr);
//      }

        boolean quit = false;

        while(!quit) {
            try {
                outputStream.print("> ");

                String command = null;
                try {
                    command = inputReader.readLine();
                } catch (IOException e) {
                    // an IOException here means the input stream is closed.  We should quit.
                    quit=true;
                    continue;
                }

                if (command==null || command.equals("exit") || command.equals("quit")) {
                    quit = true;
                } else if (command.equals("?")) {
                    help();
                } else if (command.equals("re-index")) {
                    reIndex(mailboxId);
                } else if (command.equals("re-index-all")) {
                    reIndexAll();
                } else if (command.equals("re-index-msg")) {
                    outputStream.print("MSGID> THIS_FUNCTION_CURRENTLY_UNIMPLEMENTED");
//                    String msg = inputReader.readLine();
//                    int msgId = Integer.parseInt(msg);
//                    reIndexMsg(mailboxId, msgId);
                } else if (command.equals("sort da")) {
                    sortOrder = SortBy.DATE_ASCENDING;
                    outputStream.println("---->Search order = DATE_ASCENDING");
                } else if (command.equals("sort dd")) {
                    sortOrder = SortBy.DATE_DESCENDING;
                    outputStream.println("---->Search order = DATE_DESCENDING");
                } else if (command.equals("sort sa")) {
                    sortOrder = SortBy.SUBJ_ASCENDING;
                    outputStream.println("---->Search order = SUBJ_ASCENDING");
                } else if (command.equals("sort sd")) {
                    sortOrder = SortBy.SUBJ_DESCENDING;
                    outputStream.println("---->Search order = SUBJ_DESCENDING");
                } else if (command.equals("sort na")) {
                    sortOrder = SortBy.NAME_ASCENDING;
                    outputStream.println("---->Search order = NAME_ASCENDING");
                } else if (command.equals("sort nd")) {
                    sortOrder = SortBy.NAME_DESCENDING;
                    outputStream.println("---->Search order = NAME_DESCENDING");
                } else if (command.equals("sort za")) {
                    sortOrder = SortBy.SIZE_ASCENDING;
                    outputStream.println("---->Search order = SIZE_ASCENDING");
                } else if (command.equals("sort zd")) {
                    sortOrder = SortBy.SIZE_DESCENDING;
                    outputStream.println("---->Search order = SIZE_DESCENDING");
                } else if (command.equals("q") || command.equals("query")) {
                    QueryRunner runner = new SingleQueryRunner(mailboxId);
                    doQuery(runner,false, SEARCH_RETURN_MESSAGES);
                } else if (command.equals("qd") || command.equals("querydump")) {
                    QueryRunner runner = new SingleQueryRunner(mailboxId);
                    doQuery(runner,true, SEARCH_RETURN_DOCUMENTS);
                } else if (command.equals("qc") || command.equals("queryconv")) {
                    QueryRunner runner = new SingleQueryRunner(mailboxId);
                    doQuery(runner,true, SEARCH_RETURN_CONVERSATIONS);
//                  } else if (command.equals("nq") || command.equals("newquery")) {
//                  newQuery(mailboxId);
                } else if (command.equals("qp") || command.equals("queryconv")) {
                    QueryRunner runner = new SingleQueryRunner(mailboxId);
                    doQuery(runner,true, SEARCH_RETURN_DOCUMENTS);
                } else if (command.equals("mq")) {
                    ArrayList<Long> ids = new ArrayList<Long>();
                    do {
                        outputStream.print("Enter Mailbox ID (blank when done): ");
                        mailboxIdStr = inputReader.readLine();

                        if (!mailboxIdStr.equals("")) {
                            long id = getMailboxIdFromString(mailboxIdStr);
                            outputStream.println("\tAdded mailbox ID "+id);
                            ids.add(new Long(id));
                        }
                    } while (!mailboxIdStr.equals(""));
                    QueryRunner runner = new MultiQueryRunner(ids);
                    doQuery(runner, false, SEARCH_RETURN_CONVERSATIONS);
                } else if (command.equals("mbox")) {
                    outputStream.print("Enter New Mailbox ID: ");
                    mailboxIdStr = inputReader.readLine();
                    mailboxId = getMailboxIdFromString(mailboxIdStr);
                    outputStream.println("\tMailboxID set to "+mailboxId);
                } else if (command.equals("fields")) {
                    dumpFields(mailboxId);
                } else if (command.equals("terms")) {
                    dumpTerms(mailboxId);
                } else if (command.equals("s")) {
                    spanTest(mailboxId);
                } else if (command.equals("delete_index")) {
                    if (confirm("Are you sure you want to delete the index for mailbox "+ mailboxId+"?"))
                    {
                        deleteIndex(mailboxId);
                    }
                } else if (command.equals("dumpmi")) {
                    outputStream.print("Enter Mail-Item-ID:");
                    String midStr = inputReader.readLine();

                    if (!midStr.equals("")) {
                        int id = Integer.parseInt(midStr);
                        dumpDocumentByMailItemId(mailboxId, id);
                    }
                } else if (command.equals("dumpall")) {
                    dumpAll(mailboxId);
                } else if (command.equals("unit")) {
                    TestSearch.runTests();
//                  } else if (command.equals("archive")) {
//                  archive(mailboxId);
                } else if (command.equals("verify")) {
                    checkIndex(mailboxId, false);
                } else if (command.equals("repair")) {
                    checkIndex(mailboxId, true);
                } else if (command.equals("size")) {
                    getSize(mailboxId);
                } else if (command.equals("loglevel")) {
                    logLevel();
                } else if (command.equals("snoop")) {
                    if (mHandler == null) {
                        outputStream.println("Log Snooping only available in remote mode");
                    } else {
                        if (mHandler.enableLogging()) {
                            outputStream.println("Log Snooping ENABLED");
                        } else {
                            outputStream.println("Log Snooping already active.");
                        }
                    }
                } else if (command.equals("nosnoop")) {
                    if (mHandler == null) {
                        outputStream.println("Log Snooping only available in remote mode");
                    } else {
                        if (mHandler.disableLogging()) {
                            outputStream.println("Log Snooping DISABLED");
                        } else {
                            outputStream.println("Log Snooping not active.");
                        }
                    }
                }
            } catch(Exception e) {
                outputStream.println("Caught Exception "+ExceptionToString.ToString(e));
            }
        }

    }


    long getMailboxIdFromString(String str) throws ServiceException {
        if (str != null && !str.equals("")) {
            if (str.indexOf('@') >= 0) {
                // account
                Account acct = Provisioning.getInstance().get(AccountBy.name, str);
                Mailbox mbx = MailboxManager.getInstance().getMailboxByAccount(acct);
                return mbx.getId();

            } else {
                return Long.parseLong(str);
            }
        }
        return 0;
    }

    public void logLevel()
    {
        String logLevel = null;
        try {
            outputStream.print("Enter logging level> ");
            logLevel = inputReader.readLine();
        } catch(Exception e) {
            outputStream.print("Caught exception: "+e.toString());
        }

        Logger root = Logger.getRootLogger();

        if (logLevel != null && !logLevel.equals("")) {

            Level newLevel = null;

            if (logLevel.equalsIgnoreCase("ALL")) {
                newLevel = Level.ALL;
            } else if (logLevel.equalsIgnoreCase("DEBUG")) {
                newLevel = Level.DEBUG;
            } else if (logLevel.equalsIgnoreCase("ERROR")) {
                newLevel = Level.ERROR;
            } else if (logLevel.equalsIgnoreCase("FATAL")) {
                newLevel = Level.FATAL;
            } else if (logLevel.equalsIgnoreCase("INFO")) {
                newLevel = Level.INFO;
            } else if (logLevel.equalsIgnoreCase("OFF")) {
                newLevel = Level.OFF;
            } else if (logLevel.equalsIgnoreCase("WARN")) {
                newLevel = Level.WARN;
            }

            if (newLevel == null) {
                outputStream.println("Unknown level - must be ALL/DEBUG/ERROR/FATAL/INFO/OFF/WARN");
                return;
            }

            root.setLevel(newLevel);
        }
        Level cur = root.getLevel();
        outputStream.println("Current level is: "+cur);
    }

    private IndexEditorProtocolhandler mHandler;
    public IndexEditor(IndexEditorProtocolhandler handler) {
        mHandler = handler;
    }
    public IndexEditor() {
        mHandler = null;
    }

    public static void main(String[] args)
    {
        CliUtil.toolSetup("DEBUG");

        MailboxIndex.startup();
        IndexEditor editor = new IndexEditor();
        editor.run();
        MailboxIndex.shutdown();
    }

    void getSize(long mailboxId) throws ServiceException {
        Mailbox mbx = MailboxManager.getInstance().getMailboxById(mailboxId);
        long size = mbx.getSize();
        outputStream.println("Mailbox "+mailboxId+" has size "+size+" ("+(size/1024)+"kb)");
    }

    void help() {
        outputStream.println("\nHELP (updated)");
        outputStream.println("----");
        outputStream.println("exit-- exit this program");
        outputStream.println("re-index -- re-index mailbox from message store");
        outputStream.println("re-index-all -- re-index ALL MAILBOXES (!) in the message store");
        outputStream.println("re-index-msg -- re-index mailbox from message store");
        outputStream.println("sort na|nd|sa|sd|da|dd -- set sort order (name asc/desc, subj asc/desc or date asc/desc)");
        outputStream.println("query -- run a query group_by_message");
        outputStream.println("queryconv -- run a query group_by_conv");
//      outputStream.println("newquery -- run new query");
        outputStream.println("mbox -- change mailbox");
        outputStream.println("fields -- dump all known fields");
        outputStream.println("terms -- dump all known terms for a field");
//      outputStream.println("spans -- spantest");
        outputStream.println("delete_index -- deletes the index");
        outputStream.println("dumpmi -- dump document by mail_item");
        outputStream.println("dumpall -- dump all documents");
        outputStream.println("unit -- run unit tests");
//      outputStream.println("archive -- archive from recent idx to stable idx");
        outputStream.println("hack -- hacked test code to make a copy of index");
        outputStream.println("size -- Return the (uncompressed) size of this mailbox");
        outputStream.println("verify -- Verify that all messages in this mailbox are indexed");
        outputStream.println("loglevel -- Change the default global logging level (affects all appenders!)");
        outputStream.println("snoop -- copy log4j root logger to local output (snoop logs)");
        outputStream.println("nosnoop -- stop copying log4j logger");
    }
}
