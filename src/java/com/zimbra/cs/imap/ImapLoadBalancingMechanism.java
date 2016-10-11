/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
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

package com.zimbra.cs.imap;

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.QuotedStringParser;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;

public abstract class ImapLoadBalancingMechanism {

    public static enum ImapLBMech {

        /**
         * zimbraImapLoadBalancingAlgorithm type of "ClientIpHash" will select an IMAP
         * server based on the hash of the client IP address.
         * TODO - should these be refactored to use all lower-case?
         */
        ClientIpHash,

        /**
         * zimbraImapLoadBalancingAlgorithm type of "custom:{handler}" means use registered extension
         * TODO: details and documentation
         */
        custom;

        public static ImapLBMech fromString(String lbMechStr) throws ServiceException {
            if (lbMechStr == null) {
                return null;
            }

            try {
                return ImapLBMech.valueOf(lbMechStr);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown IMAP load balancing mech: " + lbMechStr, e);
            }
        }
    }

    protected ImapLBMech lbMech;

    protected ImapLoadBalancingMechanism(ImapLBMech lbMech) {
        this.lbMech = lbMech;
    }
    
    public static ImapLoadBalancingMechanism newInstance()
    throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Config config = prov.getConfig();
        String lbMechStr = config.getAttr(
            Provisioning.A_zimbraImapLoadBalancingAlgorithm,
            ImapLBMech.ClientIpHash.name()
        );
        return newInstance(lbMechStr);
    }

    public static ImapLoadBalancingMechanism newInstance(String lbMechStr)
    throws ServiceException {
        if (lbMechStr.startsWith(ImapLBMech.custom.name() + ":")) {
            return loadCustomLBMech(lbMechStr);
        } else {
            try {
                ImapLBMech lbMech = ImapLBMech.fromString(lbMechStr);

                switch (lbMech) {
                    case ClientIpHash:
                    default:
                        return new ClientIpHashMechanism(lbMech);
                }
            } catch (ServiceException e) {
                ZimbraLog.account.warn("invalid load balancing mechanism", e);
            }

            ZimbraLog.imap.warn("unknown value for " + Provisioning.A_zimbraImapLoadBalancingAlgorithm + ": " +
                lbMechStr + ", falling back to default mech");
            return new ClientIpHashMechanism(ImapLBMech.ClientIpHash);
        }

    }

    @VisibleForTesting
    protected static ImapLoadBalancingMechanism loadCustomLBMech(String lbMechStr) throws ServiceException {
        String customMechName = null;
        List<String> args = null;
        int mechNameStart = lbMechStr.indexOf(':');
        if (mechNameStart != -1) {
            int mechNameEnd = lbMechStr.indexOf(' ');
            if (mechNameEnd != -1) {
                customMechName = lbMechStr.substring(mechNameStart+1, mechNameEnd);
                QuotedStringParser parser = new QuotedStringParser(lbMechStr.substring(mechNameEnd+1));
                args = parser.parse();
                if (args.size() == 0) {
                    args = null;
                }
            } else {
                customMechName = lbMechStr.substring(mechNameStart+1);
            }
        }
        if (!StringUtil.isNullOrEmpty(customMechName)) {
            CustomLBMech mech = CustomLBMech.getCustomMech(customMechName, args);
            if (mech != null) {
                return mech;
            } else {
                return new ClientIpHashMechanism(ImapLBMech.ClientIpHash);
            }
        } else {
            ZimbraLog.imap.warn("invalid custom load balancing mechanism: %s, falling back to default mech", lbMechStr);
            return new ClientIpHashMechanism(ImapLBMech.ClientIpHash);
        }
    }

    public abstract Server getImapServerFromPool(HttpServletRequest httpReq, List<Server> pool)
    throws ServiceException;


    /*
     * ClientIpHash load balancing mechanism
     */
    public static class ClientIpHashMechanism extends ImapLoadBalancingMechanism {
        public static final String CLIENT_IP = "Client-IP";
        private Comparator<Server> serverComparator = new Comparator<Server>() {
            @Override
            public int compare (Server a, Server b) {
                return a.getName().compareTo(b.getName());
            }
        };
        ClientIpHashMechanism(ImapLBMech lbMech) {
            super(lbMech);
        }

        @Override
        public Server getImapServerFromPool(HttpServletRequest httpReq, List<Server> pool)
        throws ServiceException {
            if (pool.size() == 0) {
                throw ServiceException.INVALID_REQUEST("Empty IMAP server pool", null);
            }
            try {
                pool.sort(serverComparator);
                int clientIpHash = InetAddress.getByName(httpReq.getHeader(CLIENT_IP)).hashCode();
                int serverPoolIdx = clientIpHash % pool.size();
                ZimbraLog.imap.debug(
                    "ClientIpHashMechanism.getImapServerFromPool: CLIENT_IP=%s, Server.pool.size=%d, clientIpHash=%d, serverPoolIdx=%d",
                    httpReq.getHeader(CLIENT_IP), pool.size(), clientIpHash, serverPoolIdx
                );
                return pool.get(serverPoolIdx);
            }
            catch (UnknownHostException e) {
                ZimbraLog.imap.warn(
                    "Error resolving CLIENT_IP '" +
                    httpReq.getHeader(CLIENT_IP) +
                    "' - returning random IMAP server from pool"
                );
                return pool.get((int)(Math.random() * pool.size()));
            }
        }
    }

    /**
     * Base class for custom load balancing mechanisms.
     * Implementations of CustomLBMech need to register themselves using the CustomLBMEch.register() method
     * in order for the "custom:{LB mech} [args ...]" value of zimbraImapLoadBalancingAlgorithm to be
     * recognized. Argument lists
     */
    public static abstract class CustomLBMech extends ImapLoadBalancingMechanism {
        protected List<String> args;

        protected CustomLBMech() {
            this(null);
        }

        protected CustomLBMech(List<String> args) {
            super(ImapLBMech.custom);
            this.args = args;
        }

        private static Map<String, Class<? extends CustomLBMech>> customLBMechs;

        /**
         * Implementations of CustomLBMech need to register themselves using this method in order
         * for the "custom:{LB mech} [args ...]" value of zimbraImapLoadBalancingAlgorithm to be
         * recognized.
         *
         * @param customMechName
         * @param customMech
         */
        public static void register(String customMechName, Class<? extends CustomLBMech> customMech) {
            if (customLBMechs == null) {
                customLBMechs = new HashMap<String, Class<? extends CustomLBMech>>();
            } else if (customLBMechs.get(customMechName) != null) {
                ZimbraLog.imap.warn("load-balancing mechanism " + customMechName + " is already registered");
                return;
            }
            customLBMechs.put(customMechName, customMech);
        }


        public synchronized static CustomLBMech getCustomMech(String customMechName, List<String> args) {
            if (customLBMechs == null || customLBMechs.get(customMechName) == null) {
                ZimbraLog.imap.warn("no CustomLBMech class registered for key %s; "
                        + "falling back to default", customMechName);
                return null;
            } else {
                Class<? extends CustomLBMech> klass = customLBMechs.get(customMechName);
                try {
                    return klass.getDeclaredConstructor(List.class).newInstance(args);
                } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                    ZimbraLog.imap.warn("cannot instantiate custom load-balancing mechanism %s; "
                            + "falling back to default", klass.getName(), e);
                    return null;
                }
            }
        }
    }
}

