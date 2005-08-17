/*
 * Created on Dec 23, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.liquidsys.coco.lmtpserver;

import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.account.Server;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.util.Config;

/**
 * @author schemers
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class LmtpConfig {

    private int mNumThreads;
    private int mPort;
    private String mBindAddress;
    private String mAdvertisedName;
    
    public LmtpConfig() throws ServiceException {
        reload();
    }
    
    public void reload() throws ServiceException {
        Server config = Provisioning.getInstance().getLocalServer();
        mNumThreads = config.getIntAttr(Provisioning.A_liquidLmtpNumThreads, Config.D_LMTP_THREADS);
        mPort = config.getIntAttr(Provisioning.A_liquidLmtpBindPort, Config.D_LMTP_BIND_PORT);
        mBindAddress = config.getAttr(Provisioning.A_liquidLmtpBindAddress, Config.D_LMTP_BIND_ADDRESS);
        mAdvertisedName = config.getAttr(Provisioning.A_liquidLmtpAdvertisedName, Config.D_LMTP_ANNOUNCE_NAME);
    }
    /**
     * @return Returns the advertisedName.
     */
    public String getAdvertisedName() {
        return mAdvertisedName;
    }
    /**
     * @return Returns the bindAddress.
     */
    public String getBindAddress() {
        return mBindAddress;
    }
    /**
     * @return Returns the numThreads.
     */
    public int getNumThreads() {
        return mNumThreads;
    }
    /**
     * @return Returns the port.
     */
    public int getPort() {
        return mPort;
    }
}
