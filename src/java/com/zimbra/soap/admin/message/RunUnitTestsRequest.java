/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014 Zimbra, Inc.
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

package com.zimbra.soap.admin.message;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Runs the server-side unit test suite.
 * <br />
 * If <b>&lt;test></b>'s are specified, then run the requested tests (instead of the standard test suite).
 * Otherwise the standard test suite is run.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_RUN_UNIT_TESTS_REQUEST)
public class RunUnitTestsRequest {

    /**
     * @zm-api-field-tag classname super junit.framework.Test
     * @zm-api-field-description Test Names
     */
    @XmlElement(name=AdminConstants.E_TEST, required=false)
    private List<String> tests = Lists.newArrayList();

    public RunUnitTestsRequest() {
    }

    public void setTests(Iterable <String> tests) {
        this.tests.clear();
        if (tests != null) {
            Iterables.addAll(this.tests,tests);
        }
    }

    public RunUnitTestsRequest addTest(String test) {
        this.tests.add(test);
        return this;
    }

    public List<String> getTests() {
        return Collections.unmodifiableList(tests);
    }
}
