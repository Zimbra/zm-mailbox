/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.admin.type;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"completedTests", "failedTests"})
public class TestResultInfo {

    /**
     * @zm-api-field-description Information for completed tests
     */
    @XmlElement(name="completed", required=false)
    private List<CompletedTestInfo> completedTests = Lists.newArrayList();

    /**
     * @zm-api-field-description Information for failed tests
     */
    @XmlElement(name="failure", required=false)
    private List<FailedTestInfo> failedTests = Lists.newArrayList();

    public TestResultInfo() {
    }

    public void setCompletedTests(Iterable <CompletedTestInfo> completedTests) {
        this.completedTests.clear();
        if (completedTests != null) {
            Iterables.addAll(this.completedTests,completedTests);
        }
    }

    public TestResultInfo addCompletedTest(CompletedTestInfo completedTest) {
        this.completedTests.add(completedTest);
        return this;
    }


    public void setFailedTests(Iterable <FailedTestInfo> failedTests) {
        this.failedTests.clear();
        if (failedTests != null) {
            Iterables.addAll(this.failedTests,failedTests);
        }
    }

    public TestResultInfo addFailedTest(FailedTestInfo failedTest) {
        this.failedTests.add(failedTest);
        return this;
    }

    public List<CompletedTestInfo> getCompletedTests() {
        return Collections.unmodifiableList(completedTests);
    }
    public List<FailedTestInfo> getFailedTests() {
        return Collections.unmodifiableList(failedTests);
    }
}
