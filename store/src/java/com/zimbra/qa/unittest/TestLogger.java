/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import com.zimbra.common.util.ZimbraLog;

public class TestLogger extends RunListener {

    @Override
    public void testAssumptionFailure(Failure failure) {
        Description desc = failure.getDescription();
        ZimbraLog.test.info("Test %s.%s skipped.", desc.getClassName(), desc.getMethodName());
    }

    @Override
    public void testRunStarted(Description description) throws Exception {
        ZimbraLog.test.info("Starting test suite.");
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
        ZimbraLog.test.info("Finished test suite.");
    }

    @Override
    public void testStarted(Description desc) throws Exception {
        ZimbraLog.test.info("Starting test %s.%s.", desc.getClassName(), desc.getMethodName());
    }

    @Override
    public void testFinished(Description desc) throws Exception {
        ZimbraLog.test.info("Test %s.%s completed.", desc.getClassName(), desc.getMethodName());
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
        Description desc = failure.getDescription();
        ZimbraLog.test.error("Test %s.%s failed.", desc.getClassName(), desc.getMethodName(), failure.getException());
    }

    @Override
    public void testIgnored(Description desc) throws Exception {
        ZimbraLog.test.info("Test %s.%s ignored.", desc.getClassName(), desc.getMethodName());
    }
}
