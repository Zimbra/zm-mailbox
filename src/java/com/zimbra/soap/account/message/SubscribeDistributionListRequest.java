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
import com.zimbra.soap.account.type.DistributionListSubscribeOp;
import com.zimbra.soap.type.DistributionListSelector;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Subscribe to or unsubscribe from a distribution list
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_SUBSCRIBE_DISTRIBUTION_LIST_REQUEST)
public class SubscribeDistributionListRequest {

    /**
     * @zm-api-field-description The operation to perform.
     * <br />
     * <ul>
     * <li> <b>subscribe</b>  : Subscribe the requested account to the distribution list
     * <li> <b>unsubscribe</b>: Unsubscribe the requested account from the distribution list
     * </ul>
     */
    @XmlAttribute(name=AccountConstants.A_OP, required=true)
    private DistributionListSubscribeOp op;

    /**
     * @zm-api-field-description Selector for the distribution list
     */
    @XmlElement(name=AccountConstants.E_DL, required=true)
    private DistributionListSelector dl;

    public SubscribeDistributionListRequest() {
        this((DistributionListSelector) null, (DistributionListSubscribeOp) null);
    }

    public SubscribeDistributionListRequest(DistributionListSelector dl, DistributionListSubscribeOp op) {
        this.setDl(dl);
        this.setOp(op);
    }

    public void setOp(DistributionListSubscribeOp op) { this.op = op; }
    public DistributionListSubscribeOp getOp() { return op; }

    public void setDl(DistributionListSelector dl) { this.dl = dl; }
    public DistributionListSelector getDl() { return dl; }
}
