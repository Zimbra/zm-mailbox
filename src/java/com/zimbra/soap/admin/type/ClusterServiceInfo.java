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

package com.zimbra.soap.admin.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.ClusterConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ClusterServiceInfo {

    // soap-admin.txt implies that there is a "starts" attribute but handler does not add that
    /**
     * @zm-api-field-tag cluster-service-name
     * @zm-api-field-description Cluster service name
     */
    @XmlAttribute(name=ClusterConstants.A_CLUSTER_SERVICE_NAME /* name */, required=true)
    private final String name;

    /**
     * @zm-api-field-tag cluster-service-name
     * @zm-api-field-description Cluster service name
     */
    @XmlAttribute(name=ClusterConstants.A_CLUSTER_SERVICE_STATE /* state */, required=true)
    private final String state;

    /**
     * @zm-api-field-tag cluster-service-owner-server
     * @zm-api-field-description Name of server that owns this service
     */
    @XmlAttribute(name=ClusterConstants.A_CLUSTER_SERVICE_OWNER /* owner */, required=true)
    private final String owner;

    /**
     * @zm-api-field-tag cluster-service-last-owner-server
     * @zm-api-field-description Name of server that last owned this service
     */
    @XmlAttribute(name=ClusterConstants.A_CLUSTER_SERVICE_LAST_OWNER /* lastOwner */, required=true)
    private final String lastOwner;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ClusterServiceInfo() {
        this((String) null, (String) null, (String) null, (String) null);
    }

    public ClusterServiceInfo(String name, String state, String owner, String lastOwner) {
        this.name = name;
        this.state = state;
        this.owner = owner;
        this.lastOwner = lastOwner;
    }

    public String getName() { return name; }
    public String getState() { return state; }
    public String getOwner() { return owner; }
    public String getLastOwner() { return lastOwner; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("state", state)
            .add("owner", owner)
            .add("lastOwner", lastOwner);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
