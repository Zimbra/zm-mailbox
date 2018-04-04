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

package com.zimbra.soap.replication.type;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.ReplicationConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraUniqueElement;

@XmlAccessorType(XmlAccessType.NONE)
public class ReplicationMasterStatus {

    /**
     * @zm-api-field-tag master-operating-mode-invalid|normal|slaveless|catchup
     * @zm-api-field-description Master's operating mode - <b>invalid|normal|slaveless|catchup</b>
     */
    @XmlAttribute(name=ReplicationConstants.A_MASTER_OPERATING_MODE /* masterOperatingMode */, required=true)
    private final String masterOperatingMode;

    /**
     * @zm-api-field-description Catchup status
     */
    @ZimbraUniqueElement
    @XmlElement(name=ReplicationConstants.E_CATCHUP_STATUS /* catchupStatus */, required=false)
    private ReplicationMasterCatchupStatus catchupStatus;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ReplicationMasterStatus() {
        this((String) null);
    }

    public ReplicationMasterStatus(String masterOperatingMode) {
        this.masterOperatingMode = masterOperatingMode;
    }

    public void setCatchupStatus(ReplicationMasterCatchupStatus catchupStatus) {
        this.catchupStatus = catchupStatus;
    }
    public String getMasterOperatingMode() { return masterOperatingMode; }
    public ReplicationMasterCatchupStatus getCatchupStatus() { return catchupStatus; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("masterOperatingMode", masterOperatingMode)
            .add("catchupStatus", catchupStatus);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
