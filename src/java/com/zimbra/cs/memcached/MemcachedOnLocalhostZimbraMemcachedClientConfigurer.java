/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.memcached;

import javax.annotation.PostConstruct;

import net.spy.memcached.DefaultHashAlgorithm;

import org.springframework.beans.factory.annotation.Autowired;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.memcached.ZimbraMemcachedClient;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;

public class MemcachedOnLocalhostZimbraMemcachedClientConfigurer extends ZimbraMemcachedClientConfigurer {
    @Autowired protected ZimbraMemcachedClient client;

    /**
     * Reload the memcached client configuration.  Connect to the servers if configured with a
     * non-empty server list.  Any old connections are flushed and disconnected.
     * @throws ServiceException
     */
    @PostConstruct
    public void reconfigure() throws ServiceException {
        Server server = Provisioning.getInstance().getLocalServer();
        String[] serverList = {"localhost"};
        boolean useBinaryProtocol = server.getBooleanAttr(Provisioning.A_zimbraMemcachedClientBinaryProtocolEnabled, false);
        String hashAlgorithm = server.getAttr(Provisioning.A_zimbraMemcachedClientHashAlgorithm, DefaultHashAlgorithm.KETAMA_HASH.toString());
        int expirySeconds = 10;
        long timeoutMillis = 100;
        client.connect(serverList, useBinaryProtocol, hashAlgorithm, expirySeconds, timeoutMillis);
    }
}
