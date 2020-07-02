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
package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;

/**
 * @author zimbra
 *
 */
/**
 * @zm-api-command-auth-required true
 * @zm-api-command-description Get the groups in a HAB org unit.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = AccountConstants.E_GET_HAB_REQUEST)
@XmlType(propOrder = {})
public class GetHABRequest {
    
    /**
     * @zm-api-field-tag name
     * @zm-api-field-description HAB root group zimbra Id
     */
    @XmlAttribute(name = AccountConstants.A_HAB_ROOT_GROUP_ID /* habRootGroupId */, required = true)
    private String habRootGroupId;
    
    public GetHABRequest() {
        
    }
    
    public GetHABRequest(String rootGrpId) {
        this.habRootGroupId = rootGrpId;
    }

    
    public String getHabRootGroupId() {
        return habRootGroupId;
    }

    
    public void setHabRootGroupId(String habRootGroupId) {
        this.habRootGroupId = habRootGroupId;
    }

}