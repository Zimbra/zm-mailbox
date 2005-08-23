/*
***** BEGIN LICENSE BLOCK *****
Version: ZPL 1.1

The contents of this file are subject to the Zimbra Public License
Version 1.1 ("License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.zimbra.com/license

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
the License for the specific language governing rights and limitations
under the License.

The Original Code is: Zimbra Collaboration Suite.

The Initial Developer of the Original Code is Zimbra, Inc.  Portions
created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
Reserved.

Contributor(s): 

***** END LICENSE BLOCK *****
*/

/*
 * Created on Dec 23, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.lmtpserver;

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
        mNumThreads = config.getIntAttr(Provisioning.A_zimbraLmtpNumThreads, Config.D_LMTP_THREADS);
        mPort = config.getIntAttr(Provisioning.A_zimbraLmtpBindPort, Config.D_LMTP_BIND_PORT);
        mBindAddress = config.getAttr(Provisioning.A_zimbraLmtpBindAddress, Config.D_LMTP_BIND_ADDRESS);
        mAdvertisedName = config.getAttr(Provisioning.A_zimbraLmtpAdvertisedName, Config.D_LMTP_ANNOUNCE_NAME);
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
