/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018, 2019 Synacor, Inc.
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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.base.MoreObjects;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.DomainSelector;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @author zimbra
 *
 */
/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Create/Rename/Delete Hab Organizational Unit
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = AdminConstants.E_HAB_ORG_UNIT_REQUEST)
@XmlType(propOrder = {})
public class HABOrgUnitRequest {

    @XmlEnum
    public enum HabOp {
        // case must match
        create, rename, delete, list;

        public static HabOp fromString(String s) throws ServiceException {
            try {
                return HabOp.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }

    
    /**
     * @zm-api-field-description Specifies the domain under which Hab org unit
     *                           is created
     */
    @XmlElement(name = AdminConstants.E_DOMAIN, required = true)
    private DomainSelector domainSelector;

    /**
     * @zm-api-field-tag name
     * @zm-api-field-description HAB org unit name
     */
    @XmlAttribute(name = AdminConstants.A_NAME /* name */, required = false)
    private String name;

    /**
     * @zm-api-field-tag op
     * @zm-api-field-description operation
     */
    @XmlAttribute(name = AdminConstants.A_OPERATION /* op */, required = true)
    private HabOp op;

    /**
     * @zm-api-field-tag newName
     * @zm-api-field-description new HAB org unit name
     */
    @XmlAttribute(name = AdminConstants.A_NEW_NAME /* newName */, required = false)
    private String newName;
    /**
     * @zm-api-field-tag forceDelete
     * @zm-api-field-description indicates whether a HAB org unit should be
     *                           deleted when it has groups.
     */
    @XmlAttribute(name = AdminConstants.A_FORCE_DELETE /* forceDelete */, required = false)
    private ZmBoolean forceDelete;
   
   public HABOrgUnitRequest() {
       
   }

    public HABOrgUnitRequest(DomainSelector domainSelector, HabOp op) {
        this.domainSelector = domainSelector;
        this.op = op;
    }
   
    public HABOrgUnitRequest(DomainSelector domainSelector, String habOrgUnitName, 
        String newHabOrgUnitName, HabOp op) {
        this.domainSelector = domainSelector;
        this.name = habOrgUnitName;
        this.newName = newHabOrgUnitName;
        this.op = op;
    }

    public HABOrgUnitRequest(DomainSelector domainSelector, String habOrgUnitName, HabOp op) {
        this.domainSelector = domainSelector;
        this.name = habOrgUnitName;
        this.op = op;
    }

    
    public DomainSelector getDomainSelector() {
        return domainSelector;
    }

    
    public void setDomainSelector(DomainSelector domainSelector) {
        this.domainSelector = domainSelector;
    }

    
    public String getName() {
        return name;
    }

    
    public void setName(String name) {
        this.name = name;
    }

    public HabOp getOp() {
        return op;
    }

    
    public void setOp(HabOp op) {
        this.op = op;
    }

    public String getNewName() {
        return newName;
    }

    
    public void setNewName(String newName) {
        this.newName = newName;
    }


    
    public ZmBoolean getForceDelete() {
        return forceDelete;
    }

    
    public void setForceDelete(ZmBoolean forceDelete) {
        this.forceDelete = forceDelete;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("HabOrgUnitRequest [");
        if (domainSelector != null) {
            builder.append("domainSelector=");
            builder.append(domainSelector);
            builder.append(", ");
        }
        if (name != null) {
            builder.append("name=");
            builder.append(name);
            builder.append(", ");
        }
        if (op != null) {
            builder.append("op=");
            builder.append(op);
            builder.append(", ");
        }
        if (newName != null) {
            builder.append("newName=");
            builder.append(newName);
        }
        builder.append("]");
        return builder.toString();
    }
    
    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper.add("op", op.toString()).add("name", name)
            .add("domain", domainSelector.getKey()).add("newName", newName);
    }

}
