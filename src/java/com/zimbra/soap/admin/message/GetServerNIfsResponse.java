/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

package com.zimbra.soap.admin.message;

import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.NetworkInformation;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_SERVER_NIFS_RESPONSE)
public class GetServerNIfsResponse {

    /**
     * @zm-api-field-description Network interface information
     */
    @XmlElement(name=AdminConstants.E_NI, required=false)
    private List <NetworkInformation> networkInterfaces = Lists.newArrayList();

    public GetServerNIfsResponse() {
    }

    public GetServerNIfsResponse setNetworkInterfaces(
            Collection <NetworkInformation> networkInterfaces) {
        this.networkInterfaces.clear();
        if (networkInterfaces != null) {
            this.networkInterfaces.addAll(networkInterfaces);
        }
        return this;
    }

    public GetServerNIfsResponse addNetworkInterface(
            NetworkInformation networkInterface) {
        networkInterfaces.add(networkInterface);
        return this;
    }

    public List<NetworkInformation> getNetworkInterfaces() {
        return Collections.unmodifiableList(networkInterfaces);
    }
}
