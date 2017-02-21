/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2016 Synacor, Inc.
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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.AlwaysOnClusterInfo;

@XmlRootElement(name=AdminConstants.E_GET_ALL_ALWAYSONCLUSTERS_RESPONSE)
public class GetAllAlwaysOnClustersResponse {

    /**
     * @zm-api-field-description Information about alwaysOnClusters
     */
    @XmlElement(name=AdminConstants.E_ALWAYSONCLUSTER)
    private final List <AlwaysOnClusterInfo> clusterList = new ArrayList<AlwaysOnClusterInfo>();

    public GetAllAlwaysOnClustersResponse() {
    }

    public void addAlwaysOnCluster(AlwaysOnClusterInfo cluster ) {
        this.getAlwaysOnClusterList().add(cluster);
    }

    public List<AlwaysOnClusterInfo> getAlwaysOnClusterList() { return clusterList; }
}
