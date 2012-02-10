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
public class FailoverClusterServiceSpec {

    /**
     * @zm-api-field-tag cluster-svc-name
     * @zm-api-field-description Cluster service name
     */
    @XmlAttribute(name=ClusterConstants.A_CLUSTER_SERVICE_NAME /* name */, required=true)
    private final String name;

    /**
     * @zm-api-field-tag new-server
     * @zm-api-field-description New Server
     */
    @XmlAttribute(name=ClusterConstants.A_FAILOVER_NEW_SERVER /* newServer */, required=true)
    private final String newServer;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private FailoverClusterServiceSpec() {
        this((String) null, (String) null);
    }

    public FailoverClusterServiceSpec(String name, String newServer) {
        this.name = name;
        this.newServer = newServer;
    }

    public String getName() { return name; }
    public String getNewServer() { return newServer; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("newServer", newServer);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
