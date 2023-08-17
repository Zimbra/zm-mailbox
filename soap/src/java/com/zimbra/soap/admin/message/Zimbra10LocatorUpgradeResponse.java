/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2023 Synacor, Inc.
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
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.VolumeInfo;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = AdminConstants.E_ZIMBRA10_LOCATOR_UPGRADE_RESPONSE)
public final class Zimbra10LocatorUpgradeResponse {

    /**
     * @zm-api-field-description Information about the execution status
     */
    @XmlElement(name = AdminConstants.E_STATUS_NAME, required = true)
    private final boolean status;
    private Zimbra10LocatorUpgradeResponse() {
        this(false);
    }
    public Zimbra10LocatorUpgradeResponse(boolean status) {
        this.status = status;
    }
    public boolean isStatus() {
        return status;
    }
}