/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
