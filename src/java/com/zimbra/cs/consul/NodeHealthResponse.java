/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.consul;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.google.common.base.Objects;



@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class NodeHealthResponse {
    @JsonProperty("Node") public String node;
    @JsonProperty("CheckID") public String checkID;
    @JsonProperty("Name") public String name;
    @JsonProperty("Status") public String status;
    @JsonProperty("Notes") public String notes;
    @JsonProperty("Output") public String output;
    @JsonProperty("ServiceID") public String serviceID;
    @JsonProperty("ServiceName") public String serviceName;

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("Node", node)
            .add("CheckID", checkID)
            .add("Name", name)
            .add("Status", status)
            .add("Notes", notes)
            .add("Output", output)
            .add("ServiceID", serviceID)
            .add("ServiceName", serviceName)
            .toString();
    }
}
