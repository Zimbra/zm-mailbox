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

import com.google.common.collect.Lists;
import com.google.common.collect.Iterables;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.CalendarResourceInfo;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_SEARCH_CALENDAR_RESOURCES_RESPONSE)
public class SearchCalendarResourcesResponse {

    /**
     * @zm-api-field-tag more-flag
     * @zm-api-field-description <b>1 (true)</b> if more calendar resources to return
     */
    @XmlAttribute(name=AdminConstants.A_MORE, required=true)
    private ZmBoolean more;

    /**
     * @zm-api-field-tag search-total
     * @zm-api-field-description Total number of calendar resources that matched search (not affected by limit/offset)
     */
    @XmlAttribute(name=AdminConstants.A_SEARCH_TOTAL, required=true)
    private long searchTotal;

    /**
     * @zm-api-field-description Information about calendar resources
     */
    @XmlElement(name=AdminConstants.E_CALENDAR_RESOURCE)
    private List <CalendarResourceInfo> calResources = Lists.newArrayList();

    public SearchCalendarResourcesResponse() {
        this(false, 0L, (Iterable <CalendarResourceInfo>) null);
    }

    public SearchCalendarResourcesResponse(boolean more, long searchTotal,
            Iterable <CalendarResourceInfo> calResources) {
        setMore(more);
        setSearchTotal(searchTotal);
        setCalResources(calResources);
    }

    public void setCalResources(Iterable <CalendarResourceInfo> calResources) {
        this.calResources.clear();
        if (calResources != null) {
            Iterables.addAll(this.calResources, calResources);
        }
    }

    public void addCalendarResource(CalendarResourceInfo calResource ) {
        this.calResources.add(calResource);
    }

    public List <CalendarResourceInfo> getCalResources() {
        return Collections.unmodifiableList(calResources);
    }
    public void setMore(boolean more) { this.more = ZmBoolean.fromBool(more); }

    public long getSearchTotal() { return searchTotal; }
    public boolean isMore() { return ZmBoolean.toBool(more); }
    public void setSearchTotal(long searchTotal) {
        this.searchTotal = searchTotal;
    }

}
