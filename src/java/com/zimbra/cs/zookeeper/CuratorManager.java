/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zookeeper;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceCache;
import org.apache.curator.x.discovery.ServiceCacheBuilder;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;

import com.google.common.io.Closeables;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;

public class CuratorManager {

    private final CuratorFramework client;
    private static CuratorManager instance = null;
    private final ServiceDiscovery<Service> serviceDiscovery;
    private final ServiceCache<Service> serviceCache;
    private final ServiceInstance<Service> thisInstance;
    private static boolean initialized = false;

    private static final String LOCKS = "/locks/";
    private static final String DATA = "/data/";
    private static final String SERVICE = "/service";

    private CuratorManager(String zkAddress) throws Exception {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        client = CuratorFrameworkFactory.newClient(zkAddress, retryPolicy);
        Server server = Provisioning.getInstance().getLocalServer();
        thisInstance = ServiceInstance.<Service> builder()
                .name(Provisioning.SERVICE_MAILBOX)
                .payload(new Service(server.getId()))
                .build();

        JsonInstanceSerializer<Service> serializer = new JsonInstanceSerializer<Service>(
                Service.class);

        serviceDiscovery = ServiceDiscoveryBuilder.builder(Service.class)
                .client(client)
                .basePath(SERVICE)
                .serializer(serializer)
                .thisInstance(thisInstance).build();
        ServiceCacheBuilder<Service> serviceCacheBuilder = serviceDiscovery
                .serviceCacheBuilder()
                .name(Provisioning.SERVICE_MAILBOX);
        serviceCache = serviceCacheBuilder.build();
    }

    public static CuratorManager getInstance() throws ServiceException {
        if (initialized == false) {
            return createInstance();
        }
        return instance;
    }

    private synchronized static CuratorManager createInstance() throws ServiceException {
        if (initialized == false) {
            String[] servers = Provisioning.getInstance().getLocalServer().getZookeeperClientServerList();
            if (servers.length > 0) {
                TreeSet<String> tset = new TreeSet<String>(); // TreeSet provides deduping and sorting.
                for (int i = 0; i < servers.length; ++i) {
                    tset.add(servers[i].toLowerCase());
                }
                StringBuilder serverList = new StringBuilder();
                for (String s : tset) {
                    if (serverList.length() > 0) {
                        serverList.append(",");
                    }
                    serverList.append(s);
                }
                try {
                    instance = new CuratorManager(serverList.toString());
                } catch (Exception e) {
                    throw ServiceException.FAILURE("Could not initialize curator", e);
                }
            }
            initialized = true;
        }
        return instance;
    }

    public InterProcessSemaphoreMutex createLock(String id) {
        return new InterProcessSemaphoreMutex(client, LOCKS + id);
    }

    public void setData(String key, String value) throws Exception {
        byte[] data;
        try {
            data = value.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            data = value.getBytes();
        }
        String path = DATA + key;
        if (client.checkExists().forPath(path) == null) {
            client.create().creatingParentsIfNeeded().forPath(path);
        }
        client.setData().forPath(DATA + key, data);
    }

    public String getData(String key) {
        try {
            byte[] data = client.getData().forPath(DATA + key);
            return new String(data, "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    public Set<String> getActiveServers() throws Exception {
        Set<String> activeServers = new HashSet<String>();
        Collection<ServiceInstance<Service>> instances = serviceCache.getInstances();
        for (ServiceInstance<Service> instance : instances ) {
            activeServers.add(instance.getPayload().getService());
        }
        return activeServers;
    }

    public void unregisterService(String serverId) throws Exception {
        Collection<ServiceInstance<Service>> instances = serviceCache.getInstances();
        for (ServiceInstance<Service> instance : instances ) {
            if (serverId.equals(instance.getPayload().getService())) {
                serviceDiscovery.unregisterService(instance);
                break;
            }
        }
    }

    public void registerLocalService() throws Exception {
        serviceDiscovery.registerService(thisInstance);
    }

    public synchronized void start() throws Exception {
        if (client != null) {
            client.start();
            serviceDiscovery.start();
            serviceCache.start();
        }
    }

    public synchronized void stop() {
        if (client != null) {
            Closeables.closeQuietly(serviceCache);
            Closeables.closeQuietly(serviceDiscovery);
            Closeables.closeQuietly(client);
        }
    }

}
