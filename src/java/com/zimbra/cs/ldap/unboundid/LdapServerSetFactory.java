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
package com.zimbra.cs.ldap.unboundid;

import com.unboundid.ldap.sdk.FailoverServerSet;
import com.unboundid.ldap.sdk.FastestConnectServerSet;
import com.unboundid.ldap.sdk.FewestConnectionsServerSet;
import com.unboundid.ldap.sdk.RoundRobinServerSet;
import com.unboundid.ldap.sdk.ServerSet;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.ldap.LdapException;

/**
 * @author sankumar
 *
 */
public class LdapServerSetFactory {
    
    /***  Valid values : DNSSRVRecordServerSet, FailoverServerSet, FastestConnectServerSet, FewestConnectionsServerSet, RoundRobinDNSServerSet, RoundRobinServerSet
     DNSSRVRecordServerSet and RoundRobinDNSServerSet are not implemented
      reference: https://docs.ldap.com/ldap-sdk/docs/javadoc/com/unboundid/ldap/sdk/ServerSet.html
      **/
    
    public enum ServerSetType {
        FailoverServerSet, FastestConnectServerSet, FewestConnectionsServerSet, RoundRobinServerSet
    }

    public static ServerSet getServerSet(ServerSetConfig serverSetConfig)
            throws LdapException {
        ServerSetType serverSetType = getSeverSetType();
        switch (serverSetType) {
        case FailoverServerSet:
            return new FailoverServerSet(serverSetConfig.getHosts(),
                    serverSetConfig.getPorts(),
                    serverSetConfig.getSocketFactory(),
                    serverSetConfig.getLdapConnectionOptions());
        case FastestConnectServerSet:
            return new FastestConnectServerSet(serverSetConfig.getHosts(),
                    serverSetConfig.getPorts(),
                    serverSetConfig.getSocketFactory(),
                    serverSetConfig.getLdapConnectionOptions());
        case FewestConnectionsServerSet:
            return new FewestConnectionsServerSet(serverSetConfig.getHosts(),
                    serverSetConfig.getPorts(),
                    serverSetConfig.getSocketFactory(),
                    serverSetConfig.getLdapConnectionOptions());
        case RoundRobinServerSet:
            return new RoundRobinServerSet(serverSetConfig.getHosts(),
                    serverSetConfig.getPorts(),
                    serverSetConfig.getSocketFactory(),
                    serverSetConfig.getLdapConnectionOptions());
        default:
            ZimbraLog.ldap.error("Server set type %s not handled",
                    serverSetType);
            throw LdapException.INVALID_CONFIG("LC value for ldap_client_server_set_type is not valid. Server set type "
                    + serverSetType + "not handled", new Throwable());
        }
    }

    public static ServerSetType getSeverSetType() {
        String lcServerSetType = LC.ldap_client_server_set_type.value();
        ServerSetType serverSetType = ServerSetType.valueOf(lcServerSetType);
        return serverSetType;
    }
}
