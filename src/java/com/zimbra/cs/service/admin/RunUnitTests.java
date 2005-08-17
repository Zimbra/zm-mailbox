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

