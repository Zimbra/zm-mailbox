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

package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class GrantInfo {

    /**
     * @zm-api-field-description Information on target
     */
    @XmlElement(name=AdminConstants.E_TARGET /* target */, required=true)
    private final TypeIdName target;

    /**
     * @zm-api-field-description Information on grantee
     */
    @XmlElement(name=AdminConstants.E_GRANTEE /* grantee */, required=true)
    private final GranteeInfo grantee;

    /**
     * @zm-api-field-description Information on right
     */
    @XmlElement(name=AdminConstants.E_RIGHT /* right */, required=true)
    private final RightModifierInfo right;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GrantInfo() {
        this((TypeIdName) null, (GranteeInfo) null, (RightModifierInfo) null);
    }

    public GrantInfo(TypeIdName target, GranteeInfo grantee,
                RightModifierInfo right) {
        this.target = target;
        this.grantee = grantee;
        this.right = right;
    }

    public TypeIdName getTarget() { return target; }
    public GranteeInfo getGrantee() { return grantee; }
    public RightModifierInfo getRight() { return right; }
}
