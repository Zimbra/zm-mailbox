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
