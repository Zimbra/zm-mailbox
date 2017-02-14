/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
