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

package com.zimbra.soap.account.message;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.account.type.CalendarResourceInfo;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_SEARCH_CALENDAR_RESOURCES_RESPONSE)
@XmlType(propOrder = {})
public class SearchCalendarResourcesResponse {

    /**
     * @zm-api-field-tag sort-by
     * @zm-api-field-description Name of attribute sorted on. If not present then sorted by the calendar resource name.
     */
    @XmlAttribute(name=MailConstants.A_SORTBY /* sortBy */, required=false)
    private String sortBy;

    /**
     * @zm-api-field-description The 0-based offset into the results list to return as the first result for this
     * search operation.
     */
    @XmlAttribute(name=MailConstants.A_QUERY_OFFSET /* offset */, required=false)
    private Integer offset;

    /**
     * @zm-api-field-description Flags whether there are more results
     */
    @XmlAttribute(name=MailConstants.A_QUERY_MORE /* more */, required=false)
    private ZmBoolean more;

    /**
     * @zm-api-field-tag pagination-supported
     * @zm-api-field-description Flag whether the underlying search supported pagination.
     * <ul>
     * <li> <b>1 (true)</b> - limit and offset in the request was honored
     * <li> <b>0 (false)</b> - the underlying search does not support pagination. <b>limit</b> and <b>offset</b> in
     *      the request was not honored
     * </ul>
     */
    @XmlAttribute(name=AccountConstants.A_PAGINATION_SUPPORTED /* paginationSupported */, required=false)
    private ZmBoolean pagingSupported;

    /**
     * @zm-api-field-description Matching calendar resources
     */
    @XmlElement(name=AccountConstants.E_CALENDAR_RESOURCE /* calresource */, required=false)
    private List<CalendarResourceInfo> calendarResources = Lists.newArrayList();

    public SearchCalendarResourcesResponse() {
    }

    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public void setOffset(Integer offset) { this.offset = offset; }
    public void setMore(Boolean more) { this.more = ZmBoolean.fromBool(more); }
    public void setPagingSupported(Boolean pagingSupported) {
        this.pagingSupported = ZmBoolean.fromBool(pagingSupported);
    }
    public void setCalendarResources(Iterable <CalendarResourceInfo> calendarResources) {
        this.calendarResources.clear();
        if (calendarResources != null) {
            Iterables.addAll(this.calendarResources,calendarResources);
        }
    }

    public void addCalendarResource(CalendarResourceInfo calendarResource) {
        this.calendarResources.add(calendarResource);
    }

    public String getSortBy() { return sortBy; }
    public Integer getOffset() { return offset; }
    public Boolean getMore() { return ZmBoolean.toBool(more); }
    public Boolean getPagingSupported() { return ZmBoolean.toBool(pagingSupported); }
    public List<CalendarResourceInfo> getCalendarResources() {
        return Collections.unmodifiableList(calendarResources);
    }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("sortBy", sortBy)
            .add("offset", offset)
            .add("more", more)
            .add("pagingSupported", pagingSupported)
            .add("calendarResources", calendarResources);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
