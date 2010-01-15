/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.qa.unittest;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import com.zimbra.common.util.ZimbraLog;

public class TestLogger implements ITestListener {

    public void onFinish(ITestContext context) {
        ZimbraLog.test.info("Finished test suite.");
    }

    public void onStart(ITestContext context) {
        ZimbraLog.test.info("Starting test suite.");
    }

    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        ZimbraLog.test.warn("Test %s.%s failed but within success percentage.",
            result.getTestClass().getName(), result.getName(), result.getThrowable());
    }

    public void onTestFailure(ITestResult result) {
        ZimbraLog.test.error("Test %s.%s failed.", result.getTestClass().getName(), result.getName(), result.getThrowable());
    }

    public void onTestSkipped(ITestResult result) {
        ZimbraLog.test.info("Test %s.%s skipped.", result.getTestClass().getName(), result.getName());
    }

    public void onTestStart(ITestResult result) {
        ZimbraLog.test.info("Starting test %s.%s.", result.getTestClass().getName(), result.getName());
    }

    public void onTestSuccess(ITestResult result) {
        ZimbraLog.test.info("Test %s.%s succeeded.", result.getTestClass().getName(), result.getName());
    }
}
