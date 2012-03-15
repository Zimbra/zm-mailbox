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

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("status", status);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
