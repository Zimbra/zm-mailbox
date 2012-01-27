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

package com.zimbra.soap.admin.message;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.BackupConstants;
import com.zimbra.soap.admin.type.Name;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-description Show mailbox moves in progress on this server.  Both move-ins and move-outs are shown.
 * <br />
 * If accounts are given only data for those accounts are returned.  Data for all moves are returned if no accounts
 * are given.
 * <br />
 * If checkPeer=1 (true), peer servers are queried to check if the move is active on the peer. [default 0 (false)]
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=BackupConstants.E_QUERY_MAILBOX_MOVE_REQUEST)
public class QueryMailboxMoveRequest {

    /**
     * @zm-api-field-description Flag whether to query peer servers to see if the move is active on them.
     * [default 0 (false)]
     */
    @XmlAttribute(name=BackupConstants.A_CHECK_PEER, required=false)
    private ZmBoolean checkPeer;

    /**
     * @zm-api-field-description Accounts - If empty report on all outstanding moves
     */
    @XmlElement(name=BackupConstants.E_ACCOUNT /* account */, required=false)
    private List<Name> accounts = Lists.newArrayList();

    public QueryMailboxMoveRequest() {
    }

    public void setCheckPeer(Boolean checkPeer) { this.checkPeer = ZmBoolean.fromBool(checkPeer); }
    public void setAccounts(Iterable <Name> accounts) {
        this.accounts.clear();
        if (accounts != null) {
            Iterables.addAll(this.accounts,accounts);
        }
    }

    public void addAccount(Name account) {
        this.accounts.add(account);
    }

    public Boolean getCheckPeer() { return ZmBoolean.toBool(checkPeer); }
    public List<Name> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("checkPeer", checkPeer)
            .add("accounts", accounts);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
