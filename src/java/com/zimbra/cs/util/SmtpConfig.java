/*
 * Created on Dec 23, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.util;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.Config;

/**
 * @author schemers
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class SmtpConfig {

    private int mTimeout;
    private int mPort;
    private String mHostname;
    
    public SmtpConfig() throws ServiceException {
        reload();
    }
    
    public void reload() throws ServiceException {
        Server config = Provisioning.getInstance().getLocalServer();
        mTimeout = config.getIntAttr(Provisioning.A_zimbraSmtpTimeout, Config.D_SMTP_TIMEOUT);
        mPort = config.getIntAttr(Provisioning.A_zimbraSmtpPort, Config.D_SMTP_PORT);
        mHostname = config.getAttr(Provisioning.A_zimbraSmtpHostname, null);
        if (mHostname == null) {
            throw ServiceException.FAILURE("no value for "+Provisioning.A_zimbraSmtpHostname, null);
        }
    }
    /**
     * @return Returns the hostname.
     */
    public String getHostname() {
        return mHostname;
    }
    /**
     * @return Returns the port.
     */
    public int getPort() {
        return mPort;
    }

    /**
     * @return Returns the timeout in seconds.
     */
    public int getTimeout() {
        return mTimeout;
    }
    
    /**
     * @return Returns the timeout in milliseconds.
     */
    public int getTimeoutMS() {
        return mTimeout*1000;
    }
}
