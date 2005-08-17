package com.zimbra.cs.scan;

import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.service.ServiceException;

public class ScanConfig {

    private boolean mEnabled;
    
    private String mClass;
    
    private String mURL;
    
    public ScanConfig() throws ServiceException {
        reload();
    }
    
    public void reload() throws ServiceException {
        Config globalConfig = Provisioning.getInstance().getConfig();
        mEnabled = globalConfig.getBooleanAttr(Provisioning.A_zimbraAttachmentsScanEnabled, false);
        mClass = globalConfig.getAttr(Provisioning.A_zimbraAttachmentsScanClass);
        
        Server serverConfig = Provisioning.getInstance().getLocalServer();
        mURL = serverConfig.getAttr(Provisioning.A_zimbraAttachmentsScanURL);
    }

    public boolean getEnabled() {
        return mEnabled;
    }
    
    public String getClassName() {
        return mClass;
    }

    public String getURL() {
        return mURL;
    }
}
