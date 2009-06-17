/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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
package com.zimbra.cs.index.queryparser;

import java.io.StringReader;
import java.util.AbstractList;
import java.util.Enumeration;
import java.util.Locale;
import java.util.TimeZone;

import junit.framework.TestCase;
import junit.framework.TestFailure;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.apache.lucene.analysis.Analyzer;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.index.ZimbraQuery;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.qa.unittest.TestUtil;

/**
 * 
 */
public class TestQueryParser extends TestCase {
    long mMailboxId;
    Mailbox mMbox;
    MailboxIndex mMi;
    
    
    @Override protected void setUp() throws Exception {
        super.setUp();
        Mailbox mbox = TestUtil.getMailbox("user1");
        mMailboxId = mbox.getId();
    }
    
    public void testQueryParser() throws ServiceException, ParseException  {
        mMbox = MailboxManager.getInstance().getMailboxById(mMailboxId);
        mMi = mMbox.getMailboxIndex();
        mMi.initAnalyzer(mMbox);

        runQuery("subject:\"foo bar\" and content:\"baz gub\"");
        runQuery("date:-4d");
        runQuery("date:\"-4d\"");
        runQuery("subject:\"foo\"");
        runQuery("(a or b)");
        runQuery("(a or b) and in:inbox");
        
        runQuery("(a or b) and before:1/1/2009 and -subject:\"quoted string\"");
        runQuery("\"This is a \\\"phrase\\\" query\"");
    }
    
    private void runQuery(String queryStr) throws ServiceException, ParseException {
        runQuery(queryStr, null, null, "content:");
    }
    
    private void runQuery(String queryStr, TimeZone tz, Locale l, String defaultField) throws ServiceException, ParseException {
        System.out.println("Parsing: "+queryStr);
        ZimbraQueryParser parser = new ZimbraQueryParser(new StringReader(queryStr));
        Analyzer analyzer = mMi.getAnalyzer();
        parser.init(analyzer, mMbox, tz, l, ZimbraQuery.lookupQueryTypeFromString(defaultField));
        AbstractList clauses = parser.Parse();
        System.out.println("Output: ");
        for (Object o : clauses) {
            System.out.println("\t"+o);
        }
    }
    
    
    public static void runTests()
    {
        TestSuite suite = new TestSuite(TestQueryParser.class);
        TestResult results = new TestResult();
        suite.run(results);
//        System.out.println("Test Results: "+results.toString());

        if (!results.wasSuccessful()) {
            System.out.println("\n**************************");
            System.out.println("TEST FAILURES:");
            System.out.println("**************************");
        }

        if (results.failureCount() > 0) {
            Enumeration failures = results.failures();
            while(failures.hasMoreElements()) {
                TestFailure error = (TestFailure)failures.nextElement();
                System.out.println("--> Test Failure: " + error.trace() + error.thrownException());
                System.out.print("\n");
            }
        }

        if (results.errorCount() > 0) {
            Enumeration errors = results.errors();
            while(errors.hasMoreElements()) {
                TestFailure failure = (TestFailure)errors.nextElement();
                System.out.println("--> Test Error: " + failure.trace() + failure.thrownException() + " at ");
                failure.thrownException().printStackTrace();
                System.out.print("\n");
            }
        }

        if (results.wasSuccessful()) {
            System.out.println("\n**************************");
            System.out.println("Tests SUCCESSFUL!");
            System.out.println("**************************");
        }
    }
    

    /**
     * @param args
     */
    public static void main(String[] args) {
        CliUtil.toolSetup("DEBUG");
        ZimbraLog.account.info("INFO TEST!");
        ZimbraLog.account.debug("DEBUG TEST!");

        runTests();
        // hack: some system thread isn't cleaned up...just exit
        System.exit(1);

    }

}
