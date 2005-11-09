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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.util;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.Config;

public class SmtpConfig {

    private int mTimeout;
    private int mPort;
    private String mHostname;
    private boolean mSendPartial;
    
    public SmtpConfig() throws ServiceException {
        reload();
    }
    
    public void reload() throws ServiceException {
        Server config = Provisioning.getInstance().getLocalServer();
        mTimeout = config.getIntAttr(Provisioning.A_zimbraSmtpTimeout, Config.D_SMTP_TIMEOUT);
        mPort = config.getIntAttr(Provisioning.A_zimbraSmtpPort, Config.D_SMTP_PORT);
        mHostname = config.getAttr(Provisioning.A_zimbraSmtpHostname, null);
        mSendPartial = config.getBooleanAttr(Provisioning.A_zimbraSmtpSendPartial, false);
        if (mHostname == null) {
            throw ServiceException.FAILURE("no value for "+Provisioning.A_zimbraSmtpHostname, null);
        }
    }

    public String getHostname() {
        return mHostname;
    }

    public int getPort() {
        return mPort;
    }

    public boolean getSendPartial() {
    	return mSendPartial;
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
