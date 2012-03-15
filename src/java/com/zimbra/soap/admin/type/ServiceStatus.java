/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

package com.zimbra.soap.admin.type;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.util.CsvReader;
import com.zimbra.soap.type.ZeroOrOne;

/**
 * Used by {@CountAccountResponse}
 */
@XmlAccessorType(XmlAccessType.NONE)
public class ServiceStatus {

    /**
     * @zm-api-field-tag server
     * @zm-api-field-description Server
     */
    @XmlAttribute(name=AdminConstants.A_SERVER /* server */, required=true)
    private final String server;

    /**
     * @zm-api-field-tag service
     * @zm-api-field-description Service
     */
    @XmlAttribute(name=AdminConstants.A_SERVICE /* service */, required=true)
    private final String service;

    // seconds since epoch
    /**
     * @zm-api-field-tag date-time
     * @zm-api-field-description Number of seconds since the epoch (1970), UTC time
     */
    @XmlAttribute(name=AdminConstants.A_T /* t */, required=true)
    private final long time;

    // "1" or "0"
    /**
     * @zm-api-field-tag status
     * @zm-api-field-description Status
     */
    @XmlValue
    private final ZeroOrOne status;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ServiceStatus() {
        this(null, null, -1, null);
    }

    private ServiceStatus(String server, String service, long time, ZeroOrOne status) {
        this.server = server;
        this.service = service;
        this.time = time;
        this.status = status;
    }

    public static ServiceStatus fromServerServiceTimeStatus(
                String server, String service, long time, ZeroOrOne status) {
        return new ServiceStatus(server, service, time, status);
    }

    public String getServer() { return server; }
    public String getService() { return service; }
    public long getTime() { return time; }
    public ZeroOrOne getStatus() { return status; }

    @Override
    public int hashCode() {
        return server.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        boolean eq = false;
        if (other instanceof ServiceStatus) {
            ServiceStatus o = (ServiceStatus) other;
            eq = server.equals(o.server) && service.equals(o.service);
        }
        return eq;
    }

    public static List<ServiceStatus> parseData(Map<String,CsvReader> data)
    throws IOException {
        List<ServiceStatus> results = Lists.newArrayList();
        for (String host : data.keySet()) {
            CsvReader r = data.get(host);
            List<String> columns = new ArrayList<String>(
                    Arrays.asList(r.getColNames()));
            columns.remove("timestamp");
            Map<String,String> row = Maps.newHashMap();
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
                long timeStamp = Long.parseLong(lastTS);
                for (String service : row.keySet()) {
                    String status = row.get(service);
                    if (status != null) {
                        status = status.trim();
                        // for some reason, QA is getting an empty string
                        if ("".equals(status)) continue;
                        ServiceStatus s = fromServerServiceTimeStatus(host,
                                service, timeStamp,
                                // 0.8 check because of rrd rounding,
                                // happens when a service has just started
                                ZeroOrOne.fromBool(Float.parseFloat(status) > 0.8));
                        results.add(s);
                    }
                }
            }
        }
        return results;
    }
}
