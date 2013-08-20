/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.callback;

import java.io.IOException;

import org.dom4j.DocumentException;

import com.zimbra.common.localconfig.ConfigException;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.localconfig.LocalConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;

public class MailPort extends CheckPortConflict {

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
        super.postModify(context, attrName, entry);
        if (entry instanceof Server) {
            Server localServer = null;
            try {
                localServer = Provisioning.getInstance().getLocalServer();
                if (entry == localServer) {
                    String port = localServer.getAttr(attrName);
                    LocalConfig lc = new LocalConfig(null);
                    lc.set(LC.zimbra_mail_service_port.key(), port);
                    lc.save();
                }
            } catch (ServiceException | DocumentException | ConfigException | NumberFormatException | IOException e) {
                ZimbraLog.misc.warn("Unable to update LC.zimbra_mail_port due to Exception", e);
            }
        }
    }
}
