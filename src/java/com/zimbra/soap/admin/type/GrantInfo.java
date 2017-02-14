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
