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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.HABGroupOperation;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Modify Hierarchical address book group parent
 *                             <br />
 *                             <b>Access</b>: domain admin sufficient
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = AdminConstants.E_MODIFY_HAB_GROUP_REQUEST)
public class ModifyHABGroupRequest {

    /**
     * @zm-api-field-description Tokens
     */
    @XmlElement(name = AdminConstants.E_HAB_GROUP_OPERATION /* token */, required = true)
    private HABGroupOperation operation;

    public ModifyHABGroupRequest() {

    }

    public ModifyHABGroupRequest(HABGroupOperation operation) {
        this.operation = operation;
    }

    
    public HABGroupOperation getOperation() {
        return operation;
    }

    
    public void setOperation(HABGroupOperation operation) {
        this.operation = operation;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ModifyHABGroupRequest [");
        if (operation != null) {
            builder.append("operation=");
            builder.append(operation);
        }
        builder.append("]");
        return builder.toString();
    }
    
    

}
