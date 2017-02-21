/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;


public class MailMode extends LocalBind {
    static final String ERROR_MM_W_IP_SEC = "Only \"https\" and \"both\" are valid modes when requiring interprocess security with web proxy.";
    static final String ERROR_MM_WO_IP_SEC = "Only \"http\" and \"both\" are valid modes when not requiring interprocess security with web proxy.";
    @Override
    public void preModify(CallbackContext context, String attrName, Object attrValue,
            Map attrsToModify, Entry entry)
    throws ServiceException {
        SingleValueMod mod = singleValueMod(attrsToModify, attrName);
        if (mod.setting()) {
            String zimbraMailMode = mod.value();
            if (isReverseProxySSLToUpstreamEnabled(entry)) {
                if (!(zimbraMailMode.equals("https") || zimbraMailMode.equals("both"))) {
                    throw ServiceException.INVALID_REQUEST(ERROR_MM_W_IP_SEC, null);
                }
            }
            else if (!(zimbraMailMode.equals("http") || zimbraMailMode.equals("both"))) {
                throw ServiceException.INVALID_REQUEST(ERROR_MM_WO_IP_SEC, null);
            }
        }
    }

    private boolean isReverseProxySSLToUpstreamEnabled (Entry entry)
    throws ServiceException {
        if (entry instanceof Server) {
            Server server = (Server) entry;
            return server.isReverseProxySSLToUpstreamEnabled();
        }
        else {
            return Provisioning.getInstance().getConfig().isReverseProxySSLToUpstreamEnabled();
        }
    }
}
