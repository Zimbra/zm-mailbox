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

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.google.common.base.Objects;



@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class CatalogRegistration {
    @JsonProperty("DataCenter") public String dc;
    @JsonProperty("Node") public String node;
    @JsonProperty("Address") public String address;
    @JsonProperty("Service") public Service service;
    @JsonProperty("Check") public Void check;


    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    public static class Service {
        @JsonProperty("ID") public String id;
        @JsonProperty("Name") public String name;
        @JsonProperty("Tags") public List<String> tags = new ArrayList<>();
        @JsonProperty("Port") public Integer port;

        public Service() {
        }

        public Service(String name) {
            this.name = name;
        }

        public Service(String id, String name) {
            this(name);
            this.id = id;
        }

        public Service(String name, int port) {
            this(name);
            this.port = port;
        }

        public Service(String id, String name, int port) {
            this(name, port);
            this.id = id;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                .add("ID", id)
                .add("Name", name)
                .add("Tags", tags)
                .add("Port", port)
                .toString();
        }
    }
}
