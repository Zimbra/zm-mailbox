/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;

public class TestDomain extends TestCase {
    private Provisioning mProv  = Provisioning.getInstance();
    private String DOMAIN_NAME = "testautporov.domain.com";
    Domain domain = null;
    @Override
    @Before
    public void setUp() throws Exception {
        // create domain
        Map<String, Object> attrs = new HashMap<String, Object>();
        domain = mProv.createDomain(DOMAIN_NAME, attrs);
        assertNotNull(domain);
    }


    @Override
    @After
    public void tearDown() throws Exception {
        if(domain != null) {
            mProv.deleteDomain(domain.getId());
        }
    }

    @Test
    public void testModifyAutoProvTimeStamp() {
        try {
            domain.setAutoProvLastPolledTimestampAsString("20140328000454.349Z");
            assertEquals("domain has incorrect last prov time stamp", "20140328000454.349Z",domain.getAutoProvLastPolledTimestampAsString());

            domain.setAutoProvLastPolledTimestampAsString("20140328000454Z");
            assertEquals("domain has incorrect last prov time stamp", "20140328000454Z",domain.getAutoProvLastPolledTimestampAsString());

            domain.setAutoProvLastPolledTimestampAsString("20140328000454.0Z");
            assertEquals("domain has incorrect last prov time stamp", "20140328000454.0Z",domain.getAutoProvLastPolledTimestampAsString());
        } catch (ServiceException e) {
            fail(e.getMessage());
        }
        try {
            domain.setAutoProvLastPolledTimestampAsString("20140328000454.123456Z");
            fail("invalid date-time format should cause an exception");
        } catch (ServiceException e) {
            //should throw an exception
            assertEquals("catching a wrong exception: " + e.getMessage(), e.getCode(),AccountServiceException.INVALID_ATTR_VALUE);
        }
    }

}
