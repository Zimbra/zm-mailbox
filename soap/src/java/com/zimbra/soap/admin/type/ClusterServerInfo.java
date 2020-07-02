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
public class ClusterServerInfo {

    /**
     * @zm-api-field-tag cluster-server-name
     * @zm-api-field-description Cluster server name
     */
    @XmlAttribute(name=ClusterConstants.A_CLUSTER_SERVER_NAME /* name */, required=true)
    private final String name;

    /**
     * @zm-api-field-tag cluster-server-status
     * @zm-api-field-description Server status - 1 or 0
     */
    @XmlAttribute(name=ClusterConstants.A_CLUSTER_SERVER_STATUS /* status */, required=true)
    private final int status;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ClusterServerInfo() {
        this((String) null, -1);
    }

    public ClusterServerInfo(String name, int status) {
        this.name = name;
        this.status = status;
    }

    public String getName() { return name; }
    public int getStatus() { return status; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("status", status);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
