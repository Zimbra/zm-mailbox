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

package com.zimbra.cs.service.admin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.qa.unittest.ZimbraSuite;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author bburtin
 */
public class RunUnitTests extends AdminDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Element response = lc.createElement(AdminConstants.RUN_UNIT_TESTS_RESPONSE);
        
        List<String> testNames = null;
        for (Iterator<Element> iter = request.elementIterator(AdminConstants.E_TEST); iter.hasNext();) {
            if (testNames == null)
                testNames = new ArrayList<String>();
            Element e = iter.next();
            testNames.add(e.getText());
        }
        
        TestListenerAdapter results;          
        if (testNames == null) 
            results = ZimbraSuite.runTestSuite();
        else 
            results = ZimbraSuite.runUserTests(testNames);
            
        Element resultsElement = response.addElement("results");
        
        int numPassed = 0;
        for (ITestResult result : results.getPassedTests()) {
            Element completedElement = resultsElement.addElement("completed");
            completedElement.addAttribute("name", result.getName());
            double execSeconds = (double) (result.getEndMillis() - result.getStartMillis()) / 1000;
            completedElement.addAttribute("execSeconds", String.format("%.2f", execSeconds));
            completedElement.addAttribute("class", result.getTestClass().getName());
            numPassed++;
        }
        
        int numFailed = 0;
        for (ITestResult result : results.getFailedTests()) {
            Element failureElement = resultsElement.addElement("failure");
            failureElement.addAttribute("name", result.getName());
            double execSeconds = (double) (result.getEndMillis() - result.getStartMillis()) / 1000;
            failureElement.addAttribute("execSeconds", String.format("%.2f", execSeconds));
            if (result.getThrowable() != null) {
                failureElement.setText(result.getThrowable().toString());
            }
            failureElement.addAttribute("class", result.getTestClass().getName());
            numFailed++;
        }
        
        response.addAttribute(AdminConstants.A_NUM_EXECUTED, numPassed + numFailed);
        response.addAttribute(AdminConstants.A_NUM_FAILED, numFailed);
        return response;
    }
}

