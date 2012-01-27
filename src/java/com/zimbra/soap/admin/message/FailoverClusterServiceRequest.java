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

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.ClusterConstants;
import com.zimbra.soap.admin.type.FailoverClusterServiceSpec;

/**
 * @zm-api-command-description Failover Cluster Service
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=ClusterConstants.E_FAILOVER_CLUSTER_SERVICE_REQUEST)
public class FailoverClusterServiceRequest {

    /**
     * @zm-api-field-description Failover details
     */
    @XmlElement(name=ClusterConstants.A_CLUSTER_SERVICE /* service */, required=false)
    private FailoverClusterServiceSpec service;

    public FailoverClusterServiceRequest() {
    }

    public void setService(FailoverClusterServiceSpec service) {
        this.service = service;
    }
    public FailoverClusterServiceSpec getService() { return service; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("service", service);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
