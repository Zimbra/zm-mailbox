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
