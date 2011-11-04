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

package com.zimbra.soap.replication.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.ReplicationConstants;
import com.zimbra.soap.replication.type.ReplicationMasterStatus;
import com.zimbra.soap.replication.type.ReplicationSlaveStatus;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=ReplicationConstants.E_REPLICATION_STATUS_RESPONSE)
@XmlType(propOrder = {})
public class ReplicationStatusResponse {

    @XmlAttribute(name=ReplicationConstants.A_REPLICATION_ENABLED /* replicationEnabled */, required=true)
    private final ZmBoolean replicationEnabled;

    @XmlAttribute(name=ReplicationConstants.A_CURRENT_ROLE /* currentRole */, required=false)
    private String currentRole;

    @XmlAttribute(name=ReplicationConstants.A_ORIGINAL_ROLE /* originalRole */, required=false)
    private String originalRole;

    // Only one of masterStatus and slaveStatus can be present

    @XmlElement(name=ReplicationConstants.E_MASTER_STATUS /* masterStatus */, required=false)
    private ReplicationMasterStatus masterStatus;

    @XmlElement(name=ReplicationConstants.E_SLAVE_STATUS /* slaveStatus */, required=false)
    private ReplicationSlaveStatus slaveStatus;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ReplicationStatusResponse() {
        this(false);
    }

    public ReplicationStatusResponse(boolean replicationEnabled) {
        this.replicationEnabled = ZmBoolean.fromBool(replicationEnabled);
    }

    public void setCurrentRole(String currentRole) {
        this.currentRole = currentRole;
    }
    public void setOriginalRole(String originalRole) {
        this.originalRole = originalRole;
    }
    public void setMasterStatus(ReplicationMasterStatus masterStatus) {
        this.masterStatus = masterStatus;
    }
    public void setSlaveStatus(ReplicationSlaveStatus slaveStatus) {
        this.slaveStatus = slaveStatus;
    }
    public boolean getReplicationEnabled() { return ZmBoolean.toBool(replicationEnabled); }
    public String getCurrentRole() { return currentRole; }
    public String getOriginalRole() { return originalRole; }
    public ReplicationMasterStatus getMasterStatus() { return masterStatus; }
    public ReplicationSlaveStatus getSlaveStatus() { return slaveStatus; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("replicationEnabled", replicationEnabled)
            .add("currentRole", currentRole)
            .add("originalRole", originalRole)
            .add("masterStatus", masterStatus)
            .add("slaveStatus", slaveStatus);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
