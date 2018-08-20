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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @author zimbra
 *
 */
/**
 * @zm-api-command-auth-required true
 * @zm-api-command-description Returns a list of HABGroup and its children
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = AccountConstants.E_GET_HAB_RESPONSE)
public class HABGroup {
    /**
     * @zm-api-field-description List of HabGroups under the root group
     */
    @XmlElement(name="name", required=false)
    private String name;
    
    /**
     * @zm-api-field-description List of HabGroups under the root group
     */
    @XmlElement(name="id", required=false)
    private String id;
    
    /**
     * @zm-api-field-description List of HabGroups under the root group
     */
    @XmlElement(name="habGroups", required=false)
    private List<HABGroup> childGroups = new ArrayList<HABGroup>();
    

    private Map<String, Object>attrs = new HashMap<String,Object>();
    
    /**
     * @zm-api-field-tag rootGroup indicator
     * @zm-api-field-description indicates whether a HAB group is the parent group in the Hierarchy
     */
    @XmlAttribute(name = AccountConstants.A_ROOT_HAB_GROUP /* forceDelete */, required = false)
    private ZmBoolean rootGroup;

    
    /**
     * @zm-api-field-tag parentGroupId
     * @zm-api-field-description indicates whether a HAB group is the parent group in the Hierarchy
     */
    @XmlAttribute(name = AccountConstants.A_PARENT_HAB_GROUP_ID /* forceDelete */, required = false)
    private String parentGroupId;
    
    
    public String getName() {
        return name;
    }

    
    public void setName(String name) {
        this.name = name;
    }

    
    public String getId() {
        return id;
    }

    
    public void setId(String id) {
        this.id = id;
    }

    
    public List<HABGroup> getChildGroups() {
        return childGroups;
    }

    
    public void setChildGroups(List<HABGroup> childGroups) {
        this.childGroups = childGroups;
    }

    public ZmBoolean getRootGroup() {
        if (rootGroup == null) {
            rootGroup = ZmBoolean.FALSE;
        }
        return rootGroup;
    }

    public void setRootGroup(ZmBoolean rootGroup) {
        this.rootGroup = rootGroup;
    }

    
    public String getParentGroupId() {
        return parentGroupId;
    }

    
    public void setParentGroupId(String parentGroupId) {
        this.parentGroupId = parentGroupId;
    }


    
    public Map<String, Object> getAttrs() {
        return attrs;
    }


    
    public void setAttrs(Map<String, Object> attrs) {
        this.attrs = attrs;
    }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("HABGroup [");
        if (name != null) {
            builder.append("name=");
            builder.append(name);
            builder.append(", ");
        }
        if (id != null) {
            builder.append("id=");
            builder.append(id);
            builder.append(", ");
        }
        if (childGroups != null) {
            builder.append("childGroups=");
            builder.append(childGroups);
            builder.append(", ");
        }
        if (rootGroup != null) {
            builder.append("rootGroup=");
            builder.append(rootGroup);
            builder.append(", ");
        }
        if (parentGroupId != null) {
            builder.append("parentGroupId=");
            builder.append(parentGroupId);
        }
        builder.append("]");
        return builder.toString();
    }

    
}
