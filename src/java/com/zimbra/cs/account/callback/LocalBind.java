/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.callback;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;

public class LocalBind extends AttributeCallback {

    @Override
    public void preModify(CallbackContext context, String attrName, Object attrValue,
            Map attrsToModify, Entry entry)
    throws ServiceException {
    }
    
    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
        if (entry instanceof Server) {
            Server server = (Server) entry;
            if (attrName.equals(Provisioning.A_zimbraAdminBindAddress)) {
                String address = server.getAttr(Provisioning.A_zimbraAdminBindAddress, true);
                try{
                    if ((address == null) || (address.isEmpty())) {
                        server.setAdminLocalBind(false);
                    } else {
                        InetAddress inetAddress;
                        try {
                            inetAddress = InetAddress.getByName(address);
                        } catch (UnknownHostException e) {
                            server.setAdminLocalBind(false);
                            return;
                        }
                        if (inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress())
                            server.setAdminLocalBind(false);
                        else
                            server.setAdminLocalBind(true);
                    }
                } catch (ServiceException se) {
                    ZimbraLog.misc.warn("Unable to update zimbraAdminLocalBind " + se);
                }  
            }
        }
    }
}