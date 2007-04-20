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

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestListener;

import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.SystemUtil;
import com.zimbra.common.util.ZimbraLog;

public class ZimbraTestListener implements TestListener {
    
    long mStartTimestamp;
    StringBuffer mSummary = new StringBuffer();
    Element mSummaryElement;
    boolean addedName = false;
    
    ZimbraTestListener(Element parent) {
        if (parent != null) {
            mSummaryElement = parent.addElement("results");
        }
    }
    
    public String getSummary() {
        return mSummary.toString();
    }
    
    public Element getSummaryElement() {
        return mSummaryElement;
    }
    
    public void addError(Test test, Throwable t) {
        ZimbraLog.test.error("*** TEST ERROR: " + t + "\n" + SystemUtil.getStackTrace(t));
        StringBuilder summary = new StringBuilder().append("*** TEST " + getTestName(test) + " generated an error: " + t + "\n");
        Element e = mSummaryElement.addElement("error");
        e.addAttribute("name", getTestName(test));
        e.setText(summary.toString());
    }

    public void addFailure(Test test, AssertionFailedError t) {
        ZimbraLog.test.error("*** TEST FAILED: " + t + "\n" + SystemUtil.getStackTrace(t));
        StringBuilder summary = new StringBuilder().append("*** TEST " + getTestName(test) + " failed: " + t + "\n");
        Element e = mSummaryElement.addElement("failure");
        e.addAttribute("name", getTestName(test));
        e.setText(summary.toString());
    }

    public void endTest(Test test) {
        if (test instanceof TestCase) {
            double seconds = (double) (System.currentTimeMillis() - mStartTimestamp) / 1000;
            String msg = "Test " + getTestName(test) + " finished in " +
                ZimbraSuite.TEST_TIME_FORMAT.format(seconds) + " seconds";
            ZimbraLog.test.info(msg);
            StringBuilder summary = new StringBuilder().append(msg).append("\n");
            Element e = mSummaryElement.addElement("completed");
            e.addAttribute("name", getTestName(test));
            e.setText(summary.toString());
        }
    }

    public void startTest(Test test) {
        if (test instanceof TestCase) {
            ZimbraLog.test.info("Starting test " + ((TestCase) test).getName());
            mStartTimestamp = System.currentTimeMillis();
        }
    }

    private String getTestName(Test test) {
        if (test instanceof TestCase) {
            return ((TestCase) test).getName();
        }
        else return StringUtil.getSimpleClassName(test);
    }
}
