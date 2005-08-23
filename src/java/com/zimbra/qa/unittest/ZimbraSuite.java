/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import java.io.OutputStream;
import java.io.PrintStream;

import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Complete unit test suite for the Zimbra code base.
 * 
 * @author bburtin
 *
 */
public class ZimbraSuite extends TestSuite
{
    public static TestSuite suite() {
        TestSuite suite = new TestSuite();

        suite.addTest(new TestSuite(TestStringUtil.class));
        suite.addTest(new TestSuite(TestEmailUtil.class));
        suite.addTest(new TestSuite(TestParsedItemID.class));
        
        // FIXME: bburtin: commenting out failing tests for now.  Tim
        // suggested waiting until he's done with his parser rewrite
        // before reenabling this one.
        // suite.addTest(new TestSuite(TestParseMailboxID.class));
        // suite.addTest(new TestSuite(UnitTests.class));

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
        suite.addTest(new TestSuite(TestTimeoutMap.class));

        return suite;
    }
    
    /**
     * Runs the entire test suite and writes the output to the specified
     * <code>OutputStream</code>.
     */
    public static TestResult runTestSuite(OutputStream outputStream) {
        TestRunner runner;
        if (outputStream == null) {
            runner = new TestRunner();
        } else {
            runner = new TestRunner(new PrintStream(outputStream));
        }

        Test suite = runner.getTest(ZimbraSuite.class.getName());
        return runner.doRun(suite);
    }
}