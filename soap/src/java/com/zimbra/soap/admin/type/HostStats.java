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

package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class HostStats {

    /**
     * @zm-api-field-tag hostname
     * @zm-api-field-description Hostname
     */
    @XmlAttribute(name=AdminConstants.A_HOSTNAME, required=true)
    private final String hostName;

    /**
     * @zm-api-field-description Stats information
     */
    @XmlElement(name=AdminConstants.E_STATS, required=false)
    private StatsInfo stats;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private HostStats() {
        this((String) null);
    }

    public HostStats(String hostName) {
        this.hostName = hostName;
    }

    public void setStats(StatsInfo stats) { this.stats = stats; }
    public String getHostName() { return hostName; }
    public StatsInfo getStats() { return stats; }
}
