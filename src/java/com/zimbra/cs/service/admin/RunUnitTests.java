package com.liquidsys.coco.service.admin;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import junit.framework.TestResult;

import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.util.LiquidLog;
import com.liquidsys.qa.unittest.LiquidSuite;
import com.zimbra.soap.LiquidContext;
import com.zimbra.soap.WriteOpDocumentHandler;

/**
 * @author bburtin
 */
public class RunUnitTests extends WriteOpDocumentHandler {
    
	public Element handle(Element request, Map context) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        LiquidLog.test.debug("Running unit test suite");
        TestResult result = LiquidSuite.runTestSuite(os);
        LiquidLog.test.debug("Test results:\n" + os);
        
        LiquidContext lc = getLiquidContext(context);
        Element response = lc.createElement(AdminService.RUN_UNIT_TESTS_RESPONSE);
        response.addAttribute(AdminService.A_NUM_EXECUTED, Integer.toString(result.runCount()));
        response.addAttribute(AdminService.A_NUM_FAILED,
            Integer.toString(result.failureCount() + result.errorCount()));
        response.addAttribute(AdminService.A_OUTPUT, os.toString());
    	return response;
	}
}

