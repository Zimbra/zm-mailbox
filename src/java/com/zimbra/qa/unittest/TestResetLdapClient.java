package com.zimbra.qa.unittest;

import org.junit.Before;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.soap.SoapProvisioning;

public class TestResetLdapClient {
    private SoapProvisioning prov;
    
    @Before
    public void setUp() throws ServiceException{
        prov = TestProvisioningUtil.getSoapProvisioning();
    }
    
    @Test
    public void testResetOnLocalhost() throws ServiceException{
        prov.resetLdapClient(false);
    }

    @Test
    public void testResetOnAllServers() throws ServiceException{
        prov.resetLdapClient(true);
    }
}
