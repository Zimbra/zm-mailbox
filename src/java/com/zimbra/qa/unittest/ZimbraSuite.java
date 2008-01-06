/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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

package com.zimbra.qa.unittest;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;

/**
 * Complete unit test suite for the Zimbra code base.
 * 
 * @author bburtin
 *
 */
public class ZimbraSuite extends TestSuite
{
    private static final List<Class<? extends TestCase>> sClasses = new ArrayList<Class<? extends TestCase>>();
    
    static {
        sClasses.add(TestWaitSet.class);
        sClasses.add(TestUtilCode.class);
        sClasses.add(TestEmailUtil.class);
        sClasses.add(TestOutOfOffice.class);
        sClasses.add(TestDbUtil.class);
        sClasses.add(TestUnread.class);
        sClasses.add(TestTags.class);
        sClasses.add(TestItemCache.class);
        sClasses.add(TestFolders.class);
        sClasses.add(TestSpellCheck.class);
        sClasses.add(TestAuthentication.class);
        sClasses.add(TestAccount.class);
        sClasses.add(TestConversion.class);
        sClasses.add(TestMailItem.class);
        sClasses.add(TestConcurrency.class);
        sClasses.add(TestFolderFilterRules.class);
        sClasses.add(TestTagFilterRules.class);
        sClasses.add(TestPop3Import.class);
        sClasses.add(TestFilter.class);
        sClasses.add(TestPop3ImapAuth.class);
        sClasses.add(TestContacts.class);
        sClasses.add(TestTaskScheduler.class);
        
        // XXX bburtin: commenting out TestSearch until bug 18802 is fixed
        // sClasses.add(TestSearch.class);
        
        sClasses.add(TestSendAndReceive.class);
        sClasses.add(TestConnectionPool.class);
        sClasses.add(TestLmtp.class);
        sClasses.add(TestMimeHandler.class);
        sClasses.add(TestScheduledTaskManager.class);
        sClasses.add(TestDataSource.class);
        sClasses.add(TestPurge.class);
        sClasses.add(TestImapImport.class);
        sClasses.add(TestNotification.class);
        sClasses.add(TestMaxMessageSize.class);
        sClasses.add(TestMetadata.class);
        sClasses.add(TestSoap.class);
        sClasses.add(TestBlobInputStream.class);
        sClasses.add(TestRedoLog.class);
        sClasses.add(TestFileUtil.class);
        sClasses.add(TestIndex.class);
        sClasses.add(TestParsedMessage.class);
    };
    
    /**
     * Used by extensions to add additional tests to the main test suite.
     */
    public static void addTest(Class<? extends TestCase> clazz) {
        sClasses.add(clazz);
    }
    
    public static void removeTest(Class<? extends TestCase> clazz) {
        sClasses.remove(clazz);
    }

    public static TestResult runUserTests(Element response, List<String> tests) throws ServiceException {
        TestSuite suite = new TestSuite();
        
        for (String test : tests) {
            try {
                if (test.indexOf('.') < 0) {
                    // short name...check the suite
                    boolean found = false;
                    for (Class<?> c : ZimbraSuite.sClasses) {
                        if (test.equals(c.getSimpleName())) {
                            suite.addTest(new TestSuite(c));
                        }
                    }
                    if (!found) {
                        ZimbraLog.test.warn("Could not find test %s.  Make sure it's registered with %s.",
                            test, ZimbraSuite.class.getName());
                    }
                } else {
                    // look it up by the full name
                    suite.addTest(new TestSuite(Class.forName(test)));
                }
            } catch (Exception e) {
                throw ServiceException.FAILURE("Error instantiating test "+test, e);
            }
        }

        return TestUtil.runTest(suite, response);
    }
    
    /**
     * Runs the entire test suite and writes the output to the specified
     * <code>OutputStream</code>.
     */
    public static TestResult runTestSuite(Element response) {
        TestSuite suite = new TestSuite();
        
        for (Class<? extends TestCase> c : ZimbraSuite.sClasses) {
            suite.addTest(new TestSuite(c));
        }
        
        return TestUtil.runTest(suite, response);
    }
}
