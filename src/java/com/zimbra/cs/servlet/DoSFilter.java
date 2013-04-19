/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013 VMware, Inc.
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

package com.zimbra.cs.servlet;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;


public class DoSFilter extends org.eclipse.jetty.servlets.DoSFilter {

    @Override
    public void init(FilterConfig filterConfig) {
        LogFactory.init();
        super.init(filterConfig);
        try {
            List<Server> servers = Provisioning.getInstance().getAllServers(Provisioning.SERVICE_MAILBOX);
            for (Server server : servers) {
                try {
                    InetAddress[] addresses = InetAddress.getAllByName(server.getServiceHostname());
                    for (InetAddress address : addresses) {
                        addWhitelistAddress(address.getHostAddress());
                    }
                } catch (UnknownHostException e) {
                    ZimbraLog.misc.warn("Invalid hostname: " + server.getServiceHostname(), e);
                }
            }
            String[] ips = Provisioning.getInstance().getLocalServer().getHttpThrottleSafeIPs();
            for (String ip : ips) {
                addWhitelistAddress(ip);
            }
        } catch (ServiceException e) {
            ZimbraLog.misc.warn("Unable to get throttle safe IPs", e);
        }
        // add loopback addresses
        addWhitelistAddress("127.0.0.1");
        addWhitelistAddress("::1");
        ZimbraLog.misc.info("DoSFilter: Configured whitelist IPs = " + getWhitelist());
    }

    @Override
    protected String extractUserId(ServletRequest request) {
        return ZimbraQoSFilter.extractUserId(request);
    }
}
