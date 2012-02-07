
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

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.CalendarResourceInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_ALL_CALENDAR_RESOURCES_RESPONSE)
public class GetAllCalendarResourcesResponse {

    /**
     * @zm-api-field-description Information on calendar resources
     */
    @XmlElement(name=AdminConstants.E_CALENDAR_RESOURCE)
    private List <CalendarResourceInfo> calResources = Lists.newArrayList();

    public GetAllCalendarResourcesResponse() {
    }

    public void setCalendarResourceList(Iterable <CalendarResourceInfo> calResources) {
        this.calResources.clear();
        if (calResources != null) {
            Iterables.addAll(this.calResources, calResources);
        }
    }

    public void addCalendarResource(CalendarResourceInfo calResource ) {
        this.calResources.add(calResource);
    }

    public List <CalendarResourceInfo> getCalendarResourceList() {
        return Collections.unmodifiableList(calResources);
    }
}
