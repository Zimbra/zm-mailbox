/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;

/**
 * Complete unit test suite for the Zimbra code base.
 * 
 * @author bburtin
 *
 */
public class ZimbraSuite extends TestSuite
{
    private static List<Test> sAdditionalTests = new ArrayList<Test>();

    /**
     * Used by extensions to add additional tests to the main test suite.
     */
    public static void addAdditionalTest(Test test) {
        synchronized (sAdditionalTests) {
            sAdditionalTests.add(test);
        }
    }

    static final Class[] classes = {
        TestWaitSet.class,
        TestUtilCode.class,
        TestEmailUtil.class,
        TestOutOfOffice.class,
        TestDbUtil.class,
        TestUnread.class,
        TestTags.class,
        TestItemCache.class,
        TestFolders.class,
        TestSpellCheck.class,
        TestAuthentication.class,
        TestAccount.class,
        TestConversion.class,
        TestMailItem.class,
        TestConcurrency.class,
        TestFolderFilterRules.class,
        TestTagFilterRules.class,
        TestPop3Import.class,
        TestFilter.class,
        TestPop3ImapAuth.class,
        TestContacts.class,
        TestTaskScheduler.class,
        
        // XXX bburtin: commenting out TestSearch until bug 18802 is fixed
        // TestSearch.class,
        
        TestSendAndReceive.class,
        TestConnectionPool.class,
        TestLmtp.class,
        TestMimeHandler.class,
        TestScheduledTaskManager.class,
        TestDataSource.class,
        TestPurge.class,
        TestImapImport.class,
        TestImapImport.TearDown.class,
        TestNotification.class
    };
    
    public static TestResult runUserTests(Element response, List<String> tests) throws ServiceException {
        TestSuite suite = new TestSuite();
        
        for (String test : tests) {
            try {
                if (test.indexOf('.') < 0) {
                    // short name...check the suite
                    for (Class c : ZimbraSuite.classes) {
                        if (test.equals(c.getSimpleName()))
                            suite.addTest(new TestSuite(c));
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
        
        for (Class c : ZimbraSuite.classes) {
            suite.addTest(new TestSuite(c));
        }
        synchronized (sAdditionalTests) {
            for (Test additional : sAdditionalTests) {
                suite.addTest(additional);
            }
        }
        
        return TestUtil.runTest(suite, response);
    }
}
