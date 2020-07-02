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
import com.zimbra.soap.account.type.Signature;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Create a signature.
 * <p>
 * If an id is provided it will be honored as the id for the signature.
 * </p><p>
 * CreateSignature will set account default signature to the signature being created if there is currently no
 * default signature for the account.
 * </p><p>
 * There can be at most one text/plain signatue and one text/html signature.
 *</p>
 * <b>{contact-id}</b> contact id associated with this signature
 *
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_CREATE_SIGNATURE_REQUEST)
public class CreateSignatureRequest {

    /**
     * @zm-api-field-description Details of the signature to be created
     */
    @XmlElement(name=AccountConstants.E_SIGNATURE, required=true)
    private final Signature signature;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CreateSignatureRequest() {
        this((Signature) null);
    }

    public CreateSignatureRequest(Signature signature) {
        this.signature = signature;
    }

    public Signature getSignature() { return signature; }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("signature", signature)
            .toString();
    }
}
