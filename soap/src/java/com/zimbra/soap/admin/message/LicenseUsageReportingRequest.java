/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2024 Synacor, Inc.
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

import com.zimbra.common.soap.AdminConstants;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name= AdminConstants.E_LICENSE_USAGE_REPORTING_REQUEST)
public class LicenseUsageReportingRequest {

    /**
     * @zm-api-field-tag action
     * @zm-api-field-description action to perform
     */
    @XmlAttribute(name=AdminConstants.A_USAGE_REPORTING_ACTION /* usageReportingAction */, required=true)
    private final String usageReportingAction;


    public LicenseUsageReportingRequest(String usageReportingAction) {
        this.usageReportingAction = usageReportingAction;
    }
}
