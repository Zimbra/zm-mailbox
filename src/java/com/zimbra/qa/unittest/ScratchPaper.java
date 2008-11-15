package com.zimbra.qa.unittest;

import junit.framework.TestCase;

import com.zimbra.common.util.CliUtil;

import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZGrant;
import com.zimbra.cs.zclient.ZMailbox;

public class ScratchPaper extends TestCase  {
    
    public void test1() throws Exception {
        ZMailbox zmbx = TestUtil.getZMailbox("user1");
        
        String folderPath = "/user1@phoebe.mac/inbox";
        ZFolder folder = zmbx.getFolderByPath(folderPath);
        
        System.out.println("folder path = " + folderPath);
        System.out.println("folder id = " + folder.getId());
    }
    
    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup("INFO");
        // ZimbraLog.toolSetupLog4j("DEBUG", "/Users/pshao/sandbox/conf/log4j.properties.phoebe");
        
        TestUtil.runTest(ScratchPaper.class);
    }
    
}
