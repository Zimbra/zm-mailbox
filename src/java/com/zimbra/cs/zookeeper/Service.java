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

public class Service {
    private final ServiceDiscovery serviceDiscovery;
    private final ServiceMetadata metadata;
    private final String serviceId;

    public Service(String serviceId, ServiceMetadata metadata,
            ServiceDiscovery serviceDiscovery) {
        this.serviceId = serviceId;
        this.serviceDiscovery = serviceDiscovery;
        this.metadata = metadata;
    }

    public void start() throws Exception {
        serviceDiscovery.register(serviceId, metadata);
    }

    public void stop() {
        serviceDiscovery.deregister(serviceId);
    }

    public ServiceDiscovery getServiceDiscovery() {
        return serviceDiscovery;
    }

    public ServiceMetadata getMetadata() {
        return metadata;
    }

    public String getServiceId() {
        return serviceId;
    }

}
