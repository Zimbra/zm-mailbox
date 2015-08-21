/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
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

package com.zimbra.qa.unittest.prov.soap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.soap.SoapTransport;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.soap.admin.type.DataSourceType;
import com.zimbra.soap.mail.message.GetDataSourcesRequest;
import com.zimbra.soap.mail.message.GetDataSourcesResponse;
import com.zimbra.soap.type.DataSource.ConnectionType;

/**
 * @author zimbra
 *
 */
public class TestDataSource extends SoapTest {

    private static SoapProvTestUtil provUtil;
    private static Provisioning prov;
    private static Domain domain;
    private static Account acct;

    @BeforeClass
    public static void init() throws Exception {
        provUtil = new SoapProvTestUtil();
        prov = provUtil.getProv();
        domain = provUtil.createDomain(baseDomainName());
        acct = provUtil.createAccount("acct", domain);
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (acct != null) { // test null in case init failed
            provUtil.deleteAccount(acct);
        }
        Cleanup.deleteAll(baseDomainName());
    }

    @Test
    public void testDatasourcePollingInterval() throws Exception{
        SoapTransport transport = authUser(acct.getName());
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceEnabled, LdapConstants.LDAP_FALSE);
        attrs.put(Provisioning.A_zimbraDataSourceHost, "testhost");
        attrs.put(Provisioning.A_zimbraDataSourcePort, "0");
        attrs.put(Provisioning.A_zimbraDataSourceUsername, "testuser");
        attrs.put(Provisioning.A_zimbraDataSourcePassword, "testpass");
        attrs.put(Provisioning.A_zimbraDataSourceFolderId, "1");
        attrs.put(Provisioning.A_zimbraDataSourcePollingInterval, "5m");
        attrs.put(Provisioning.A_zimbraDataSourceConnectionType, ConnectionType.cleartext.toString());
        DataSource ds = prov.createDataSource(acct, DataSourceType.pop3, "" + " testPollingInterval", attrs);
        GetDataSourcesRequest getDsReq = new GetDataSourcesRequest();
        GetDataSourcesResponse getDsRes = invokeJaxb(transport, getDsReq);
        List<com.zimbra.soap.type.DataSource> listDS = getDsRes.getDataSources();
        Assert.assertEquals("300000ms", listDS.get(0).getPollingInterval());
        attrs.put(Provisioning.A_zimbraDataSourcePollingInterval, listDS.get(0).getPollingInterval());
        prov.modifyDataSource(acct, ds.getId(), attrs);
        getDsRes = invokeJaxb(transport, getDsReq);
        listDS = getDsRes.getDataSources();
        Assert.assertEquals("300000ms", listDS.get(0).getPollingInterval());
    }

}
