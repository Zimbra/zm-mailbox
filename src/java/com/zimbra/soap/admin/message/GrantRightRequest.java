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
import com.zimbra.soap.admin.type.RightModifierInfo;

/**
 * @zm-api-command-description Grant a right on a target to an individual or group grantee.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GRANT_RIGHT_REQUEST)
public class GrantRightRequest {

    /**
     * @zm-api-field-description Target selector
     */
    @XmlElement(name=AdminConstants.E_TARGET, required=true)
    private final EffectiveRightsTargetSelector target;

    /**
     * @zm-api-field-description Grantee selector
     */
    @XmlElement(name=AdminConstants.E_GRANTEE, required=true)
    private final GranteeSelector grantee;

    /**
     * @zm-api-field-description Right
     */
    @XmlElement(name=AdminConstants.E_RIGHT, required=true)
    private final RightModifierInfo right;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GrantRightRequest() {
        this((EffectiveRightsTargetSelector) null, (GranteeSelector) null,
                (RightModifierInfo) null);
    }

    public GrantRightRequest(EffectiveRightsTargetSelector target,
                GranteeSelector grantee, RightModifierInfo right) {
        this.target = target;
        this.grantee = grantee;
        this.right = right;
    }

    public EffectiveRightsTargetSelector getTarget() { return target; }
    public GranteeSelector getGrantee() { return grantee; }
    public RightModifierInfo getRight() { return right; }
}
