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

package com.zimbra.cs.service.admin;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.TestResult;

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
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Element response = lc.createElement(AdminConstants.RUN_UNIT_TESTS_RESPONSE);
        
        List<String> testNames = null;
        for (Iterator iter = request.elementIterator(AdminConstants.E_TEST); iter.hasNext();) {
            if (testNames == null)
                testNames = new ArrayList<String>();
            Element e = (Element)iter.next();
            testNames.add(e.getText());
        }
        
        TestResult result;         
        if (testNames == null) 
            result = ZimbraSuite.runTestSuite(response);
        else 
            result = ZimbraSuite.runUserTests(response, testNames);
            
        
        response.addAttribute(AdminConstants.A_NUM_EXECUTED, Integer.toString(result.runCount()));
        response.addAttribute(AdminConstants.A_NUM_FAILED,
            Integer.toString(result.failureCount() + result.errorCount()));
        response.setText(os.toString());
        return response;
    }
}

