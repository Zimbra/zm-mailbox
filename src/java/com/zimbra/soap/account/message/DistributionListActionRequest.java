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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.account.type.AttrsImpl;
import com.zimbra.soap.account.type.DistributionListAction;
import com.zimbra.soap.type.DistributionListSelector;

/**
 * @zm-api-command-description Perform an action on a Distribution List
 * <br />
 * Notes:
 * <ul>
 * <li> Authorized account must be one of the list owners
 * <li> For owners/rights, only grants on the group itself will be modified, grants on domain and globalgrant (from
 *      which the right can be inherited) will not be touched.  Only admins can modify grants on domains and
 *      globalgrant, owners of groups can only modify grants on the group entry.
 * </ul>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_DISTRIBUTION_LIST_ACTION_REQUEST)
public class DistributionListActionRequest extends AttrsImpl {

    /**
     * @zm-api-field-description Identifies the distribution list to act upon
     */
    @XmlElement(name=AccountConstants.E_DL, required=true)
    private DistributionListSelector dl;

    /**
     * @zm-api-field-description Specifies the action to perform
     */
    @XmlElement(name=AccountConstants.E_ACTION, required=true)
    private final DistributionListAction action;

    public DistributionListActionRequest() {
        this((DistributionListSelector) null, (DistributionListAction) null);
    }

    public DistributionListActionRequest(DistributionListSelector dl, 
            DistributionListAction action) {
        this.setDl(dl);
        this.action = action;
    }

    public void setDl(DistributionListSelector dl) { this.dl = dl; }

    public DistributionListSelector getDl() { return dl; }
    public DistributionListAction getAction() { return action; }

}
