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
package com.zimbra.soap.account.type;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.collect.Lists;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraKeyValuePairs;
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
public class HABGroup extends HABMember {

    /**
     * @zm-api-field-description id of the HAB group
     */
    @XmlElement(name=AccountConstants.A_ID, required=false)
    private String id;

    /**
     * @zm-api-field-description List of HabGroups under this HAB group
     */
    @XmlElement(name=AccountConstants.A_HAB_GROUPS, required=false)
    private List<HABGroup> childGroups = new ArrayList<HABGroup>();

    /**
     * @zm-api-field-description Attributes of the HAB group
     */
    @ZimbraKeyValuePairs
    @XmlElement(name=AccountConstants.E_ATTR /* attr */, required=false)
    private List<Attr> attrs = Lists.newArrayList();

    /**
     * @zm-api-field-tag rootGroup indicator
     * @zm-api-field-description indicates whether a HAB group is the parent group in the Hierarchy
     */
    @XmlAttribute(name = AccountConstants.A_ROOT_HAB_GROUP /* rootHabGroup */, required = false)
    private ZmBoolean rootGroup;

    /**
     * @zm-api-field-tag parentGroupId
     * @zm-api-field-description id of the parent group
     */
    @XmlAttribute(name = AccountConstants.A_PARENT_HAB_GROUP_ID /* parentHabGroupId */, required = false)
    private String parentGroupId;

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

    public List<Attr> getAttrs() {
        return attrs;
    }

    
    public void setAttrs(List<Attr> attrs) {
        this.attrs = attrs;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("HABGroup [");
        if (getName() != null) {
            builder.append("name=");
            builder.append(getName());
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
        if (attrs != null) {
            builder.append("attrs=");
            builder.append(attrs);
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
            builder.append(", ");
        }
        builder.append("seniorityIndex=");
        builder.append(getSeniorityIndex());
        builder.append("]\n");
        return builder.toString();
    }

}
