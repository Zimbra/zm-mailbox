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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.EffectiveRightsTargetSelector;
import com.zimbra.soap.admin.type.GranteeSelector;

/**
 * @zm-api-command-description Returns all grants on the specified target entry, or all grants granted to the
 * specified grantee entry.
 * <br />
 * The authenticated admin must have an effective "viewGrants" (TBD) system right on the specified target/grantee.
 * <br />
 * At least one of <b>&lt;target></b> or <b>&lt;grantee></b> must be specified.  If both <b>&lt;target></b> and
 * <b>&lt;grantee></b> are specified, only grants that are granted on the target to the grantee are returned.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_GRANTS_REQUEST)
public class GetGrantsRequest {

    /**
     * @zm-api-field-description Target
     */
    @XmlElement(name=AdminConstants.E_TARGET, required=false)
    private final EffectiveRightsTargetSelector target;

    /**
     * @zm-api-field-description Grantee
     */
    @XmlElement(name=AdminConstants.E_GRANTEE, required=false)
    private final GranteeSelector grantee;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetGrantsRequest() {
        this((EffectiveRightsTargetSelector) null, (GranteeSelector) null);
    }

    public GetGrantsRequest(EffectiveRightsTargetSelector target,
                GranteeSelector grantee) {
        this.target = target;
        this.grantee = grantee;
    }

    public EffectiveRightsTargetSelector getTarget() { return target; }
    public GranteeSelector getGrantee() { return grantee; }
}
