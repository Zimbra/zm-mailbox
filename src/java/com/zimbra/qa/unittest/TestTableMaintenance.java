package com.liquidsys.qa.unittest;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.account.Server;
import com.liquidsys.coco.client.LmcSession;
import com.liquidsys.coco.client.soap.LmcMaintainTablesRequest;
import com.liquidsys.coco.client.soap.LmcMaintainTablesResponse;
import com.liquidsys.coco.db.DbTableMaintenance;
import com.liquidsys.coco.db.DbUtil;
import com.liquidsys.coco.util.LiquidLog;

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
        LiquidLog.test.debug("testAnalyze");
        setServerAttrs(0, 1000000, DbTableMaintenance.OPERATION_ANALYZE, 0);
        int numTables = runMaintenance();
        assertTrue(numTables > 0);
    }

    public void testOptimize()
    throws Exception {
        LiquidLog.test.debug("testOptimize");
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
        LiquidLog.test.debug("testNotEnoughRows");
        setServerAttrs(1000000, 10000000, DbTableMaintenance.OPERATION_ANALYZE, 0);
        int numTables = runMaintenance();
        assertEquals(0, numTables);
    }
    
    public void testNotEnoughGrowth()
    throws Exception {
        LiquidLog.test.debug("testNotEnoughGrowth");
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
        Server server = Provisioning.getInstance().getLocalServer();
        Map map = new HashMap();
        map.put(Provisioning.A_liquidTableMaintenanceMinRows, Integer.toString(minRows));
        map.put(Provisioning.A_liquidTableMaintenanceMaxRows, Integer.toString(maxRows));
        map.put(Provisioning.A_liquidTableMaintenanceOperation, operation);
        map.put(Provisioning.A_liquidTableMaintenanceGrowthFactor,
            Integer.toString(growthFactor));
        server.modifyAttrs(map);
    }
}
