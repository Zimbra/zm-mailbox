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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.AccountSelector;
import com.zimbra.soap.type.GranteeChooser;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Iterate through all folders of the owner's mailbox and return shares that match
 * grantees specified by the <b>&lt;grantee></b> specifier.
 * <br />
 * e.g.
 * <pre>
 *      &lt;GetShareInfoRequest>
 *          &lt;grantee name="dl@test.com"/>]
 *          &lt;owner by="name">user1@test.com&lt;/owner>
 *      &lt;/GetShareInfoRequest>
 * </pre>
 * returns all shares owned by user1@test.com that are shared with dl@test.com.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_SHARE_INFO_REQUEST)
public class GetShareInfoRequest {

    /**
     * @zm-api-field-description Grantee
     */
    @XmlElement(name=AdminConstants.E_GRANTEE, required=false)
    private final GranteeChooser grantee;

    /**
     * @zm-api-field-description Owner
     */
    @XmlElement(name=AdminConstants.E_OWNER, required=true)
    private final AccountSelector owner;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetShareInfoRequest() {
        this((GranteeChooser) null, (AccountSelector) null);
    }

    public GetShareInfoRequest(AccountSelector owner) {
        this(null, owner);
    }

    public GetShareInfoRequest(GranteeChooser grantee, AccountSelector owner) {
        this.grantee = grantee;
        this.owner = owner;
    }

    public GranteeChooser getGrantee() { return grantee; }
    public AccountSelector getOwner() { return owner; }
}
