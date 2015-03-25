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
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.http.HttpStatus;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.JavaType;

import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;


public class ConsulClient {
    public static final String DEFAULT_URL = "http://127.0.0.1:8500";
    protected HttpClient httpClient = ZimbraHttpConnectionManager.getInternalHttpConnMgr().getDefaultHttpClient();
    protected String url;
    protected ObjectMapper objectMapper = new ObjectMapper();


    public ConsulClient() {
        this(DEFAULT_URL);
    }

    public ConsulClient(String url) {
        this.url = url;
    }

    protected HttpMethod put(String url, String json) throws HttpException, IOException {
        PutMethod method = new PutMethod(url);
        if (json != null) {
            StringRequestEntity requestEntity = new StringRequestEntity(json, "application/json", "UTF-8");
            method.setRequestEntity(requestEntity);
        }
        httpClient.executeMethod(method);
        return method;
    }

    protected HttpMethod put(String url, String body, String queryString) throws IOException {
        PutMethod method = new PutMethod(url);
        if (body != null) {
            StringRequestEntity requestEntity = new StringRequestEntity(body, "text/plain", "UTF-8");
            method.setRequestEntity(requestEntity);
        }
        if (queryString != null) {
            method.setQueryString(queryString);
        }
        httpClient.executeMethod(method);
        return method;
    }

    protected HttpMethod get(String url, String queryString) throws HttpException, IOException {
        GetMethod method = new GetMethod(url);
        if (queryString != null) {
            method.setQueryString(queryString);
        }
        httpClient.executeMethod(method);
        return method;
    }

    private void release(HttpMethod method) {
        if (method != null) {
            method.releaseConnection();
        }
    }

    /** De-register a service. */
    public void agentDeregister(CatalogRegistration.Service service) throws IOException {
        agentDeregister(service.id);
    }

    /** De-register a service. */
    public void agentDeregister(String serviceID) throws IOException {
        HttpMethod method = null;
        try {
            method = put(url + "/v1/agent/service/deregister/" + serviceID, "");
            if (method.getStatusCode() != 200) {
                throw new IOException(method.getStatusLine().toString());
            }
        } finally {
            release(method);
        }
    }

    /**
     * Register a service
     * @param service
     * @throws IOException
     */
    public void agentRegister(CatalogRegistration.Service service) throws IOException {
        String json = objectMapper.writeValueAsString(service);
        HttpMethod method = null;
        try {
            method = put(url + "/v1/agent/service/register", json);
            if (method.getStatusCode() != 200) {
                throw new IOException(method.getStatusLine().toString());
            }
        } finally {
            release(method);
        }
    }

    /**
     * List services known to the agent
     * @return
     * @throws IOException
     */
    public ServiceListResponse listAgentServices() throws IOException {
        HttpMethod method = null;
        try {
            method = get(url + "/v1/agent/services", null);
            if (method.getStatusCode() != 200) {
                throw new IOException(method.getStatusLine().toString());
            }

            JavaType javaType = objectMapper.getTypeFactory().constructType(ServiceListResponse.class);
            ServiceListResponse response = JSON.parse(method.getResponseBodyAsString(), javaType);
            return response;
        } finally {
            release(method);
        }
    }

    /**
     * Create a session
     * @param serviceId - the serviceId (not the service name)
     * @param ttlInSeconds - ttl, or 0 if using check-based session
     * @param checks - names of previously created checks, or null for TTL
     * @return - object holding new session info
     * @throws IOException
     */
    public SessionResponse createSession(String serviceId, int ttlInSeconds, List<String> checks) throws IOException {
        CreateSessionRequest name = new CreateSessionRequest();
        //TODO: currently sticking the serviceId in session name for convenience; awkward but works
        name.name = serviceId;
        if (ttlInSeconds > 0) {
            name.ttl = ttlInSeconds + "s";
        }
        name.checks = checks;
        String json = objectMapper.writeValueAsString(name);
        HttpMethod method = null;
        try {
            method = put(url + "/v1/session/create", json);
            if (method.getStatusCode() != 200) {
                throw new IOException(method.getStatusLine().toString());
            }
            JavaType javaType = objectMapper.getTypeFactory().constructType(SessionResponse.class);
            SessionResponse response = JSON.parse(method.getResponseBodyAsString(), javaType);
            return response;
        } finally {
            release(method);
        }
    }

