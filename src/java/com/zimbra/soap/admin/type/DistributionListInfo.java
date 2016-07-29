/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.type;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import com.google.common.collect.Lists;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonArrayForWrapper;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class DistributionListInfo extends AdminObjectInfo {

    /**
     * @zm-api-field-tag dl-is-dynamic
     * @zm-api-field-description Flags whether this is a dynamic distribution list or not
     */
    @XmlAttribute(name=AdminConstants.A_DYNAMIC /* dynamic */, required=false)
    ZmBoolean dynamic;
    /**
     * @zm-api-field-description dl-members
     */
    @XmlElement(name=AdminConstants.E_DLM /* dlm */, required=false)
    private List<String> members;

    /**
     * @zm-api-field-description Owner information
     */
    @ZimbraJsonArrayForWrapper
    @XmlElementWrapper(name=AdminConstants.E_DL_OWNERS /* owners */, required=false)
    @XmlElement(name=AdminConstants.E_DL_OWNER /* owner */, required=false)
    private List<GranteeInfo> owners = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private DistributionListInfo() {
        this((String) null, (String) null,
            (Collection <String>) null, (Collection <Attr>) null);
    }

    public DistributionListInfo(String id, String name) {
        this(id, name,
            (Collection <String>) null, (Collection <Attr>) null);
    }

    public DistributionListInfo(String id, String name,
            Collection <String> members, Collection <Attr> attrs) {
        super(id, name, attrs);
        setMembers(members);
    }

    public void setMembers(Collection <String> members) {
        this.members = Lists.newArrayList();
        if (members != null) {
            this.members.addAll(members);
        }
    }

    public List<String> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public void setDynamic(Boolean dynamic) {
        this.dynamic = ZmBoolean.fromBool(dynamic);
    }

    public Boolean isDynamic() {
        return ZmBoolean.toBool(dynamic, false);
    }

    public void setOwners(List<GranteeInfo> owners) {
        this.owners = owners;
    }

    public void addOwner(GranteeInfo owner) {
        owners.add(owner);
    }

    public List<GranteeInfo> getOwners() {
        return Collections.unmodifiableList(owners);
    }

}
