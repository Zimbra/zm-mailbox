/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;

public class CheckPortConflict extends AttributeCallback {

    private static final Set<String> ProxyPortAttrs = new HashSet<String>();

    static {
        // TODO: use a flag in zimbra-attrs.xml and generate this map automatically
        ProxyPortAttrs.add(Provisioning.A_zimbraAdminProxyPort);

        ProxyPortAttrs.add(Provisioning.A_zimbraImapProxyBindPort);
        ProxyPortAttrs.add(Provisioning.A_zimbraImapSSLProxyBindPort);

        ProxyPortAttrs.add(Provisioning.A_zimbraPop3ProxyBindPort);
        ProxyPortAttrs.add(Provisioning.A_zimbraPop3SSLProxyBindPort);

        ProxyPortAttrs.add(Provisioning.A_zimbraMailProxyPort);
        ProxyPortAttrs.add(Provisioning.A_zimbraMailSSLProxyPort);
        ProxyPortAttrs.add(Provisioning.A_zimbraMailSSLProxyClientCertPort);
    }

    private static final Set<String> NonProxyPortAttrs = new HashSet<String>();

    static {
        // TODO: use a flag in zimbra-attrs.xml and generate this map automatically
        NonProxyPortAttrs.add(Provisioning.A_zimbraAdminPort);

        NonProxyPortAttrs.add(Provisioning.A_zimbraImapBindPort);
        NonProxyPortAttrs.add(Provisioning.A_zimbraImapSSLBindPort);

        NonProxyPortAttrs.add(Provisioning.A_zimbraPop3BindPort);
        NonProxyPortAttrs.add(Provisioning.A_zimbraPop3SSLBindPort);

        NonProxyPortAttrs.add(Provisioning.A_zimbraMailPort);
        NonProxyPortAttrs.add(Provisioning.A_zimbraMailSSLPort);
        NonProxyPortAttrs.add(Provisioning.A_zimbraMailSSLClientCertPort);

        NonProxyPortAttrs.add(Provisioning.A_zimbraLmtpBindPort);
        NonProxyPortAttrs.add(Provisioning.A_zimbraSmtpPort);
        NonProxyPortAttrs.add(Provisioning.A_zimbraRemoteManagementPort);
        NonProxyPortAttrs.add(Provisioning.A_zimbraMemcachedBindPort);
        NonProxyPortAttrs.add(Provisioning.A_zimbraMessageChannelPort);
        NonProxyPortAttrs.add(Provisioning.A_zimbraExtensionBindPort);
        NonProxyPortAttrs.add(Provisioning.A_zimbraMtaAuthPort);
    }

    private static Set<String> sPortAttrs = new HashSet<String>(NonProxyPortAttrs);

    @Override
    public void preModify(CallbackContext context, String attrName, Object attrValue,
            Map attrsToModify, Entry entry)
    throws ServiceException {

        if (entry != null && !(entry instanceof Server) && !(entry instanceof Config)) return;

        if (context.isDoneAndSetIfNot(CheckPortConflict.class)) {
            return;
        }

        // Have sPortAttrs as a union of NonProxyPortAttrs & ProxyPortAttrs
        sPortAttrs.addAll(ProxyPortAttrs);

        // sanity check, zimbra-attrs.xml and the sPortAttrsToCheck map has to be in sync
        if (!sPortAttrs.contains(attrName) ||
            !AttributeManager.getInstance().isServerInherited(attrName))
            assert(false);

        // server == null means the server entry is being created
        if (entry == null || entry instanceof Server)
            checkServer((Server)entry, attrsToModify);
        else
            checkConfig((Config)entry, attrsToModify);
    }

