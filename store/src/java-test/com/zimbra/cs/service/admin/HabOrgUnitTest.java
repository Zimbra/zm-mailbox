/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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

package com.zimbra.cs.service.admin;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestName;
import org.junit.rules.TestWatchman;
import org.junit.runners.model.FrameworkMethod;

import junit.framework.Assert;


/**
 * @author zimbra
 *
 */
public class HabOrgUnitTest {

    @Rule
    public TestName testName = new TestName();
    @Rule
    public MethodRule watchman = new TestWatchman() {

        @Override
        public void failed(Throwable e, FrameworkMethod method) {
            System.out.println(method.getName() + " " + e.getClass().getSimpleName());
        }
    };


    @Test
    public void testCreateOuDn() {
        String expected =  "ou=zimbraHAB,dc=ztest,dc=zdev,dc=local";
        String result = HabOrgUnit.createOuDn("zimbraHAB","dc=ztest,dc=zdev,dc=local");
        Assert.assertEquals(expected, result);
    }
}
