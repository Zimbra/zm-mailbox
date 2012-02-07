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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.TestResultInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_RUN_UNIT_TESTS_RESPONSE)
public class RunUnitTestsResponse {

    /**
     * @zm-api-field-description Information about test results
     */
    @XmlElement(name="results", required=true)
    private final TestResultInfo results;

    /**
     * @zm-api-field-tag num-executed
     * @zm-api-field-description Number of executed tests
     */
    @XmlAttribute(name=AdminConstants.A_NUM_EXECUTED, required=true)
    private final int numExecuted;

    /**
     * @zm-api-field-tag num-failed
     * @zm-api-field-description Number of failed tests
     */
    @XmlAttribute(name=AdminConstants.A_NUM_FAILED, required=true)
    private final int numFailed;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private RunUnitTestsResponse() {
        this((TestResultInfo) null, -1, -1);
    }

    public RunUnitTestsResponse(TestResultInfo results,
                int numExecuted, int numFailed) {
        this.results = results;
        this.numExecuted = numExecuted;
        this.numFailed = numFailed;
    }

    public TestResultInfo getResults() { return results; }
    public int getNumExecuted() { return numExecuted; }
    public int getNumFailed() { return numFailed; }
}
