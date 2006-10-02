/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.client.LmcSession;
import com.zimbra.cs.client.soap.LmcMaintainTablesRequest;
import com.zimbra.cs.client.soap.LmcMaintainTablesResponse;
import com.zimbra.cs.db.DbTableMaintenance;
import com.zimbra.cs.db.DbUtil;
import com.zimbra.common.util.ZimbraLog;

/**
 * @author bburtin
 */
public class TestTableMaintenance extends TestCase {

    int mMinRows;
    int mMaxRows;
    String mOperation;
    int mRowCountFactor;
    
    public void setUp()
    throws Exception {
        // Remember current settings
        mMinRows = DbTableMaintenance.getMinRows();
        mMaxRows = DbTableMaintenance.getMaxRows();
        mOperation = DbTableMaintenance.getOperation();
        mRowCountFactor = DbTableMaintenance.getGrowthFactor();
    }
    
    public void testAnalyze()
    throws Exception {
        ZimbraLog.test.debug("testAnalyze");
        setServerAttrs(0, 1000000, DbTableMaintenance.OPERATION_ANALYZE, 0);
        int numTables = runMaintenance();
        assertTrue(numTables > 0);
    }

    public void testOptimize()
    throws Exception {
        ZimbraLog.test.debug("testOptimize");
        setServerAttrs(0, 1000000, DbTableMaintenance.OPERATION_OPTIMIZE, 0);
        int numTables = runMaintenance();
        assertTrue(numTables > 0);
    }

    // xxx bburtin: Commenting out negative tests.  I've seen a good amount of
    // flakiness with these tests, possibly due to LDAP variable caching issues.
    // We can reenable them when we need to do more work on the table maintenance
    // code.
    /*
    public void testNotEnoughRows()
    throws Exception {
        ZimbraLog.test.debug("testNotEnoughRows");
        setServerAttrs(1000000, 10000000, DbTableMaintenance.OPERATION_ANALYZE, 0);
        int numTables = runMaintenance();
        assertEquals(0, numTables);
    }
    
    public void testNotEnoughGrowth()
    throws Exception {
        ZimbraLog.test.debug("testNotEnoughGrowth");
        testAnalyze();
        setServerAttrs(0, 1000000, DbTableMaintenance.OPERATION_ANALYZE, 10);
        assertEquals(0, runMaintenance());
    }
    */
    
    public void tearDown()
    throws Exception {
        // Restore previous settings
        setServerAttrs(mMinRows, mMaxRows, mOperation, mRowCountFactor);
        
        // Clear out maintenance records
        DbUtil.executeUpdate("DELETE FROM " + DbTableMaintenance.TABLE_NAME);
    }

    private int runMaintenance()
    throws Exception {
        LmcSession session = TestUtil.getAdminSoapSession();
        LmcMaintainTablesRequest req = new LmcMaintainTablesRequest();
        req.setSession(session);
        LmcMaintainTablesResponse res = 
            (LmcMaintainTablesResponse)req.invoke(TestUtil.getAdminSoapUrl());
        return res.getNumTables();
    }
    
    private void setServerAttrs(int minRows, int maxRows,
                                String operation, int growthFactor)
    throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Server server = prov.getLocalServer();
        Map map = new HashMap();
        map.put(Provisioning.A_zimbraTableMaintenanceMinRows, Integer.toString(minRows));
        map.put(Provisioning.A_zimbraTableMaintenanceMaxRows, Integer.toString(maxRows));
        map.put(Provisioning.A_zimbraTableMaintenanceOperation, operation);
        map.put(Provisioning.A_zimbraTableMaintenanceGrowthFactor,
            Integer.toString(growthFactor));
        prov.modifyAttrs(server, map);
    }
}
