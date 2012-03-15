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
