/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * <GetLoggerStatsRequest>
 *   <!-- when hostname and stats are specified, fetch stats -->
 *   <hostname hn="hostname"/> <!-- optional, will list hosts otherwise -->
 *   <stats name="stat_group"/><!-- optional, will list stat groups if host specified and stats unspecified,
 *                                 will fetch given group for all hosts if hostname not specified -->
 *   <stats name="stat_group">1</stats><!-- optional, used in conjunction with hostname, list counters for
 *                                          the specified hostname and stat group -->
 *   <startTime time="ts"/><!-- optional, defaults to last day, both must be specified otherwise -->
 *   <endTime time="ts"/><!-- these are invalid if hostname and stats are not specified -->
 * </GetLoggerStatsRequest>
 * 
 * <GetLoggerStatsResponse>
 *   <hostname hn="hn"/>
 *   <hostname hn="..."/> <!-- list hosts case -->
 *   
 *   <hostname hn="hn"> <!-- list stat groups case -->
 *     <stats name="group1"/>
 *     <stats name="group..."/>
 *   </hostname>
 *   
 *   <hostname hn="hn"> <!-- list columns case -->
 *     <stats name="group...">
 *       <values>
 *         <stat name="col1"/>
 *         <stat name="col2"/>
 *         <stat name="col..."/>
 *       </values>
 *     </stats>
 *   </hostname>
 *   
 *   <hostname hn="hn"> <!-- stats case -->
 *     <stats name="group...">
 *       <values t="ts">
 *         <stat name="col1" value="X"/>
 *         <stat name="col2" value="Y"/>
 *         <stat name="col3" value="Z"/>
 *       </values>
 *       <values t="ts + N">
 *         <stat name="col1" value="X"/>
 *         <stat name="col2" value="Y"/>
 *         <stat name="col3" value="Z"/>
 *       </values>
 *     </stats>
 *   </hostname>
 *   
 * </GetLoggerStatsResponse>
 * @author pfnguyen
 */
public class GetLoggerStats extends AdminDocumentHandler {
    
    private final static String ZMRRDFETCH = LC.zimbra_home.value() + "/libexec/zmrrdfetch";
    private static final Pattern SPLIT_PATTERN = Pattern.compile("\\\"?\\s*,\\s*\\\"?");

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        
        checkRight(zsc, context, null, AdminRight.PR_SYSTEM_ADMIN_ONLY);
        
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
        
        Element response = zsc.createElement(AdminConstants.GET_LOGGER_STATS_RESPONSE);
        boolean loggerEnabled = false;
        Server local = prov.getLocalServer();
        String[] services = local.getMultiAttr(Provisioning.A_zimbraServiceEnabled);
        if (services != null) {
            for (int i = 0; i < services.length && !loggerEnabled; i++) {
                loggerEnabled = "logger".equals(services[i]);
            }
        }
        if (loggerEnabled) {
            Element host = request.getOptionalElement(AdminConstants.E_HOSTNAME);
            Element stats = request.getOptionalElement(AdminConstants.E_STATS);
            Element start = request.getOptionalElement(AdminConstants.E_START_TIME);
            Element end = request.getOptionalElement(AdminConstants.E_END_TIME);
            
            if (host == null && stats == null) {
                // list hosts
                fetchHostnames(response);
                
                if (start != null || end != null)
                    throw ServiceException.FAILURE("Cannot specify start/end without stats", null);
                
            } else if (host != null && stats == null) {
                // list groups for host
                fetchGroupNames(response, host.getAttribute(AdminConstants.A_HOSTNAME));
                
                if (start != null || end != null)
                    throw ServiceException.FAILURE("Cannot specify start/end without stats", null);
            } else if (stats != null && host == null) {
                // fetch stats for all hosts
                // TODO implement fetch for all hosts, not really necessary in current logger
                throw ServiceException.FAILURE("Unsupported Operation, listings all stats for all hosts", null);
            } else if (stats != null && host != null) {
                
                String statsText = stats.getText();
                if (statsText != null && statsText.trim().length() > 0) {
                    // list counters
                    fetchColumnNames(response,
                            host.getAttribute(AdminConstants.A_HOSTNAME),
                            stats.getAttribute(AdminConstants.A_NAME));
                } else {
                    // fetch stats for host
                    String startTime = null;
                    String endTime   = null;

                    if (start != null || end != null) {
                        if (start == null || end == null)
                            throw ServiceException.FAILURE("both start and end must be specified", null);

                        startTime = start.getAttribute(AdminConstants.A_TIME);
                        endTime   = end.getAttribute(AdminConstants.A_TIME);
                        fetchColumnData(response,
                                host.getAttribute(AdminConstants.A_HOSTNAME),
                                stats.getAttribute(AdminConstants.A_NAME),
                                startTime, endTime);
                    } else {
                        fetchColumnData(response,
                                host.getAttribute(AdminConstants.A_HOSTNAME),
                                stats.getAttribute(AdminConstants.A_NAME));
                    }
                }
            } else {
                throw ServiceException.FAILURE("Unknown query combination", null);
            }
        } else {
            response.addElement(AdminConstants.E_NOTE).setText("Logger is not enabled");
        }
        
