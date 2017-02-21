/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.HostName;
import com.zimbra.soap.admin.type.StatsSpec;
import com.zimbra.soap.admin.type.TimeAttr;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Query to retrieve Logger statistics in ZCS
 * <br />
 * Use cases:
 * <ul>
 * <li> No elements specified
 *      <br />
 *      result: a listing of reporting host names
 * <li> hostname specified
 *      <br />
 *      result: a listing of stat groups for the specified host
 * <li> hostname and stats specified, text content of stats non-empty
 *      <br />
 *      result: a listing of columns for the given host and group
 * <li> hostname and stats specified, text content empty, startTime/endTime optional
 *      <br />
 *      result: all of the statistics for the given host/group are returned, if start and end are specified,
 *      limit/expand the timerange to the given setting.  if limit=true is specified, attempt to reduce result set
 *      to under 500 records
 * </ul>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_LOGGER_STATS_REQUEST)
public class GetLoggerStatsRequest {

    /**
     * @zm-api-field-description Hostname
     */
    @XmlElement(name=AdminConstants.E_HOSTNAME, required=false)
    private final HostName hostName;

    /**
     * @zm-api-field-description Stats
     */
    @XmlElement(name=AdminConstants.E_STATS, required=false)
    private final StatsSpec stats;

    /**
     * @zm-api-field-description Start time
     */
    @XmlElement(name=AdminConstants.E_START_TIME, required=false)
    private final TimeAttr startTime;

    /**
     * @zm-api-field-description End time
     */
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
