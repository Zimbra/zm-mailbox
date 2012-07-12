/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.VoiceAdminConstants;
import com.zimbra.soap.admin.type.VoiceProviderInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=VoiceAdminConstants.E_GET_ALL_UC_PROVIDERS_RESPONSE)
public class GetAllUCProvidersResponse {

    /**
     * @zm-api-field-description Details for a UC provider
     */
    @XmlElement(name=VoiceAdminConstants.E_PROVIDER, required=false)
    private List<VoiceProviderInfo> providers = Lists.newArrayList();

    public GetAllUCProvidersResponse() {
    }

    public void setProviders(Iterable <VoiceProviderInfo> providers) {
        this.providers.clear();
        if (providers == null) {
            this.providers = null;
        } else {
            this.providers = Lists.newArrayList();
            Iterables.addAll(this.providers, providers);
        }
    }

    public void addProvider(VoiceProviderInfo provider) {
        this.providers.add(provider);
    }

    public List<VoiceProviderInfo> getProviders() {
        return providers;
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("providers", providers);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
