/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.service.admin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.qa.unittest.TestResults;
import com.zimbra.qa.unittest.TestUtil;
import com.zimbra.qa.unittest.ZimbraSuite;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.qa.unittest.TestResults.TestStatus;
/**
 * @author bburtin
 */
public class RunUnitTests extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        try {
            Class.forName("org.junit.Test");
        } catch (ClassNotFoundException e) {
            throw ServiceException.FAILURE("JUnit is not installed.  Unable to run unit tests.", e);
        }

        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Element response = lc.createElement(AdminConstants.RUN_UNIT_TESTS_RESPONSE);

        List<String> testNames = null;
        for (Iterator<Element> iter = request.elementIterator(AdminConstants.E_TEST); iter.hasNext();) {
            if (testNames == null)
                testNames = new ArrayList<String>();
            Element e = iter.next();
            testNames.add(e.getText());
        }

        TestUtil.fromRunUnitTests = true;
        TestResults results;
        if (testNames == null)
            results = ZimbraSuite.runTestSuite();
        else
            results = ZimbraSuite.runUserTests(testNames);

        Element resultsElement = response.addNonUniqueElement("results");

        int numPassed = 0;
        for (TestResults.Result result : results.getResults(TestStatus.SUCCESS)) {
            Element completedElement = resultsElement.addNonUniqueElement("completed");
            completedElement.addAttribute("name", result.methodName);
            double execSeconds = (double) result.execMillis / 1000;
            completedElement.addAttribute("execSeconds", String.format("%.2f", execSeconds));
            completedElement.addAttribute("class", result.className);
            numPassed++;
        }

        int numSkipped = 0;
        List<TestResults.Result> skippedTests = results.getResults(TestStatus.SKIPPED);
        for (TestResults.Result result : skippedTests) {
            Element skippedElement = resultsElement.addNonUniqueElement("skipped");
            skippedElement.addAttribute("name", result.methodName);
            if (result.errorMessage != null) {
                skippedElement.setText(result.errorMessage);
            }
            skippedElement.addAttribute("class", result.className);
            numSkipped++;
        }

        int numFailed = 0;
        for (TestResults.Result result : results.getResults(TestStatus.FAILURE)) {
            Element failureElement = resultsElement.addNonUniqueElement("failure");
            failureElement.addAttribute("name", result.methodName);
            double execSeconds = (double) result.execMillis / 1000;
            failureElement.addAttribute("execSeconds", String.format("%.2f", execSeconds));
            if (result.errorMessage != null) {
                failureElement.setText(result.errorMessage);
            }
            failureElement.addAttribute("class", result.className);
            numFailed++;
        }

        response.addAttribute(AdminConstants.A_NUM_EXECUTED, numPassed + numFailed);
        response.addAttribute(AdminConstants.A_NUM_SKIPPED, numSkipped);
        response.addAttribute(AdminConstants.A_NUM_FAILED, numFailed);
        return response;
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(AdminRightCheckPoint.Notes.ALLOW_ALL_ADMINS);
    }
}