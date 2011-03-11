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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.HostName;
import com.zimbra.soap.admin.type.StatsSpec;
import com.zimbra.soap.admin.type.TimeAttr;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_GET_LOGGER_STATS_REQUEST)
public class GetLoggerStatsRequest {

    @XmlElement(name=AdminConstants.E_HOSTNAME, required=false)
    private final HostName hostName;

    @XmlElement(name=AdminConstants.E_STATS, required=false)
    private final StatsSpec stats;

    @XmlElement(name=AdminConstants.E_START_TIME, required=false)
    private final TimeAttr startTime;

    @XmlElement(name=AdminConstants.E_END_TIME, required=false)
    private final TimeAttr endTime;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetLoggerStatsRequest() {
        this((HostName) null, (StatsSpec) null,
                (TimeAttr) null, (TimeAttr) null);
    }

    public GetLoggerStatsRequest(HostName hostName, StatsSpec stats,
                    TimeAttr startTime, TimeAttr endTime) {
        this.hostName = hostName;
        this.stats = stats;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public HostName getHostName() { return hostName; }
    public StatsSpec getStats() { return stats; }
    public TimeAttr getStartTime() { return startTime; }
    public TimeAttr getEndTime() { return endTime; }
}
