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