    /**
     * delete a session without exception on failure
     * @param sessionId
     */
    public void deleteSessionSilent(String sessionId) {
        try {
            deleteSession(sessionId);
        } catch (Exception e) {
            ZimbraLog.misc.warn("Exception deleting session", e);
        }
    }

    /**
     * Delete a session
     * @param sessionId
     * @throws IOException
     */
    public void deleteSession(String sessionId) throws IOException {
        HttpMethod method = null;
        try {
            method = put(url + "/v1/session/destroy/" + sessionId, null);
            if (method.getStatusCode() != 200) {
                throw new IOException(method.getStatusLine().toString());
            }
        } finally {
            release(method);
        }
    }

    /**
     * Renew a TTL-based session
     * @param sessionId
     * @throws IOException
     */
    public void renewSession(String sessionId) throws IOException {
        HttpMethod method = null;
        try {
            method = put(url + "/v1/session/renew/" + sessionId, null);
            if (method.getStatusCode() != 200) {
                throw new IOException(method.getStatusLine().toString());
            }
        } finally {
            release(method);
        }
    }

    /**
     * Get session info
     * @param sessionId
     * @return - object holding the session info
     * @throws IOException
     */
    public SessionResponse getSessionInfo(String sessionId) throws IOException {
        HttpMethod method = null;
        try {
            method = get(url + "/v1/session/info/" + sessionId, null);
            if (method.getStatusCode() != 200) {
                throw new IOException(method.getStatusLine().toString());
            }
            JavaType javaType = objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, SessionResponse.class);
            List<SessionResponse> response = JSON.parse(method.getResponseBodyAsString(), javaType);
            return response == null || response.size() < 1 ? null : response.get(0);
        } finally {
            release(method);
        }
    }

    /**
     * Get catalog node info
     * @param nodeName - the node
     * @return - object holding the node info
     * @throws IOException
     */
    public NodeInfoResponse getNodeInfo(String nodeName) throws IOException {
        HttpMethod method = null;
        try {
            method = get(url + "/v1/catalog/node/" + nodeName, null);
            if (method.getStatusCode() != 200) {
                throw new IOException(method.getStatusLine().toString());
            }
            JavaType javaType = objectMapper.getTypeFactory().constructType(NodeInfoResponse.class);
            NodeInfoResponse response = JSON.parse(method.getResponseBodyAsString(), javaType);
            return response;
        } finally {
            release(method);
        }
    }

    private LeaderResponse leaderRespFromHttpMethod(HttpMethod method) throws IOException {
        JavaType javaType = objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, LeaderResponse.class);
        List<LeaderResponse> response = JSON.parse(method.getResponseBodyAsString(), javaType);
        if (response.size() != 1) {
            throw new IOException("unexpected response, leaders should(?) always be 1 if found");
        }
        return response.get(0);
    }

    /**
     * Find the leader for a given service
     * @param service - the service
     * @return - object holding the leader info
     * @throws IOException
     */
    public LeaderResponse findLeader(CatalogRegistration.Service service) throws IOException {
        HttpMethod method = null;
        try {
            method = get(url + "/v1/kv/service/" + service.name + "/leader", null);
            if (method.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                return null;
            } else if (method.getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException(method.getStatusLine().toString());
            }
            return leaderRespFromHttpMethod(method);
        } finally {
            release(method);
        }
    }

    private boolean isChanged(LeaderResponse leader, String knownSessionId) {
        if (knownSessionId != null) {
            if (leader == null || leader.sessionId == null) {
                return true;
            } else {
                return !knownSessionId.equals(leader.sessionId);
            }
        } else {
            return leader != null && leader.sessionId != null;
        }
    }

    /**
     * Wait for the leader to change from a known session
     * @param service - the service
     * @param knownSessionId - the current known session id, or null if unknown
     * @return - the new sessionid
     * @throws IOException
     */
    public String waitForLeaderChange(CatalogRegistration.Service service, String knownSessionId) throws IOException {
        LeaderResponse leader = findLeader(service);
        //start with non-blocking in case there was already a change
        if (!isChanged(leader, knownSessionId)) {
            do {
                //there is a leader, but it's not the session in question
                ZimbraLog.misc.info("waiting for leader to change from %s", knownSessionId);
                HttpMethod method = new GetMethod(url + "/v1/kv/service/" + service.name + "/leader");
                try {
                    if (leader != null) {
                        method.setQueryString("index=" + leader.modifyIndex);
                    }
                    HttpClient blockingClient = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
                    blockingClient.getParams().setSoTimeout(0);
                    blockingClient.executeMethod(method);
                    if (method.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                        //no leader, have to busy wait. short sleep so we don't flood
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                        }
                    } else if (method.getStatusCode() != HttpStatus.SC_OK) {
                        throw new IOException(method.getStatusLine().toString());
                    } else {
                        leader = leaderRespFromHttpMethod(method);
                    }
                } finally {
                    release(method);
                }
            } while (!isChanged(leader, knownSessionId));
        }
        String sessionId = (leader == null ? null : leader.sessionId);
        ZimbraLog.misc.info("leader for service %s is now session %s", service, sessionId);
        return sessionId;
    }

    /**
     * Atempt to acquire leadership
     * @param serviceName - name of the service
     * @param sessionId - id of the session
     * @param data - body; i.e. the value in key/value store
     * @return true if obtained, false if not
     * @throws IOException
     */
    public boolean acquireLeadership(String serviceName, String sessionId, String data) throws IOException {
        HttpMethod method = null;
        try {
            method = put(url + "/v1/kv/service/" + serviceName + "/leader", data, "acquire=" + sessionId);
            if (method.getStatusCode() != 200) {
                throw new IOException(method.getStatusLine().toString());
            }
            return Boolean.valueOf(method.getResponseBodyAsString().trim());
        } finally {
            release(method);
        }
    }

    /**
     * Release leadership
     * @param serviceId - service to release
     * @param sessionId - session which is currently the leader
     * @throws IOException
     */
    public void releaseLeadership(String serviceName, String sessionId) throws IOException {
        HttpMethod method = null;
        try {
            method = put(url + "/v1/kv/service/" + serviceName + "/leader", null, "release=" + sessionId);
            if (method.getStatusCode() != 200) {
                throw new IOException(method.getStatusLine().toString());
            } else if (!Boolean.valueOf(method.getResponseBodyAsString().trim())) {
                throw new IOException("received false response when releasing leadership");
            }
        } finally {
            release(method);
        }

    }

    /** Queries /v1/health/service/<serviceName>[?passing] for all global instances of a service */
    public List<ServiceHealthResponse> health(String serviceName, boolean passingOnly) throws IOException {
        HttpMethod method = null;
        try {
            method = put(url + "/v1/health/service/" + serviceName + (passingOnly ? "?passing" : ""), "");
            if (method.getStatusCode() != 200) {
                throw new IOException(method.getStatusLine().toString());
            }
            JavaType javaType = objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, ServiceHealthResponse.class);
            List<ServiceHealthResponse> result = JSON.parse(method.getResponseBodyAsString(), javaType);
            return result;
        } finally {
            release(method);
        }
    }

    /** Queries /v1/health/node/<hostName> for all instances on a node */
    public List<NodeHealthResponse> health(String hostName) throws IOException {
        HttpMethod method = null;
        try {
            method = put(url + "/v1/health/node/" + hostName, "");
            if (method.getStatusCode() != 200) {
                throw new IOException(method.getStatusLine().toString());
            }
            JavaType javaType = objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, NodeHealthResponse.class);
            List<NodeHealthResponse> response = JSON.parse(method.getResponseBodyAsString(), javaType);
            return response;
        } finally {
            release(method);
        }
    }

    /** Contact the Consul agent to determine whether it is reachable and responsive */
    public void ping() throws IOException {
        HttpMethod method = null;
        try {
            method = new HeadMethod(url + "/v1/agent/self");
            int statusCode = httpClient.executeMethod(method);
            if (statusCode != 200) {
                throw new IOException(method.getStatusLine().toString());
            }
        } finally {
            release(method);
        }
    }
}
