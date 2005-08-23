/*
***** BEGIN LICENSE BLOCK *****
Version: ZPL 1.1

The contents of this file are subject to the Zimbra Public License
Version 1.1 ("License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.zimbra.com/license

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
the License for the specific language governing rights and limitations
under the License.

The Original Code is: Zimbra Collaboration Suite.

The Initial Developer of the Original Code is Zimbra, Inc.  Portions
created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
Reserved.

Contributor(s): 

***** END LICENSE BLOCK *****
*/

package com.zimbra.cs.service.admin;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import junit.framework.TestResult;

import com.zimbra.cs.service.Element;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.qa.unittest.ZimbraSuite;
import com.zimbra.soap.ZimbraContext;
import com.zimbra.soap.WriteOpDocumentHandler;

/**
 * @author bburtin
 */
public class RunUnitTests extends WriteOpDocumentHandler {
    
	public Element handle(Element request, Map context) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ZimbraLog.test.debug("Running unit test suite");
        TestResult result = ZimbraSuite.runTestSuite(os);
        ZimbraLog.test.debug("Test results:\n" + os);
        
        ZimbraContext lc = getZimbraContext(context);
        Element response = lc.createElement(AdminService.RUN_UNIT_TESTS_RESPONSE);
        response.addAttribute(AdminService.A_NUM_EXECUTED, Integer.toString(result.runCount()));
        response.addAttribute(AdminService.A_NUM_FAILED,
            Integer.toString(result.failureCount() + result.errorCount()));
        response.addAttribute(AdminService.A_OUTPUT, os.toString());
    	return response;
	}
}

