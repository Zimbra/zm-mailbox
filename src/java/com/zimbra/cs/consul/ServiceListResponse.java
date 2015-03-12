package com.zimbra.cs.consul;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonAnyGetter;
import org.codehaus.jackson.annotate.JsonAnySetter;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.google.common.base.Objects;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class ServiceListResponse {

    protected Map<String, Service> servicesById = new HashMap<String, Service>();

    @JsonAnyGetter
    public Map<String, Service> getServicesById() {
        return servicesById;
    }

    @JsonAnySetter
    public void setServicesById(String name, Service value) {
        servicesById.put(name, value);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("services", servicesById)
            .toString();
    }

    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    public static class Service {
        @JsonProperty("ID") public String id;
        @JsonProperty("Service") public String service;
        @JsonProperty("Tags") public List<String> tags = new ArrayList<>();
        @JsonProperty("Port") public Integer port;
        @JsonProperty("Address") public String address;

        public Service() {
        }

        public Service(String service) {
            this.service = service;
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
                .add("Service", service)
                .add("Tags", tags)
                .add("Port", port)
                .add("Address", address)
                .toString();
        }
    }

}
