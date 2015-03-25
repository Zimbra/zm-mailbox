package com.zimbra.cs.consul;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.google.common.base.Objects;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class SessionResponse {
    @JsonProperty("ID") public String id;
    @JsonProperty("Name") public String name;
    @JsonProperty("LockDelay") public String lockDelay;
    @JsonProperty("Node") public String nodeName;

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("ID", id)
            .add("Name", name)
            .add("LockDelay", lockDelay)
            .add("Node", nodeName)
            .toString();
    }
}
