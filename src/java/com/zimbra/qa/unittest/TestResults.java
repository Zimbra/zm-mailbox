/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.internal.AssumptionViolatedException;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import com.google.common.collect.Lists;

public class TestResults extends RunListener {
	public enum TestStatus {SUCCESS,FAILURE,SKIPPED,IGNORED};
	
    public static class Result {
        public final String className;
        public final String methodName;
        public final long execMillis;
        public final String errorMessage;
        public final TestStatus status;
        
        private Result(String className, String methodName, long execMillis, String errorMessage, TestStatus status) {
            this.className = className;
            this.methodName = methodName;
            this.execMillis = execMillis;
            this.errorMessage = errorMessage;
            this.status = status;
        }
    }

    private long lastTestStartTime;
    private String lastErrorMessage;
    private List<Result> results = Lists.newArrayList();
    private Map<String,TestStatus> statusMap = new HashMap<String,TestStatus>();
    
    public List<Result> getResults(TestStatus s) {
    	List<Result> list = Lists.newArrayList();
    	for (Result result : this.results) {
            if (result.status == s) {
                list.add(result);
            }
        }
        return list;
    }
    
    @Override
    public void testStarted(Description description) throws Exception {
        lastTestStartTime = System.currentTimeMillis();
        lastErrorMessage = null;
        statusMap.put(description.getClassName(), TestStatus.SUCCESS);
    }

    @Override
    public void testFinished(Description desc) throws Exception {
        results.add(new Result(desc.getClassName(), desc.getMethodName(),
            System.currentTimeMillis() - lastTestStartTime, lastErrorMessage,statusMap.get(desc.getClassName())));
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
    	String className = failure.getDescription().getClassName();
		statusMap.put(className, TestStatus.FAILURE);
		lastErrorMessage = failure.getMessage();
    }
    
    @Override
	public void testAssumptionFailure(Failure failure) {
    	String className = failure.getDescription().getClassName();
		statusMap.put(className, TestStatus.SKIPPED);
		lastErrorMessage = failure.getMessage();
	}
}
