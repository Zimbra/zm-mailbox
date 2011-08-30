/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
import com.zimbra.qa.unittest.ZimbraSuite;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author bburtin
 */
public class RunUnitTests extends AdminDocumentHandler {

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
        
        TestResults results;          
        if (testNames == null) 
            results = ZimbraSuite.runTestSuite();
        else 
            results = ZimbraSuite.runUserTests(testNames);
            
        Element resultsElement = response.addElement("results");
        
        int numPassed = 0;
        for (TestResults.Result result : results.getResults(true)) {
            Element completedElement = resultsElement.addElement("completed");
            completedElement.addAttribute("name", result.methodName);
            double execSeconds = (double) result.execMillis / 1000;
            completedElement.addAttribute("execSeconds", String.format("%.2f", execSeconds));
            completedElement.addAttribute("class", result.className);
            numPassed++;
        }
        
        int numFailed = 0;
        for (TestResults.Result result : results.getResults(false)) {
            Element failureElement = resultsElement.addElement("failure");
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
        response.addAttribute(AdminConstants.A_NUM_FAILED, numFailed);
        return response;
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(AdminRightCheckPoint.Notes.ALLOW_ALL_ADMINS);
    }
}