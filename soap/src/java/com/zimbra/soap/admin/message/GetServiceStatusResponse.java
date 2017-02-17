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
