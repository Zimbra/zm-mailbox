/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.scan;

import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.service.ServiceException;

public class ClamScannerConfig {

    private boolean mEnabled;
    
    private String mURL;
    
    public ClamScannerConfig() throws ServiceException {
        reload();
    }
    
    public void reload() throws ServiceException {
        Config globalConfig = Provisioning.getInstance().getConfig();
        mEnabled = globalConfig.getBooleanAttr(Provisioning.A_zimbraAttachmentsScanEnabled, false);
        
        Server serverConfig = Provisioning.getInstance().getLocalServer();
        mURL = serverConfig.getAttr(Provisioning.A_zimbraAttachmentsScanURL);
    }

    public boolean getEnabled() {
        return mEnabled;
    }
    
    public String getURL() {
        return mURL;
    }
}
