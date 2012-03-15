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
 * @zm-api-command-description Change attributes of the given signature.
 * Only the attributes specified in the request are modified.
 * <br />
 * Note: The Server identifies the signature by <b>id</b>, if the <b>name</b> attribute is present and is
 * different from the current name of the signature, the signature will be renamed.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_MODIFY_SIGNATURE_REQUEST)
public class ModifySignatureRequest {

    /**
     * @zm-api-field-description Specifies the changes to the signature
     */
    @XmlElement(name=AccountConstants.E_SIGNATURE, required=true)
    private final Signature signature;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ModifySignatureRequest() {
        this((Signature) null);
    }

    public ModifySignatureRequest(Signature signature) {
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