    private void checkServer(Server server, Map<String, Object> serverAttrsToModify) throws ServiceException {
        Map<String, String> ports = new HashMap<String, String>();
        Map<String, Object> defaults = Provisioning.getInstance().getConfig().getServerDefaults();

        // collect current port values
        if (server != null) {
            for (String attrName : sPortAttrs) {
                if (!serverAttrsToModify.containsKey(attrName))
                    ports.put(server.getAttr(attrName), attrName);
            }
        }

        // check conflict for attrs being changed
        for (Map.Entry<String, Object> attrToModify : serverAttrsToModify.entrySet()) {
            String attrName = attrToModify.getKey();

            if (!sPortAttrs.contains(attrName))
                continue;

            SingleValueMod mod = singleValueMod(serverAttrsToModify, attrName);
            String newValue = null;
            if (mod.setting())
                newValue = mod.value();
            else {
                // unsetting, get default, which would become the new value
                Object defValue = defaults.get(attrName);
                if (defValue != null) {
                    if (defValue instanceof String)
                        newValue = (String)defValue;
                    else
                        ZimbraLog.misc.info("default value for " + attrName + " should be a single-valued attribute, invalid default value ignored");
                }
            }

            if (conflict(server, ports, newValue, attrName)) {
                String serverInfo = (server == null) ? "" : " on server " + server.getName();
                throw ServiceException.INVALID_REQUEST("port " + newValue + " conflict between " +
                                                       attrName + " and " + ports.get(newValue) + serverInfo, null);
            } else
                ports.put(newValue, attrName);
        }
    }

    private void checkConfig(Config config, Map<String, Object> configAttrsToModify) throws ServiceException {
        BiMap<String, String> newDefaults = HashBiMap.create();

        /*
         * First, make sure there is no conflict in the Config entry, even
         * if the value on the config entry might not be effective on a server.
         */
        for (String attrName : sPortAttrs) {
            if (!configAttrsToModify.containsKey(attrName))
                newDefaults.put(config.getAttr(attrName), attrName);
        }

        // check conflict for attrs being changed
        for (Map.Entry<String, Object> attrToModify : configAttrsToModify.entrySet()) {
            String attrName = attrToModify.getKey();

            if (!sPortAttrs.contains(attrName))
                continue;

            SingleValueMod mod = singleValueMod(configAttrsToModify, attrName);
            String newValue = null;
            if (mod.setting())
                newValue = mod.value();

            if (conflict(null, newDefaults, newValue, attrName)) {
                throw ServiceException.INVALID_REQUEST("port " + newValue + " conflict between " +
                        attrName + " and " + newDefaults.get(newValue) + " on global config", null);
            } else
                newDefaults.put(newValue, attrName);
        }

        /*
         * Then, iterate through all servers see if this port change on the Config
         * entry has impact on a server.
         */
        List<Server> servers = Provisioning.getInstance().getAllServers();
        for (Server server : servers) {
            checkServerWithNewDefaults(server, newDefaults, configAttrsToModify);
        }
    }

    private void checkServerWithNewDefaults(Server server, BiMap<String, String> newDefaults,
            Map<String, Object> configAttrsToModify)
    throws ServiceException {
        Map<String, String> ports = new HashMap<String, String>();

        for (String attrName : sPortAttrs) {
            String newValue = null;
            String curValue = server.getAttr(attrName, false); // value on the server entry
            if (curValue == null)
                newValue = newDefaults.inverse().get(attrName);  // will inherit from new default
            else
                newValue = curValue;

            if (conflict(server, ports, newValue, attrName)) {
                String conflictWith = ports.get(newValue);
                // throw only when the attr is one of the attrs being modified, otherwise, just let it pass.
                if (configAttrsToModify.containsKey(attrName) || configAttrsToModify.containsKey(conflictWith))
                    throw ServiceException.INVALID_REQUEST("port " + newValue + " conflict between " +
                        attrName + " and " + ports.get(newValue) + " on server " + server.getName(), null);
            } else
                ports.put(newValue, attrName);
        }
    }

    private boolean conflict(Server server, Map<String, String> ports, String port, String attrName) {
        if (StringUtil.isNullOrEmpty(port))
            return false;
        else if (port.equals("0"))
            return false;
        else if (server != null) {
            if (!server.hasMailboxService() || !server.hasProxyService()) {
                if ((ProxyPortAttrs.contains(attrName) && NonProxyPortAttrs.contains(ports.get(port))) ||
                    (NonProxyPortAttrs.contains(attrName) && ProxyPortAttrs.contains(ports.get(port))))
                    return false;
            }
        }

        return ports.containsKey(port);
    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
    }
}
