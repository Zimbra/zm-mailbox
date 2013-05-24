/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zookeeper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.helix.HelixDataAccessor;
import org.apache.helix.HelixException;
import org.apache.helix.HelixManager;
import org.apache.helix.HelixManagerFactory;
import org.apache.helix.InstanceType;
import org.apache.helix.LiveInstanceChangeListener;
import org.apache.helix.LiveInstanceInfoProvider;
import org.apache.helix.NotificationContext;
import org.apache.helix.PropertyKey;
import org.apache.helix.PropertyKey.Builder;
import org.apache.helix.ZNRecord;
import org.apache.helix.manager.zk.ZKHelixManager;
import org.apache.helix.model.HelixConfigScope;
import org.apache.helix.model.HelixConfigScope.ConfigScopeProperty;
import org.apache.helix.model.LiveInstance;
import org.apache.helix.model.builder.HelixConfigScopeBuilder;
import org.apache.helix.store.zk.ZkHelixPropertyStore;
import org.apache.helix.tools.ClusterSetup;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;

public class ServiceDiscovery {
    private final String zkAddress;
    private final String cluster;
    private HelixManager admin;
    List<ServiceMetadata> cache;
    Map<String, HelixManager> serviceMap;
    private static ServiceDiscovery instance = null;
    private static final String CLUSTERNAME = "AlwaysOn";

    private ServiceDiscovery(String zkAddress, String cluster) {
        this.zkAddress = zkAddress;
        this.cluster = cluster;
        serviceMap = new HashMap<String, HelixManager>();
        cache = Collections.emptyList();
    }

    public synchronized static ServiceDiscovery getInstance() throws ServiceException {
        if (instance == null) {
            String[] servers = Provisioning.getInstance().getLocalServer().getZookeeperClientServerList();
            if (servers.length == 0) {
                return null;
            }
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
            instance = new ServiceDiscovery(serverList.toString(), CLUSTERNAME);
        }
        return instance;
    }

    public synchronized boolean start() throws Exception {
        if (admin != null) {
            return true;
        }
        // auto create cluster and allow nodes to automatically join the cluster
        ClusterSetup setupTool = new ClusterSetup(zkAddress);
        try {
            setupTool.addCluster(cluster, false);
        } catch (HelixException e) {
            // ignore if cluster is already exists
        }
        admin = HelixManagerFactory.getZKHelixManager(cluster,
                "service-discovery", InstanceType.ADMINISTRATOR, zkAddress);
        admin.connect();
        // admin.getClusterManagmentTool().addCluster(cluster, false);
        HelixConfigScope scope = new HelixConfigScopeBuilder(
                ConfigScopeProperty.CLUSTER).forCluster(cluster).build();

        Map<String, String> properties = new HashMap<String, String>();
        properties.put(ZKHelixManager.ALLOW_PARTICIPANT_AUTO_JOIN,
                String.valueOf(true));
        admin.getClusterManagmentTool().setConfig(scope, properties);
        return true;
    }

    public synchronized boolean stop() {
        if (admin != null && admin.isConnected()) {
            admin.disconnect();
        }
        return true;
    }

    public synchronized ZkHelixPropertyStore<ZNRecord> getPropertyStore()
            throws Exception {
        if (admin == null) {
            start();
        }
        return admin.getHelixPropertyStore();
    }

    public synchronized void setupWatcher() throws Exception {

        LiveInstanceChangeListener listener = new LiveInstanceChangeListener() {
            @Override
            public void onLiveInstanceChange(List<LiveInstance> liveInstances,
                    NotificationContext changeContext) {
                if (changeContext.getType() != NotificationContext.Type.FINALIZE) {
                    refreshCache();
                }
            }
        };
        if (admin == null) {
            start();
        }
        admin.addLiveInstanceChangeListener(listener);
    }

    private void refreshCache() {
        Builder propertyKeyBuilder = new PropertyKey.Builder(cluster);
        HelixDataAccessor helixDataAccessor = admin.getHelixDataAccessor();
        List<LiveInstance> liveInstances = helixDataAccessor
                .getChildValues(propertyKeyBuilder.liveInstances());
        refreshCache(liveInstances);
    }

    private void refreshCache(List<LiveInstance> liveInstances) {
        List<ServiceMetadata> services = new ArrayList<ServiceMetadata>();
        for (LiveInstance liveInstance : liveInstances) {
            ServiceMetadata metadata = new ServiceMetadata();
            ZNRecord rec = liveInstance.getRecord();
            metadata.setServer(rec.getSimpleField("SERVER"));
            metadata.setServiceName(rec.getSimpleField("SERVICE_NAME"));
            services.add(metadata);
        }
        // protect against multiple threads updating this
        synchronized (this) {
            cache = services;
        }
    }

    public Set<String> getActiveServers() {
        Set<String> servers = new HashSet<String>();
        for (ServiceMetadata meta : cache) {
            servers.add(meta.getServer());
        }
        return servers;
    }

    public void deregister(final String serviceId) {
        HelixManager helixManager = serviceMap.get(serviceId);
        if (helixManager != null && helixManager.isConnected()) {
            helixManager.disconnect();
        }
    }

    public boolean register(final String serviceId,
            final ServiceMetadata serviceMetadata) throws Exception {
        HelixManager helixManager = HelixManagerFactory.getZKHelixManager(
                cluster, serviceId, InstanceType.PARTICIPANT, zkAddress);
        LiveInstanceInfoProvider liveInstanceInfoProvider = new LiveInstanceInfoProvider() {
            @Override
            public ZNRecord getAdditionalLiveInstanceInfo() {
                // serialize serviceMetadata to ZNRecord
                ZNRecord rec = new ZNRecord(serviceId);
                rec.setSimpleField("SERVER", serviceMetadata.getServer());
                rec.setSimpleField("SERVICE_NAME",
                        serviceMetadata.getServiceName());
                return rec;
            }
        };
        helixManager.setLiveInstanceInfoProvider(liveInstanceInfoProvider);
        helixManager.connect();
        serviceMap.put(serviceId, helixManager);
        return true;
    }
}
