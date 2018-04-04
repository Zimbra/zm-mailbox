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

package com.zimbra.soap.admin.type;

import com.google.common.base.MoreObjects;
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

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("newServer", newServer);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
