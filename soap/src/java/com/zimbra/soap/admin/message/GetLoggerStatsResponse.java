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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.NamedElement;
import com.zimbra.soap.admin.type.HostStats;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_LOGGER_STATS_RESPONSE)
public class GetLoggerStatsResponse {

    /**
     * @zm-api-field-description Info by hostname
     */
    @XmlElement(name=AdminConstants.E_HOSTNAME, required=false)
    private List<HostStats> hostNames = Lists.newArrayList();

    /**
     * @zm-api-field-tag note
     * @zm-api-field-description Note.  For instance "Logger is not enabled"
     */
    @XmlElement(name=AdminConstants.E_NOTE, required=false)
    private String note;

    public GetLoggerStatsResponse() {
    }

    public void setHostNames(Iterable <HostStats> hostNames) {
        this.hostNames.clear();
        if (hostNames != null) {
            Iterables.addAll(this.hostNames,hostNames);
        }
    }

    public GetLoggerStatsResponse addHostName(HostStats hostName) {
        this.hostNames.add(hostName);
        return this;
    }

    public void setNote(String note) { this.note = note; }
    public List<HostStats> getHostNames() {
        return Collections.unmodifiableList(hostNames);
    }

    public String getNote() { return note; }
}
