/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestListener;

import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;

public class ZimbraTestListener implements TestListener {
    
    long mStartTimestamp;
    Element mSummaryElement;
    boolean addedName = false;
    
    ZimbraTestListener(Element parent) {
        if (parent != null) {
            mSummaryElement = parent.addElement("results");
        }
    }
    
    public Element getSummaryElement() {
        return mSummaryElement;
    }
    
    public void addError(Test test, Throwable t) {
        ZimbraLog.test.error("*** TEST ERROR", t);

        if (mSummaryElement != null) {
            Element e = mSummaryElement.addElement("error");
            e.addAttribute("name", getTestName(test));
            e.setText(t.toString());
        }
    }

    public void addFailure(Test test, AssertionFailedError t) {
        ZimbraLog.test.error("*** TEST FAILED", t);
        
        if (mSummaryElement != null) {
            Element e = mSummaryElement.addElement("failure");
            e.addAttribute("name", getTestName(test));
            e.setText(t.toString());
        }
    }

    public void endTest(Test test) {
        if (test instanceof TestCase) {
            double seconds = (double) (System.currentTimeMillis() - mStartTimestamp) / 1000;
            ZimbraLog.test.info("Test %s finished in %.2f seconds", getTestName(test), seconds);
            
            if (mSummaryElement != null) {
                Element e = mSummaryElement.addElement("completed");
                e.addAttribute("name", getTestName(test));
                e.addAttribute("execSeconds", String.format("%.2f", seconds));
            }
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
