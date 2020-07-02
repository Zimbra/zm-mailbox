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

import com.google.common.base.MoreObjects;
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

import com.zimbra.soap.json.jackson.annotate.ZimbraJsonArrayForWrapper;

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
    @ZimbraJsonArrayForWrapper
    @XmlElementWrapper(name=ClusterConstants.A_CLUSTER_SERVERS /* servers */, required=false)
    @XmlElement(name=ClusterConstants.A_CLUSTER_SERVER /* server */, required=false)
    private List<ClusterServerInfo> servers = Lists.newArrayList();

    /**
     * @zm-api-field-description Information on cluster services
     */
    @ZimbraJsonArrayForWrapper
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

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("clusterName", clusterName)
            .add("servers", servers)
            .add("services", services);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
