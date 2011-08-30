/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import com.google.common.collect.Lists;

public class TestResults extends RunListener {

    public static class Result {
        public final String className;
        public final String methodName;
        public final long execMillis;
        public final boolean success;
        public final String errorMessage;
        
        private Result(String className, String methodName, long execMillis, boolean success, String errorMessage) {
            this.className = className;
            this.methodName = methodName;
            this.execMillis = execMillis;
            this.success = success;
            this.errorMessage = errorMessage;
        }
    }

    private long lastTestStartTime;
    private boolean lastTestSucceeded;
    private String lastErrorMessage;
    
    private List<Result> results = Lists.newArrayList();

    public List<Result> getResults(boolean success) {
        List<Result> list = Lists.newArrayList();
        for (Result result : this.results) {
            if (result.success == success) {
                list.add(result);
            }
        }
        return list;
    }
    
    @Override
    public void testStarted(Description description) throws Exception {
        lastTestStartTime = System.currentTimeMillis();
        lastTestSucceeded = true;
        lastErrorMessage = null;
    }

    @Override
    public void testFinished(Description desc) throws Exception {
        results.add(new Result(desc.getClassName(), desc.getMethodName(),
            System.currentTimeMillis() - lastTestStartTime, lastTestSucceeded, lastErrorMessage));
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
        lastTestSucceeded = false;
        lastErrorMessage = failure.getMessage();
    }
}
