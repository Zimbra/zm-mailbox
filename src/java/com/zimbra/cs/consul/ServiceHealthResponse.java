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

import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.google.common.base.Objects;



@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class ServiceHealthResponse {
    @JsonProperty("Node") public Node node;
    @JsonProperty("Service") public Service service;
    @JsonProperty("Checks") public List<Check> checks;

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("Node", node)
            .add("Service", service)
            .add("Checks", checks)
            .toString();
    }


    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    public static class Node {
        @JsonProperty("Node") public String name;
        @JsonProperty("Address") public String address;

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                .add("Node", name)
                .add("Address", address)
                .toString();
        }
    }


    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    public static class Service {
        @JsonProperty("ID") public String id;
        @JsonProperty("Service") public String name;
        @JsonProperty("Tags") public List<String> tags;
        @JsonProperty("Port") public String port;

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                .add("ID", id)
                .add("Service", name)
                .add("Tags", tags)
                .add("Port", port)
                .toString();
        }
    }


    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    public static class Check {
        @JsonProperty("Node") public String node;
        @JsonProperty("CheckID") public String id;
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
                .add("CheckID", id)
                .add("Name", name)
                .add("Status", status)
                .add("Notes", notes)
                .add("Output", output)
                .add("ServiceID", serviceID)
                .add("ServiceName", serviceName)
                .toString();
        }
    }
}
