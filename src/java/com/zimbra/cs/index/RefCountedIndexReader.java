/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.lucene.index.IndexReader;

import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.common.stats.StatsDumper;
import com.zimbra.common.stats.StatsDumperDataSource;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;

/**
 * Ref Counting wrapper around a Lucene IndexReader
 */
final class RefCountedIndexReader {
    private ILuceneIndex mIdx;
    private IndexReader mReader;
    private int mCount = 1;
    private long mAccessTime;
    private boolean mRequiresReopen = false;
    
    // debugging stuff
    private boolean mDebug = false;
    private static RefCountedIndexReaderStats sStatsInstance = null;
    private static final String DEBUG_DELIM = ", ";
    private enum DebugAction {
        CRT("*"),  // creating 
        ADD("+"),  // add ref
        DEC("-") ; // dec ref
        
        private String mSymbol; 
        DebugAction(String symbol) {
            mSymbol = symbol;
        }
        
        String symbol() {
            return mSymbol;
        }
    }
    
    static {
        if (DebugConfig.enableRefCountedIndexReaderStats) {
            sStatsInstance = new RefCountedIndexReaderStats();
            StatsDumper.schedule(sStatsInstance, 5 * Constants.MILLIS_PER_SECOND);
        }
    }
    
    private static final class RefCountedIndexReaderStats implements StatsDumperDataSource {
        public String getFilename() {
            return "RefCountedIndexReader.csv";
        }
        
        public String getHeader() {
            List<String> columns = new ArrayList<String>();
            columns.add("ThreadName");
            columns.add("MailboxId");
            columns.add("Notes");
            columns.add("Hashcode");
            columns.add("RefCount");
            columns.add("LastAccessTime");
            columns.add("StackFrames");
            return StringUtil.join(",", columns);
        }
        
        synchronized public Collection<String> getDataLines() {
            Collection<String> curDataLines = mDataLines;
            mDataLines = new ArrayList<String>(); // "empty it out" after we've logged 
            return curDataLines;
        }
        
        public boolean hasTimestampColumn() {
            return true;
        }
        
        Collection<String> mDataLines = new ArrayList<String>();
        
        synchronized void addStatLine(String line) {
            mDataLines.add(line);
        }
        
        static void addStats(String line) {
            if (sStatsInstance == null)
                return;

            sStatsInstance.addStatLine(line);
        }
    }
    
    void statMe(DebugAction action) {
        if (!mDebug)
            return;

        // the thread must be holding the lock on the RefCountedIndexReader object
        // log it if that's not the case (should not happen)
        StringBuilder line = new StringBuilder();
            
        Thread curThread = Thread.currentThread();
            
        // ThreadName,
        // line.append(curThread.getId() + DEBUG_DELIM);
        line.append(curThread.getName() + DEBUG_DELIM);        
        
        // MailboxId
        long mailboxId = -1;
        if (mIdx instanceof LuceneIndex) {
            LuceneIndex li = (LuceneIndex)mIdx;
            mailboxId = li.getMailboxId();
        }
        if (mailboxId == -1)
            line.append("unknown" + DEBUG_DELIM);
        else
            line.append(mailboxId + DEBUG_DELIM);
            
        // Notes,
        line.append("[");
        line.append(action.symbol());
        if (action != DebugAction.CRT && !Thread.holdsLock(this)) 
            line.append(" nolock!");     // no lock warning, not likely to happen, just checking
        line.append("]" + DEBUG_DELIM);

        // Hashcode,
        line.append(hashCode() + DEBUG_DELIM);
        
        // RefCount,
        line.append("["+ mCount + "]" + DEBUG_DELIM);
        
        // LastAccessTime,
        String accessTime = String.format("%1$tm%1$td-%1$tH:%1$tM:%1$tS.%1$tL", new Date(mAccessTime));
        line.append(accessTime + "(" + mAccessTime + ")" + DEBUG_DELIM);
            
        // StackFrames
        StackTraceElement[] stack = curThread.getStackTrace();
        int maxFrames = Math.min(50, stack.length);
            
        // skip the top two frames, they are Thread:1409 and RefCountedIndexReader:108 
        for (int i = 2; i < maxFrames; i++) {
            String className = stack[i].getClassName();
            int lastDot = className.lastIndexOf('.');
            if (lastDot != -1)
                className = className.substring(lastDot+1);
            line.append(className + ":" + stack[i].getLineNumber() + " - ");
            
            // we don't want the frames to go too deep, stop if we see these:
            if (className.equals("SoapEngine") || 
                className.equals("ZimbraServlet") ||   
                className.equals("ProtocolHandler"))
                break;
        }
            
        RefCountedIndexReaderStats.addStats(line.toString() + "\n"); // add an empty line so it's easier for human eyes 
    }
    
    RefCountedIndexReader(ILuceneIndex idx, IndexReader reader) {
        mIdx = idx;
        mReader= reader;
        mAccessTime = System.currentTimeMillis();
        
        if (DebugConfig.enableRefCountedIndexReaderStats && ZimbraLog.index.isDebugEnabled())
            mDebug = true;
        
        statMe(DebugAction.CRT);
    }
    
    synchronized IndexReader getReader() {
        return mReader; 
    }

    synchronized void addRef() {
        mAccessTime = System.currentTimeMillis();
        mCount++;
        
        statMe(DebugAction.ADD);
    }

    synchronized void forceClose() {
        closeIt();
    }

    synchronized void release() {
        mCount--;
        assert(mCount >= 0);
        if (0 == mCount) {
            closeIt();
        }
        
        statMe(DebugAction.DEC);
    }
    
    synchronized boolean markForReopen() {
        if (mCount != 1) 
            return false;
        mRequiresReopen = true;
        return true;
    }
    
    synchronized boolean requiresReopen() { assert(!mRequiresReopen || mCount==1); return mRequiresReopen; }
    
    synchronized void reopened(IndexReader newReader) {
        assert(mRequiresReopen && mCount==1); 
        mRequiresReopen = false;
        mReader = newReader;
    }
    
    synchronized int getRefCount() {
        return mCount;
    }
    
    synchronized long getAccessTime() {
        return mAccessTime;
    }

    private void closeIt() {
        try {
            mReader.close();
        } catch (IOException e) {
            ZimbraLog.im.debug("Caught exception while closing IndexReader: ", e);
        } finally {
            mReader= null;
            mIdx.onReaderClose(this);
        }
    }
}