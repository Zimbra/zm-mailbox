/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.type;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Collections;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.account.type.DistributionListOwnerInfo;
import com.zimbra.soap.base.DistributionListOwnerInfoInterface;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_DL)
public class DistributionListInfo extends AdminObjectInfo {

    @XmlAttribute(name=AdminConstants.A_DYNAMIC, required=false)
    ZmBoolean dynamic;
    @XmlElement(name=AdminConstants.E_DLM, required=false)
    private List<String> members;

    @XmlElementWrapper(name=AdminConstants.E_DL_OWNERS, required=false)
    @XmlElement(name=AdminConstants.E_DL_OWNER, required=false)
    private List<DistributionListOwnerInfo> owners = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private DistributionListInfo() {
        this((String) null, (String) null,
            (Collection <String>) null, (Collection <Attr>) null);
    }

    public DistributionListInfo(String id, String name) {
        this((String)id, (String)name,
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

    public void setOwners(List<DistributionListOwnerInfo> owners) {
        this.owners = owners;
    }

    public void addOwner(DistributionListOwnerInfo owner) {
        owners.add(owner);
    }

    public List<? extends DistributionListOwnerInfoInterface> getOwners() {
        return Collections.unmodifiableList(owners);
    }

}
