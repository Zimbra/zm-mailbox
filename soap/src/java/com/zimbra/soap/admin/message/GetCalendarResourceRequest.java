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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.AttributeSelectorImpl;
import com.zimbra.soap.admin.type.CalendarResourceSelector;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Get a calendar resource
 * <b>Access</b>: domain admin sufficient
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_CALENDAR_RESOURCE_REQUEST)
public class GetCalendarResourceRequest extends AttributeSelectorImpl {

    /**
     * @zm-api-field-tag apply-cos
     * @zm-api-field-description Flag whether to apply Class of Service (COS)
     * <table>
     * <tr> <td> <b>1 (true) [default]</b> </td> <td> COS rules apply and unset attrs on the calendar resource will
     *                                                get their value from the COS. </td> </tr>
     * <tr> <td> <b>0 (false) </b> </td> <td> only attributes directly set on the calendar resource will be
     *                                        returned * </td> </tr>
     * </table>
     */
    @XmlAttribute(name=AdminConstants.A_APPLY_COS, required=false)
    private ZmBoolean applyCos;
    /**
     * @zm-api-field-description Specify calendar resource
     */
    @XmlElement(name=AdminConstants.E_CALENDAR_RESOURCE)
    private CalendarResourceSelector calResource;

    public GetCalendarResourceRequest() {
        this((CalendarResourceSelector) null, (Boolean) null);
    }

    public GetCalendarResourceRequest(
            CalendarResourceSelector calResource) {
        this(calResource, (Boolean) null, (Iterable<String>) null);
    }

    public GetCalendarResourceRequest(
            CalendarResourceSelector calResource,
            Boolean applyCos) {
        this(calResource, applyCos, (Iterable<String>) null);
    }

    public GetCalendarResourceRequest(
            CalendarResourceSelector calResource,
            Boolean applyCos,
            Iterable<String> attrs) {
        super(attrs);
        setApplyCos(applyCos);
        setCalResource(calResource);
    }

    public void setApplyCos(Boolean applyCos) { this.applyCos = ZmBoolean.fromBool(applyCos); }
    public void setCalResource(CalendarResourceSelector calResource) {
        this.calResource = calResource;
    }

    public Boolean getApplyCos() { return ZmBoolean.toBool(applyCos); }
    public CalendarResourceSelector getCalResource() { return calResource; }
}
