/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012 Zimbra, Inc.
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

package com.zimbra.soap.replication.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.ReplicationConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ReplicationSlaveStatus {

    /**
     * @zm-api-field-description Catchup status
     */
    @XmlElement(name=ReplicationConstants.E_CATCHUP_STATUS /* catchupStatus */, required=false)
    private ReplicationSlaveCatchupStatus catchupStatus;

    public ReplicationSlaveStatus() {
    }

    public void setCatchupStatus(ReplicationSlaveCatchupStatus catchupStatus) {
        this.catchupStatus = catchupStatus;
    }
    public ReplicationSlaveCatchupStatus getCatchupStatus() { return catchupStatus; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("catchupStatus", catchupStatus);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
