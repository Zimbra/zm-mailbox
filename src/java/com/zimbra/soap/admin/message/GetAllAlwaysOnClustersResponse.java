/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
