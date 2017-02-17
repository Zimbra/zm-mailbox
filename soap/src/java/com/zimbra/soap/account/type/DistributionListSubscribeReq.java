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
package com.zimbra.soap.account.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_DL_SUBS_REQ)
public class DistributionListSubscribeReq {

    /**
     * @zm-api-field-description Operation
     */
    @XmlAttribute(name=AccountConstants.A_OP /* op */, required=true)
    private DistributionListSubscribeOp op;

    /**
     * @zm-api-field-description Flag whether to bcc all other owners on the accept/reject notification emails.
     * <table>
     * <tr> <td> <b>1 (true)</b> [default] </td> <td> bcc all other owners </td> </tr>
     * <tr> <td> <b>0 (false)</b> </td> <td> do not bcc any other owners </td> </tr>
     * </table>
     */
    @XmlAttribute(name=AccountConstants.A_BCC_OWNERS /* bccOwners */, required=false)
    private ZmBoolean bccOwners = ZmBoolean.ONE /* true */;

    /**
     * @zm-api-field-tag member-email
     * @zm-api-field-description Member email
     */
    @XmlValue
    private String memberEmail;

    public DistributionListSubscribeReq() {
        this((DistributionListSubscribeOp) null, (String) null);
    }

    public DistributionListSubscribeReq(DistributionListSubscribeOp op, String memberEmail) {
        setOp(op);
        setMemberEmail(memberEmail);
    }

    public void setOp(DistributionListSubscribeOp op) {
        this.op = op;
    }

    public DistributionListSubscribeOp getOp() {
        return op;
    }

    public void setBccOwners(boolean bccOwners) {
        this.bccOwners = ZmBoolean.fromBool(bccOwners);
    }

    public boolean getBccOwners() {
        return ZmBoolean.toBool(bccOwners);
    }

    public void setMemberEmail(String memberEmail) {
        this.memberEmail = memberEmail;
    }

    public String getMemberEmail() {
        return memberEmail;
    }

}
