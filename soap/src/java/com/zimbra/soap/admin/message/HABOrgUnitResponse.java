/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;

/**
 * @author zimbra
 *
 */
/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description response for HabOrgUnit creation/rename/deletion response
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_HAB_ORG_UNIT_RESPONSE)
@XmlType(propOrder = {})
public class HABOrgUnitResponse {

    /**
     * @zm-api-field-description List of HabOrg units under a domain
     */
    @XmlElement(name=AdminConstants.E_HAB_ORG_UNIT_NAME, required=false)
    private List<String> habOrgList = new ArrayList<String>();
    
    public HABOrgUnitResponse() {
        
    }

    public HABOrgUnitResponse(List<String> habOrgList) {
        this.habOrgList = habOrgList;
    }
    
    public List<String> getHabOrgList() {
        return habOrgList;
    }

    
    public void setHabOrgList(List<String> habOrgList) {
        this.habOrgList = habOrgList;
    }
    
    
}

