package com.zimbra.cs.consul;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.google.common.base.Objects;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class NodeInfoResponse {
    @JsonProperty("Node") public NodeInfo node;
    @JsonProperty("Services") public ServiceListResponse services;

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("Node", node)
            .add("Services", services)
            .toString();
    }


    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    public class NodeInfo {
        @JsonProperty("Node") public String nodeName;
        @JsonProperty("Address") public String address;

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                .add("Node", nodeName)
                .add("Address", address)
                .toString();
        }
    }
}
