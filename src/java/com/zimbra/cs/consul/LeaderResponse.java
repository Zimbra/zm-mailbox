package com.zimbra.cs.consul;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.google.common.base.Objects;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class LeaderResponse {
    @JsonProperty("CreateIndex") public int createIndex;
    @JsonProperty("ModifyIndex") public int modifyIndex;
    @JsonProperty("LockIndex") public int lockIndex;
    @JsonProperty("Key") public String key;
    @JsonProperty("Flags") public String flags;
    @JsonProperty("Value") public String value;
    @JsonProperty("Session") public String sessionId;

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("CreateIndex", createIndex)
            .add("ModifyIndex", modifyIndex)
            .add("LockIndex", lockIndex)
            .add("Key", key)
            .add("Flags", flags)
            .add("Value", value)
            .add("Session", sessionId)
            .toString();
    }
}
