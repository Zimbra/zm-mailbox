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
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {})
public class RightViaInfo {

    /**
     * @zm-api-field-description Target
     */
    @XmlElement(name=AdminConstants.E_TARGET /* target */, required=true)
    private final TargetWithType target;

    /**
     * @zm-api-field-description Grantee
     */
    @XmlElement(name=AdminConstants.E_GRANTEE /* grantee */, required=true)
    private final GranteeWithType grantee;

    /**
     * @zm-api-field-description Checked right
     */
    @XmlElement(name=AdminConstants.E_RIGHT /* right */, required=true)
    private final CheckedRight right;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private RightViaInfo() {
        this((TargetWithType) null, (GranteeWithType) null,
                (CheckedRight) null);
    }

    public RightViaInfo(TargetWithType target, GranteeWithType grantee,
                CheckedRight right) {
        this.target = target;
        this.grantee = grantee;
        this.right = right;
    }

    public TargetWithType getTarget() { return target; }
    public GranteeWithType getGrantee() { return grantee; }
    public CheckedRight getRight() { return right; }
}
