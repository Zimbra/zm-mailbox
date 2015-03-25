package com.zimbra.cs.consul;

import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.google.common.base.Objects;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class CreateSessionRequest {
    @JsonProperty("Name") public String name;
    @JsonProperty("TTL") public String ttl;
    @JsonProperty("Checks") public List<String> checks;

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("Name", name)
            .add("TTL", ttl)
            .add("Checks", checks)
            .toString();

    }

}
