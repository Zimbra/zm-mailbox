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

import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.GranteeInfo;
import com.zimbra.soap.admin.type.EffectiveRightsTarget;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_ALL_EFFECTIVE_RIGHTS_RESPONSE)
@XmlType(propOrder = {"grantee", "targets"})
public class GetAllEffectiveRightsResponse {

    /**
     * @zm-api-field-description Grantee information
     */
    @XmlElement(name=AdminConstants.E_GRANTEE, required=false)
    private final GranteeInfo grantee;

    /**
     * @zm-api-field-description Effective rights targets
     */
    @XmlElement(name=AdminConstants.E_TARGET, required=false)
    private List <EffectiveRightsTarget> targets = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetAllEffectiveRightsResponse() {
        this((GranteeInfo) null);
    }

    public GetAllEffectiveRightsResponse(GranteeInfo grantee) {
        this.grantee = grantee;
    }

    public GetAllEffectiveRightsResponse setTargets(
            Collection <EffectiveRightsTarget> targets) {
        this.targets.clear();
        if (targets != null) {
            this.targets.addAll(targets);
        }
        return this;
    }

    public GetAllEffectiveRightsResponse addTarget(
            EffectiveRightsTarget target) {
        targets.add(target);
        return this;
    }

    public List <EffectiveRightsTarget> getTargets() {
        return Collections.unmodifiableList(targets);
    }

    public GranteeInfo getGrantee() { return grantee; }
}
