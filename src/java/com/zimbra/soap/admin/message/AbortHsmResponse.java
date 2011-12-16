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

package com.zimbra.soap.admin.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.HsmConstants;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-response-description The aborted attribute is set to true (1) if an HSM session was running at the time the
 * request was made, false (0) otherwise.<p>Note: If the abort request is sent after all the blobs for the last mailbox
 * have been moved, but before its database table has been updated, &lt;AbortHsmRequest> will return true even though
 * the process was not really aborted.  This state is very unlikely.  The official aborted state can be verified
 * with &lt;GetHsmStatusRequest>.</p>
 * @zm-api-command-network-edition true
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=HsmConstants.E_ABORT_HSM_RESPONSE)
public class AbortHsmResponse {

    /**
     * @zm-api-field-description Set to true (1) if an HSM session was running at the time the request was made.
     */
    @XmlAttribute(name=HsmConstants.A_ABORTED /* aborted */, required=true)
    private final ZmBoolean aborted;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AbortHsmResponse() {
        this(false);
    }

    public AbortHsmResponse(boolean aborted) {
        this.aborted = ZmBoolean.fromBool(aborted);
    }

    public boolean getAborted() { return ZmBoolean.toBool(aborted); }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("aborted", aborted);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
