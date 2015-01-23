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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.JavaType;

import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.qless.JSON;


public class ConsulClient {
    public static final String DEFAULT_URL = "http://127.0.0.1:8500";
    protected HttpClient httpClient = ZimbraHttpConnectionManager.getExternalHttpConnMgr().getDefaultHttpClient();
    protected String url;
    protected ObjectMapper objectMapper = new ObjectMapper();


    public ConsulClient() {
        this(DEFAULT_URL);
    }

    public ConsulClient(String url) {
        this.url = url;
    }

    protected HttpMethod put(String url, String json/**, UrlParameters... urlParams*/) throws HttpException, IOException {
        StringRequestEntity requestEntity = new StringRequestEntity(json, "application/json", "UTF-8");
        PutMethod method = new PutMethod(url);
        method.setRequestEntity(requestEntity);
        httpClient.executeMethod(method);
        return method;
    }

    /** De-register a service. */
    public void agentDeregister(CatalogRegistration.Service service) throws IOException {
        agentDeregister(service.id);
    }

    /** De-register a service. */
    public void agentDeregister(String serviceID) throws IOException {
        HttpMethod method = put(url + "/v1/agent/service/deregister/" + serviceID, "");
        if (method.getStatusCode() != 200) {
            throw new IOException(method.getStatusLine().toString());
        }
    }

    public void agentRegister(CatalogRegistration.Service service) throws IOException {
        String json = objectMapper.writeValueAsString(service);
        HttpMethod method = put(url + "/v1/agent/service/register", json);
        if (method.getStatusCode() != 200) {
            throw new IOException(method.getStatusLine().toString());
        }
    }

    /** Queries /v1/health/service/<serviceID>[?passing] for all global instances of a service */
    public List<ServiceHealthResponse> health(String serviceID, boolean passingOnly) throws IOException {
        HttpMethod method = put(url + "/v1/health/service/" + serviceID + (passingOnly ? "?passing" : ""), "");
        if (method.getStatusCode() != 200) {
            throw new IOException(method.getStatusLine().toString());
        }
        JavaType javaType = objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, ServiceHealthResponse.class);
        List<ServiceHealthResponse> result = JSON.parse(method.getResponseBodyAsString(), javaType);
        return result;
    }

    /** Queries /v1/health/node/<hostName> for all instances on a node */
    public List<NodeHealthResponse> health(String hostName) throws IOException {
        HttpMethod method = put(url + "/v1/health/node/" + hostName, "");
        if (method.getStatusCode() != 200) {
            throw new IOException(method.getStatusLine().toString());
        }
        JavaType javaType = objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, NodeHealthResponse.class);
        List<NodeHealthResponse> response = JSON.parse(method.getResponseBodyAsString(), javaType);
        return response;
    }

    /** Contact the Consul agent to determine whether it is reachable and responsive */
    public void ping() throws IOException {
        HttpMethod method = new HeadMethod(url + "/v1/agent/self");
        int statusCode = httpClient.executeMethod(method);
        if (statusCode != 200) {
            throw new IOException(method.getStatusLine().toString());
        }
    }
}
