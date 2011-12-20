/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

package com.zimbra.soap.account.type;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class DistributionListAction extends AccountKeyValuePairs {

    @XmlEnum
    public static enum Operation {
        // case must match protocol
        delete, 
        modify, 
        rename, 
        addAlias, 
        removeAlias, 
        addOwners, 
        removeOwners, 
        setOwners,
        grantRights,
        revokeRights,
        setRights,
        addMembers, 
        removeMembers,
        acceptSubsReq,
        rejectSubsReq;
        
        public static Operation fromString(String s) throws ServiceException {
            try {
                return Operation.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown operation: "+s, e);
            }
        }
    }
    
    @XmlAttribute(name=AccountConstants.A_OP, required=true)
    private final Operation op;
    
    @XmlElement(name=AccountConstants.E_DLM, required=false)
    protected List<String> members = Lists.newArrayList();

    @XmlElement(name=AccountConstants.E_ALIAS, required=false)
    protected String alias;
    
    @XmlElement(name=AccountConstants.E_NEW_NAME, required=false)
    protected String newName;
    
    @XmlElement(name=AccountConstants.E_OWNER, required=false)
    protected List<DistributionListGranteeSelector> owners;
    
    @XmlElement(name=AccountConstants.E_RIGHT, required=false)
    protected List<DistributionListRightSpec> rights;
    
    @XmlElement(name=AccountConstants.E_DL_SUBS_REQ, required=false)
    protected DistributionListSubscribeReq subsReq;
    
    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private DistributionListAction() {
        this(null);
    }

    public DistributionListAction(Operation op) {
        this.op = op;
    }
    
    public Operation getOp() { return op; }

    public void addMember(String member) {
        if (members == null) {
            members = Lists.newArrayList();
        }
        members.add(member);
    }
    
    public void setMembers(Iterable <String> members) {
        this.members = null;
        if (members != null) {
            this.members = Lists.newArrayList();
            Iterables.addAll(this.members, members);
        }
    }
    
    public List<String> getMembers() {
        return members;
    }
    
    public void setAlias(String alias) {
        this.alias = alias;
    }
    
    public String getAlias() {
        return alias;
    }
    
    public void setNewName(String newName) {
        this.newName = newName;
    }
    
    public String getNewName() {
        return newName;
    }
    
    public void addOwner(DistributionListGranteeSelector owner) {
        if (owners == null) {
            owners = Lists.newArrayList();
        }
        owners.add(owner);
    }
    
    public void setOwners(List<DistributionListGranteeSelector> owners) {
        this.owners = null;
        if (owners != null) {
            this.owners = Lists.newArrayList();
            Iterables.addAll(this.owners, owners);
        }
    }
    
    public List<DistributionListGranteeSelector> getOwners() {
        return owners;
    }
    
    
    public void addRight(DistributionListRightSpec right) {
        if (rights == null) {
            rights = Lists.newArrayList();
        }
        rights.add(right);
    }
    
    public void setRights(List<DistributionListRightSpec> rights) {
        this.rights = null;
        if (rights != null) {
            this.rights = Lists.newArrayList();
            Iterables.addAll(this.rights, rights);
        }
    }
    
    public List<DistributionListRightSpec> getRights() {
        return rights;
    }
    
    public void setSubsReq(DistributionListSubscribeReq subsReq) {
        this.subsReq = subsReq;
    }
    
    public DistributionListSubscribeReq getSubsReq() {
        return subsReq;
    }
    
}
