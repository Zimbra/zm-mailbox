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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.ClusterConstants;
import com.zimbra.soap.admin.type.ClusterServerInfo;
import com.zimbra.soap.admin.type.ClusterServiceInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=ClusterConstants.E_GET_CLUSTER_STATUS_RESPONSE)
@XmlType(propOrder = {})
public class GetClusterStatusResponse {

    /**
     * @zm-api-field-description Cluster name
     */
    @XmlElement(name=ClusterConstants.E_CLUSTER_NAME /* clusterName */, required=false)
    private String clusterName;

    /**
     * @zm-api-field-description Information on cluster servers
     */
    @XmlElementWrapper(name=ClusterConstants.A_CLUSTER_SERVERS /* servers */, required=false)
    @XmlElement(name=ClusterConstants.A_CLUSTER_SERVER /* server */, required=false)
    private List<ClusterServerInfo> servers = Lists.newArrayList();

    /**
     * @zm-api-field-description Information on cluster services
     */
    @XmlElementWrapper(name=ClusterConstants.A_CLUSTER_SERVICES /* services */, required=false)
    @XmlElement(name=ClusterConstants.A_CLUSTER_SERVICE /* service */, required=false)
    private List<ClusterServiceInfo> services = Lists.newArrayList();

    public GetClusterStatusResponse() {
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }
    public void setServers(Iterable <ClusterServerInfo> servers) {
        this.servers.clear();
        if (servers != null) {
            Iterables.addAll(this.servers,servers);
        }
    }

    public void addServer(ClusterServerInfo server) {
        this.servers.add(server);
    }

    public void setServices(Iterable <ClusterServiceInfo> services) {
        this.services.clear();
        if (services != null) {
            Iterables.addAll(this.services,services);
        }
    }

    public void addService(ClusterServiceInfo service) {
        this.services.add(service);
    }

    public String getClusterName() { return clusterName; }

    public List<ClusterServerInfo> getServers() {
        if ((servers == null) || (servers.size() == 0)) {
            return null;
        } else {
            return Collections.unmodifiableList(servers);
        }
    }

    public List<ClusterServiceInfo> getServices() {
        if ((services == null) || (services.size() == 0)) {
            return null;
        } else {
            return Collections.unmodifiableList(services);
        }
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("clusterName", clusterName)
            .add("servers", servers)
            .add("services", services);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
