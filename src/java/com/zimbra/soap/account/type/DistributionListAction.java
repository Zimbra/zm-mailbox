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

@XmlAccessorType(XmlAccessType.NONE)
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

    /**
     * @zm-api-field-tag operation
     * @zm-api-field-description Operation to perform.
     * <table>
     * <tr><td><b>delete</b>         </td><td>delete the list</td></tr>
     * <tr><td><b>rename</b>         </td><td>rename the list</td></tr>
     * <tr><td><b>modify</b>         </td><td>modify the list</td></tr>
     * <tr><td><b>addOwners</b>      </td><td>add list owner</td></tr>
     * <tr><td><b>removeOwners</b>   </td><td>remove list owners</td></tr>
     * <tr><td><b>setOwners</b>      </td><td>set list owners</td></tr>
     * <tr><td><b>grantRights</b>    </td><td>grant rights</td></tr>
     * <tr><td><b>revokeRights</b>   </td><td>revoke rights</td></tr>
     * <tr><td><b>setRights</b>      </td><td>set rights</td></tr>
     * <tr><td><b>addAlias</b>       </td><td>add list alias</td></tr>
     * <tr><td><b>removeAlias</b>    </td><td>remove list alias</td></tr>
     * <tr><td><b>addMembers</b>     </td><td>add list members</td></tr>
     * <tr><td><b>removeMembers</b>  </td><td>remove list members</td></tr>
     * <tr><td><b>acceptSubsReq</b>  </td><td>accept subscription/un-subscription request</td></tr>
     * <tr><td><b>rejectSubsReq</b>  </td><td>reject subscription/un-subscription request</td></tr>
     * </table>
     */
    @XmlAttribute(name=AccountConstants.A_OP /* op */, required=true)
    private final Operation op;

    /**
     * @zm-api-field-description Group members
     * <br />
     * Required if op="addMembers" or op="removeMembers"
     */
    @XmlElement(name=AccountConstants.E_DLM /* dlm */, required=false)
    protected List<String> members = Lists.newArrayList();

    /**
     * @zm-api-field-description Alias
     * <br />
     * Required if op="addAlias" or or="removeAlias"
     */
    @XmlElement(name=AccountConstants.E_ALIAS /* alias */, required=false)
    protected String alias;

    /**
     * @zm-api-field-tag new-name
     * @zm-api-field-description New Name
     * <br />
     * Required if op="rename"
     */
    @XmlElement(name=AccountConstants.E_NEW_NAME /* newName */, required=false)
    protected String newName;

    /**
     * @zm-api-field-description Owner
     * <br />
     * Required if op="addOwners" or op="removeOwners",
     * <br />
     * Optional if op="setOwners".  If not present when op="setOwners" all current owners will be removed.
     */
    @XmlElement(name=AccountConstants.E_OWNER /* owner */, required=false)
    protected List<DistributionListGranteeSelector> owners;

    /**
     * @zm-api-field-description Rights
     * <br />
     * Required if op="grantRight", op="revokeRight", op="setRight".
     */
    @XmlElement(name=AccountConstants.E_RIGHT /* right */, required=false)
    protected List<DistributionListRightSpec> rights;

    /**
     * @zm-api-field-description Subscription request
     * <br />
     * Required if op="acceptSubsReq" or op="rejectSubsReq"
     */
    @XmlElement(name=AccountConstants.E_DL_SUBS_REQ /* subsReq */, required=false)
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
