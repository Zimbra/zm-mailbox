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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.EffectiveRightsTargetInfo;
import com.zimbra.soap.admin.type.GranteeInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_EFFECTIVE_RIGHTS_RESPONSE)
@XmlType(propOrder = {})
public class GetEffectiveRightsResponse {

    /**
     * @zm-api-field-description Information about grantee
     */
    @XmlElement(name=AdminConstants.E_GRANTEE, required=true)
    private final GranteeInfo grantee;

    /**
     * @zm-api-field-description Information about target
     */
    @XmlElement(name=AdminConstants.E_TARGET, required=true)
    private final EffectiveRightsTargetInfo target;


    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetEffectiveRightsResponse() {
        this((GranteeInfo) null, (EffectiveRightsTargetInfo) null);
    }

    public GetEffectiveRightsResponse(GranteeInfo grantee, EffectiveRightsTargetInfo target) {
        this.grantee = grantee;
        this.target = target;
    }

    public GranteeInfo getGrantee() { return grantee; }
    public EffectiveRightsTargetInfo getTarget() { return target; }
}
