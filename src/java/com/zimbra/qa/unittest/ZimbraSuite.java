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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import com.zimbra.cs.util.ZimbraLog;

/**
 * Complete unit test suite for the Zimbra code base.
 * 
 * @author bburtin
 *
 */
public class ZimbraSuite extends TestSuite
{
    static final DecimalFormat TEST_TIME_FORMAT = new DecimalFormat("0.00");
    private static List<TestSuite> sAdditionalSuites = new ArrayList<TestSuite>();

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();

        suite.addTest(new TestSuite(TestUtilCode.class));
        suite.addTest(new TestSuite(TestEmailUtil.class));
        suite.addTest(new TestSuite(TestOutOfOffice.class));
        suite.addTest(new TestSuite(TestDbUtil.class));
        suite.addTest(new TestSuite(TestTableMaintenance.class));
        suite.addTest(new TestSuite(TestUnread.class));
        suite.addTest(new TestSuite(TestTags.class));
        suite.addTest(new TestSuite(TestItemCache.class));
        suite.addTest(new TestSuite(TestFolders.class));
        suite.addTest(new TestSuite(TestSpellCheck.class));
        suite.addTest(new TestSuite(TestAuthentication.class));
        suite.addTest(new TestSuite(TestAccount.class));
        suite.addTest(new TestSuite(TestConversion.class));
        suite.addTest(new TestSuite(TestMailItem.class));
        suite.addTest(new TestSuite(TestConcurrency.class));
        
        synchronized (sAdditionalSuites) {
            for (TestSuite additional : sAdditionalSuites) {
                suite.addTest(additional);
            }
        }
        
        return suite;
    }
    
    public static void addSuite(TestSuite suite) {
        synchronized (sAdditionalSuites) {
            sAdditionalSuites.add(suite);
        }
    }
    
    
    /**
     * Runs the entire test suite and writes the output to the specified
     * <code>OutputStream</code>.
     */
    public static TestResult runTestSuite(OutputStream outputStream) {
        ZimbraLog.test.debug("Starting unit test suite");
        
        long suiteStart = System.currentTimeMillis();
        TestResult result = new TestResult();
        ZimbraTestListener listener = new ZimbraTestListener();
        result.addListener(listener);
        TestSuite suite = ZimbraSuite.suite();
        
        Enumeration tests = suite.tests();
        while (tests.hasMoreElements()) {
            Test test = (Test) tests.nextElement();
            test.run(result);
        }
        
        double seconds = (double) (System.currentTimeMillis() - suiteStart) / 1000;
        String msg = "Unit test suite finished in " + TEST_TIME_FORMAT.format(seconds) +
            " seconds.  " + result.errorCount() + " errors, " + result.failureCount() +
            " failures.\n" + listener.getSummary();
        ZimbraLog.test.info(msg);
        try {
            outputStream.write(msg.getBytes());
        } catch (IOException e) {
            ZimbraLog.test.error(e.toString());
        }

        return result;
    }
}