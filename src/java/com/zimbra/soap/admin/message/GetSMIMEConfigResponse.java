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

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.SMIMEConfigInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_SMIME_CONFIG_RESPONSE)
@XmlType(propOrder = {})
public class GetSMIMEConfigResponse {

    /**
     * @zm-api-field-description SMIME configuration information
     */
    @XmlElement(name=AdminConstants.E_CONFIG /* config */, required=false)
    private List<SMIMEConfigInfo> configs = Lists.newArrayList();

    public GetSMIMEConfigResponse() {
    }

    public void setConfigs(Iterable <SMIMEConfigInfo> configs) {
        this.configs.clear();
        if (configs != null) {
            Iterables.addAll(this.configs,configs);
        }
    }

    public void addConfig(SMIMEConfigInfo config) {
        this.configs.add(config);
    }

    public List<SMIMEConfigInfo> getConfigs() {
        return Collections.unmodifiableList(configs);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("configs", configs);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
