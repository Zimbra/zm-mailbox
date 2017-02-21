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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.account.type.AttrsImpl;
import com.zimbra.soap.type.DistributionListSelector;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Get a distribution list, optionally with ownership information an granted rights.
 * <br />
 * Notes:
 * <ul>
 * <li> If the authed account is one of the list owners, all (requested) attributes of the DL are returned in the
 *      response.  Otherwise only attributes visible and useful to non-owners are returned.
 * <li> Specified &lt;rights> are returned only if the authed account is one of the list owners.
 * <li> Only grants on this group entry are returned, inherited grants on domain or globalgrant are not returned.
 * </ul>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_GET_DISTRIBUTION_LIST_REQUEST)
public class GetDistributionListRequest extends AttrsImpl {

    /**
     * @zm-api-field-description Whether to return owners, default is 0 (i.e. Don't return owners)
     */
    @XmlAttribute(name=AccountConstants.A_NEED_OWNERS /* needOwners */, required=false)
    private ZmBoolean needOwners;

    /**
     * @zm-api-field-description return grants for the specified (comma-seperated) rights.
     * <br />
     * e.g. needRights="sendToDistList,viewDistList"
     */
    @XmlAttribute(name=AccountConstants.A_NEED_RIGHTS /* needRights */, required=false)
    private String needRights;

    /**
     * @zm-api-field-description Specify the distribution list
     */
    @XmlElement(name=AccountConstants.E_DL, required=true)
    private DistributionListSelector dl;

    public GetDistributionListRequest() {
        this((DistributionListSelector) null, (Boolean) null, (String) null);
    }

    public GetDistributionListRequest(DistributionListSelector dl, Boolean needOwners) {
        this(dl, needOwners, null);
    }

    public GetDistributionListRequest(DistributionListSelector dl, Boolean needOwners, 
            String needRights) {
        this.setDl(dl);
        this.setNeedOwners(needOwners);
        this.setNeedRights(needRights);
    }

    public void setNeedOwners(Boolean needOwners) { 
        this.needOwners = ZmBoolean.fromBool(needOwners); 
    }

    public void setNeedRights(String needRights) { 
        this.needRights = needRights;
    }

    public Boolean getNeedOwners() { return ZmBoolean.toBool(needOwners); }

    public String getNeedRights() { return needRights; }

    public void setDl(DistributionListSelector dl) { this.dl = dl; }
    public DistributionListSelector getDl() { return dl; }

}
