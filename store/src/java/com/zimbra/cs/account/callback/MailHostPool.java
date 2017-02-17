/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.List;
import java.util.Map;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;

public class MailHostPool extends AttributeCallback {

    /**
     * check to make sure zimbraMailHostPool points to a valid server id
     */
    @Override
    public void preModify(CallbackContext context, String attrName, Object value,
            Map attrsToModify, Entry entry)
    throws ServiceException {

        MultiValueMod mod = multiValueMod(attrsToModify, Provisioning.A_zimbraMailHostPool);

        if (mod.adding() || mod.replacing()) {
            Provisioning prov = Provisioning.getInstance();
            List<String> pool = mod.values();
            for (String host : pool) {
                if (host == null || host.equals("")) continue;
                Server server = prov.get(Key.ServerBy.id, host);
                if (server == null)
                    throw ServiceException.INVALID_REQUEST(
                            "specified " + Provisioning.A_zimbraMailHostPool +
                            " does not correspond to a valid server: "+host, null);
                else {
                    if (!server.hasMailClientService()) {
                        throw ServiceException.INVALID_REQUEST("specified " + Provisioning.A_zimbraMailHost +
                                " is not a mailclient server with service webapp enabled: "
                                +host, null);
                    }
                }
            }
        }
    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
    }
}
