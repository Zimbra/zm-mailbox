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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Lists;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.ServiceStatus;
import com.zimbra.soap.admin.type.TimeZoneInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_SERVICE_STATUS_RESPONSE)
public class GetServiceStatusResponse {

    /**
     * @zm-api-field-description TimeZone information
     */
    @XmlElement(name=AdminConstants.E_TIMEZONE, required=true)
    private TimeZoneInfo timezone;

    /**
     * @zm-api-field-description Service status information
     */
    @XmlElement(name=AdminConstants.E_STATUS, required=false)
    private List <ServiceStatus> serviceStatuses = Lists.newArrayList();

    public GetServiceStatusResponse() {
    }

    public GetServiceStatusResponse setServiceStatuses(Collection<ServiceStatus> serviceStatuses) {
        this.serviceStatuses.clear();
        if (serviceStatuses != null) {
            this.serviceStatuses.addAll(serviceStatuses);
        }
        return this;
    }

    public GetServiceStatusResponse addServiceStatus(ServiceStatus attr) {
        serviceStatuses.add(attr);
        return this;
    }

    public List<ServiceStatus> getServiceStatuses() {
        return Collections.unmodifiableList(serviceStatuses);
    }

    public void setTimezone(TimeZoneInfo timezone) {
        this.timezone = timezone;
    }

    public TimeZoneInfo getTimezone() { return timezone; }
}