        return response;
    }
    
    static void fetchHostnames(Element response) throws ServiceException {
        Iterator<String> results = execfetch("-n");
        
        while (results.hasNext()) {
            Element host = response.addElement(AdminConstants.E_HOSTNAME);
            host.addAttribute(AdminConstants.A_HOSTNAME, results.next());
        }
    }
    static void fetchGroupNames(Element response, String hostname) throws ServiceException {
        Iterator<String> results = execfetch("-l", "-h", hostname);
        Element host = response.addElement(AdminConstants.E_HOSTNAME);
        host.addAttribute(AdminConstants.A_HOSTNAME, hostname);
        while (results.hasNext()) {
            Element stats = host.addElement(AdminConstants.E_STATS);
            stats.addAttribute(AdminConstants.A_NAME, results.next());
        }
    }
    static void fetchColumnNames(Element response, String hostname, String group) throws ServiceException {
        Iterator<String> results = execfetch("-l", "-h", hostname, "-f", group);
        Element host = response.addElement(AdminConstants.E_HOSTNAME);
        host.addAttribute(AdminConstants.A_HOSTNAME, hostname);
        Element stats = host.addElement(AdminConstants.E_STATS);
        stats.addAttribute(AdminConstants.A_NAME, group);
        Element values = stats.addElement(AdminConstants.E_VALUES);
        while (results.hasNext()) {
            Element stat = values.addElement(AdminConstants.E_STAT);
            stat.addAttribute(AdminConstants.A_NAME, results.next());
        }
    }
    static void fetchColumnData(Element response, String hostname, String group,
            String start, String end)
    throws ServiceException {
        Iterator<String> results = execfetch("-h", hostname, "-f", group,
                "-s", start, "-e", end);
        populateResponseData(response, hostname, group, results);
    }
    static void fetchColumnData(Element response, String hostname, String group)
    throws ServiceException {
        Iterator<String> results = execfetch("-h", hostname, "-f", group);
        populateResponseData(response, hostname, group, results);
    }
    
    static void populateResponseData(Element response,
            String hostname, String group, Iterator<String> results)
    throws ServiceException {
        if (!results.hasNext())
            throw ServiceException.FAILURE("Stats fetched do not contain column names", null);
        
        String line = results.next();
        String[] columns = SPLIT_PATTERN.split(line);
        
        Element host = response.addElement(AdminConstants.E_HOSTNAME);
        host.addAttribute(AdminConstants.A_HOSTNAME, hostname);
        Element stats = host.addElement(AdminConstants.E_STATS);
        stats.addAttribute(AdminConstants.A_NAME, group);
        while (results.hasNext()) {
            line = results.next();
            String[] data = SPLIT_PATTERN.split(line);
            
            boolean rowHasData = false;
            for (int i = 1, j = data.length; i < j; i++)
                rowHasData = rowHasData || (data[i] != null && !data[i].trim().equals(""));
            if (rowHasData) {
                Element values = stats.addElement(AdminConstants.E_VALUES);
                values.addAttribute(AdminConstants.A_T, data[0]);
                for (int i = 1, j = data.length; i < j; i++) {
                    Element stat = values.addElement(AdminConstants.E_STAT);
                    stat.addAttribute(AdminConstants.A_NAME, columns[i]);
                    if (data[i] != null)
                        stat.addAttribute(AdminConstants.A_VALUE, data[i]);
                }
            }
        }
        
    }

    static Iterator<String> execfetch(String... args) throws ServiceException {
        BufferedReader in = null;
        try {
            ArrayList<String> cmdline = new ArrayList<String>();
            cmdline.add(ZMRRDFETCH);
            cmdline.addAll(Arrays.asList(args));
            ProcessBuilder pb = new ProcessBuilder(cmdline);
            final Process p = pb.start();
            in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            final BufferedReader inbr = in;
            
            // there is a stream leak here if not read until the end
            return new Iterator<String>() {
                String line;
                boolean isClosed = false;

                public boolean hasNext() {
                    try {
                        line = inbr.readLine();
                    }
                    catch (IOException e) {
                        ZimbraLog.soap.error("GetLoggerStats IOE", e);
                        line = null;
                    }
                    
                    if (line == null) {
                        try {
                            inbr.close();
                            isClosed = true;
                        }
                        catch (IOException e) { } // ignore
                    }
                    return line != null;
                }

                public String next() {
                    if (!isClosed && line == null) {
                        try {
                            inbr.close();
                        }
                        catch (IOException e) { } // ignore
                        throw new IllegalStateException("hasNext not called");
                    }
                    if (isClosed)
                        throw new IllegalStateException("no more results");
                    String l = line;
                    line = null;
                    return l;
                }

                public void remove() { throw new UnsupportedOperationException("remove"); }
                
            };
        }
        catch (IOException e) {
            throw ServiceException.FAILURE("Unable to read logger stats", e);
        }
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(AdminRightCheckPoint.Notes.SYSTEM_ADMINS_ONLY);
    }
}
