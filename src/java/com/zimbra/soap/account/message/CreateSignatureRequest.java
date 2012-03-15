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

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.account.type.Signature;

/**
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
        return Objects.toStringHelper(this)
            .add("signature", signature)
            .toString();
    }
}
