/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.util.CsvReader;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class GetServiceStatus extends AdminDocumentHandler {
    private final static String ZMRRDFETCH = LC.zimbra_home.value() + "/libexec/zmrrdfetch";
    private final static String ZMSTATUSLOG_CSV = "zmstatuslog";

    public Element handle(Element request, Map<String, Object> context) throws SoapFaultException, ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        // this command can only execute on the monitor host, so proxy if necessary
        Provisioning prov = Provisioning.getInstance();
        String monitorHost = prov.getConfig().getAttr(Provisioning.A_zimbraLogHostname);
        if (monitorHost == null || monitorHost.trim().equals(""))
            throw ServiceException.FAILURE("zimbraLogHostname is not configured", null);
        Server monitorServer = prov.get(ServerBy.name, monitorHost);
        if (monitorServer == null)
            throw ServiceException.FAILURE("could not find zimbraLogHostname server: " + monitorServer, null);
        if (!prov.getLocalServer().getId().equalsIgnoreCase(monitorServer.getId()))
            return proxyRequest(request, context, monitorServer);

        Element response = zsc.createElement(AdminConstants.GET_SERVICE_STATUS_RESPONSE);
        
        TimeZone tz = TimeZone.getDefault();
        Element eTimeZone = response.addElement(AdminConstants.E_TIMEZONE);
        eTimeZone.addAttribute(AdminConstants.A_TIMEZONE_ID, tz.getID());
        eTimeZone.addAttribute(AdminConstants.A_TIMEZONE_DISPLAYNAME, tz.getDisplayName());
        
        boolean loggerEnabled = false;
        Server local = prov.getLocalServer();
        String[] services = local.getMultiAttr(Provisioning.A_zimbraServiceEnabled);
        if (services != null) {
            for (int i = 0; i < services.length && !loggerEnabled; i++) {
                loggerEnabled = "logger".equals(services[i]);
            }
        }
        if (loggerEnabled) {
            HashSet<ServiceStatus> serviceStatus = new HashSet<ServiceStatus>();
            List<Server> servers = prov.getAllServers();
            for (Server s : servers) {
                String[] srvs = s.getMultiAttr(Provisioning.A_zimbraServiceEnabled);
                for (String service : srvs) {
                    ServiceStatus st = new ServiceStatus();
                    st.server = s.getName();
                    st.service = service;
                    st.time = Long.toString(System.currentTimeMillis() / 1000);
                    st.status = "0";
                    serviceStatus.add(st);
                }
                
            }
            BufferedReader in = null;
            try {
                ProcessBuilder pb = new ProcessBuilder(ZMRRDFETCH, "-f", ZMSTATUSLOG_CSV);
                Process p = pb.start();
                in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                Map<String,CsvReader> hostStatus = new HashMap<String,CsvReader>();
                StringWriter currentWriter = null;
                String currentHost = null;
                String line;
                while ((line = in.readLine()) != null) {
                    if ("".equals(line.trim())) continue;
                    if (line.startsWith("Host: ")) {
                        if (currentHost != null)
                            hostStatus.put(currentHost, new CsvReader(
                                    new StringReader(currentWriter.toString())));
                        currentHost   = line.substring("Host: ".length());
                        currentWriter = new StringWriter();
                    } else {
                        if (currentWriter != null)
                            currentWriter.write(line + "\n");
                    }
                }
                if (currentHost != null && currentWriter != null)
                    hostStatus.put(currentHost, new CsvReader(
                            new StringReader(currentWriter.toString())));
                List<ServiceStatus> status = ServiceStatus.parseData(hostStatus);
                for (ServiceStatus stat : status) {
                    serviceStatus.remove(stat);
                    serviceStatus.add(stat);
                }
                for (ServiceStatus stat : serviceStatus) {
                    if (!checkRights(zsc, stat.server)) {
                        ZimbraLog.misc.info("skipping server " + stat.server + ", has not right to get service status");
                        continue;
                    }
                    
                    Element s = response.addElement(AdminConstants.E_STATUS);
                    s.addAttribute(AdminConstants.A_SERVER, stat.server);
                    s.addAttribute(AdminConstants.A_SERVICE, stat.service);
                    s.addAttribute(AdminConstants.A_T, stat.time);
                    s.setText(stat.status);
                }
            }
            catch (IOException e) {
                try {
                    if (in != null) in.close();
                }
                catch (IOException x) { }
                ServiceException.FAILURE("Unable to read logger stats", e);
            }
        }
        return response;
    }

    private static class ServiceStatus {
        String server;
        String service;
        String time;    // seconds since epoch
        String status;  // "1" or "0"
        
        @Override public int hashCode() {
            return server.hashCode();
        }
        
        @Override public boolean equals(Object other) {
            boolean eq = false;
            if (other instanceof ServiceStatus) {
                ServiceStatus o = (ServiceStatus) other;
                eq = server.equals(o.server) && service.equals(o.service);
            }
            return eq;
        }
        static List<ServiceStatus> parseData(Map<String,CsvReader> data)
        throws IOException {
            List<ServiceStatus> results = new ArrayList<ServiceStatus>();
            for (String host : data.keySet()) {
                CsvReader r = data.get(host);
                ArrayList<String> columns = new ArrayList<String>(
                        Arrays.asList(r.getColNames()));
                columns.remove("timestamp");
                Map<String,String> row = new HashMap<String,String>();
                String lastTS = null;
                
                while (r.hasNext()) {
                    String ts = r.getValue("timestamp");
                    boolean rowHasData = false;
                    for (String column : columns) {
                        String value = r.getValue(column);
                        rowHasData = rowHasData || value != null;
                    }
                    if (rowHasData) {
                        lastTS = ts;
                        row.clear();
                        for (String column : columns) {
                            String value = r.getValue(column);
                            if (value != null)
                                row.put(column, value);
                        }
                    }
                }
                if (lastTS != null) {
                    for (String service : row.keySet()) {
                        String status = row.get(service);
                        if (status != null) {
                            status = status.trim();
                            // for some reason, QA is getting an empty string
                            if ("".equals(status)) continue;
                            ServiceStatus s = new ServiceStatus();
                            s.server  = host;
                            s.time    = lastTS;
                            s.service = service;
                            // 0.8 check because of rrd rounding,
                            // happens when a service has just started
                            s.status = Float.parseFloat(status) > 0.8 ? "1" : "0";
                            results.add(s);
                        }
                    }
                }
            }
            return results;
        }
    }

    private boolean checkRights(ZimbraSoapContext zsc, String serverName) throws ServiceException {
        try {
            Server server = Provisioning.getInstance().get(ServerBy.name, serverName);
            checkRight(zsc, server, Admin.R_getServiceStatus);
        } catch (ServiceException e) {
            // if PERM_DENIED, return false and log, do not throw, so we 
            // can continue with the next entry
            if (ServiceException.PERM_DENIED.equals(e.getCode())) {
                return false;
            } else {
                // encountered a real error, rethrow
                throw e;
            }
        }
        
        return true;
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_getServiceStatus);
    }
}
