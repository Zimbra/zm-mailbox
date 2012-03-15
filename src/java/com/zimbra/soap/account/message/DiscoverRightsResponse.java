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
package com.zimbra.soap.account.message;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Lists;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.account.type.DiscoverRightsInfo;

@XmlRootElement(name=AccountConstants.E_DISCOVER_RIGHTS_RESPONSE)
public class DiscoverRightsResponse {

    /**
     * @zm-api-field-description Information about targets for rights
     */
    @XmlElement(name=AccountConstants.E_TARGETS, required=false)
    private List<DiscoverRightsInfo> discoveredRights = Lists.newArrayList();

    public DiscoverRightsResponse() {
        this(null);
    }

    public DiscoverRightsResponse(Iterable<DiscoverRightsInfo> targets) {
        if (targets != null) {
            setDiscoveredRights(targets);
        }
    }

    public void setDiscoveredRights(Iterable<DiscoverRightsInfo> discoveredRights) {
        this.discoveredRights = Lists.newArrayList(discoveredRights);
    }

    public void addDiscoveredRight(DiscoverRightsInfo discoveredRight) {
        this.discoveredRights.add(discoveredRight);
    }

    public List<DiscoverRightsInfo> getDiscoveredRights() {
        return discoveredRights;
    }
}
