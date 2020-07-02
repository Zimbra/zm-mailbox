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

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.account.type.NameId;
import com.zimbra.soap.json.jackson.annotate.ZimbraUniqueElement;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Delete an Identity
 * <p>
 * must specify either <b>{name}</b> or <b>{id}</b> attribute to <b>&lt;identity></b>
 * </p>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_DELETE_IDENTITY_REQUEST)
public class DeleteIdentityRequest {

    /**
     * @zm-api-field-description Details of the identity to delete.
     */
    @ZimbraUniqueElement
    @XmlElement(name=AccountConstants.E_IDENTITY /* identity */, required=true)
    private final NameId identity;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private DeleteIdentityRequest() {
        this((NameId) null);
    }

    public DeleteIdentityRequest(NameId identity) {
        this.identity = identity;
    }

    public NameId getIdentity() { return identity; }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("identity", identity)
            .toString();
    }
}
