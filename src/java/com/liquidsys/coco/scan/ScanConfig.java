package com.liquidsys.coco.scan;

import com.liquidsys.coco.account.Config;
import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.account.Server;
import com.liquidsys.coco.service.ServiceException;

public class ScanConfig {

    private boolean mEnabled;
    
    private String mClass;
    
    private String mURL;
    
    public ScanConfig() throws ServiceException {
        reload();
    }
    
    public void reload() throws ServiceException {
        Config globalConfig = Provisioning.getInstance().getConfig();
        mEnabled = globalConfig.getBooleanAttr(Provisioning.A_liquidAttachmentsScanEnabled, false);
        mClass = globalConfig.getAttr(Provisioning.A_liquidAttachmentsScanClass);
        
        Server serverConfig = Provisioning.getInstance().getLocalServer();
        mURL = serverConfig.getAttr(Provisioning.A_liquidAttachmentsScanURL);
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
