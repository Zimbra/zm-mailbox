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

package com.zimbra.soap.admin.message;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.DistributionListMembershipInfo;

/**
 * Response which provides a list of DLs that a particular DL is a member of
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_DISTRIBUTION_LIST_MEMBERSHIP_RESPONSE)
public class GetDistributionListMembershipResponse {

    /**
     * @zm-api-field-description Information about distribution lists
     */
    @XmlElement(name=AdminConstants.E_DL, required=false)
    private List <DistributionListMembershipInfo> dls = Lists.newArrayList();

    public GetDistributionListMembershipResponse() {
        this((Iterable<DistributionListMembershipInfo>) null);
    }

    public GetDistributionListMembershipResponse(
            Iterable<DistributionListMembershipInfo> dls) {
        setDls(dls);
    }

    public GetDistributionListMembershipResponse setDls(
            Iterable<DistributionListMembershipInfo> dls) {
        this.dls.clear();
        if (dls != null) {
            Iterables.addAll(this.dls,dls);
        }
        return this;
    }

    public GetDistributionListMembershipResponse addDl(
            DistributionListMembershipInfo dl) {
        dls.add(dl);
        return this;
    }

    public List<DistributionListMembershipInfo> getDls() {
        return Collections.unmodifiableList(dls);
    }
}
