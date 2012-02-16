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

package com.zimbra.soap.mail.message;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.Id;
import com.zimbra.soap.type.WaitSetAddSpec;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-description WaitSetRequest optionally modifies the wait set and checks for any notifications.
 * If <b>block</b> is set and there are no notificatins, then this API will BLOCK until there is data.
 * <p>
 * Client should always set 'seq' to be the highest known value it has received from the server.  The server will use
 * this information to retransmit lost data.
 * </p><p>
 * If the client sends a last known sync token then the notification is calculated by comparing the accounts current
 * token with the client's last known.
 * </p><p>
 * If the client does not send a last known sync token, then notification is based on change since last Wait
 * (or change since &lt;add> if this is the first time Wait has been called with the account)
 * </p><p>
 * The client may specifiy a custom timeout-length for their request if they know something about the particular
 * underlying network.  The server may or may not honor this request (depending on server configured max/min values).
 * See LocalConfig values:
 * </p>
 * <pre>
 * zimbra_waitset_default_request_timeout,
 * zimbra_waitset_min_request_timeout,
 * zimbra_waitset_max_request_timeout,
 * zimbra_admin_waitset_default_request_timeout,
 * zimbra_admin_waitset_min_request_timeout, and
 * zimbra_admin_waitset_max_request_timeout
 * </pre>
 * <p>
 * WaitSet: scalable mechanism for listening for changes to one or more accounts
 * </p>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_WAIT_SET_REQUEST)
public class WaitSetRequest {

    /**
     * @zm-api-field-tag waitset-id
     * @zm-api-field-description Waitset ID
     */
    @XmlAttribute(name=MailConstants.A_WAITSET_ID /* waitSet */, required=true)
    private final String waitSetId;

    /**
     * @zm-api-field-tag waitset-last-known-seq-no
     * @zm-api-field-description Last known sequence number
     */
    @XmlAttribute(name=MailConstants.A_SEQ /* seq */, required=true)
    private final String lastKnownSeqNo;

    /**
     * @zm-api-field-tag waitset-block
     * @zm-api-field-description Flag whether or not to block until some account has new data
     */
    @XmlAttribute(name=MailConstants.A_BLOCK /* block */, required=false)
    private ZmBoolean block;

    // default interest types required for "All" waitsets
    /**
     * @zm-api-field-tag default-interests
     * @zm-api-field-description Default interest types: comma-separated list.  Currently:
     * <table>
     * <tr> <td> <b>c</b> </td> <td> contacts </td> </tr>
     * <tr> <td> <b>m</b> </td> <td> msgs (and subclasses) </td> </tr>
     * <tr> <td> <b>a</b> </td> <td> appointments </td> </tr>
     * <tr> <td> <b>t</b> </td> <td> tasks </td> </tr>
     * <tr> <td> <b>d</b> </td> <td> documents </td> </tr>
     * <tr> <td> <b>all</b> </td> <td> all types (equiv to "c,m,a,t,d") * </td> </tr>
     * </table>
     */
    @XmlAttribute(name=MailConstants.A_DEFTYPES /* defTypes */, required=false)
    private String defaultInterests;

    /**
     * @zm-api-field-tag waitset-timeout-length
     * @zm-api-field-description Timeout length
     */
    @XmlAttribute(name=MailConstants.A_TIMEOUT /* timeout */, required=false)
    private Long timeout;

    /**
     * @zm-api-field-description Waitsets to add
     */
    @XmlElementWrapper(name=MailConstants.E_WAITSET_ADD /* add */, required=false)
    @XmlElement(name=MailConstants.E_A /* a */, required=false)
    private List<WaitSetAddSpec> addAccounts = Lists.newArrayList();

    /**
     * @zm-api-field-description Waitsets to update
     */
    @XmlElementWrapper(name=MailConstants.E_WAITSET_UPDATE /* update */, required=false)
    @XmlElement(name=MailConstants.E_A /* a */, required=false)
    private List<WaitSetAddSpec> updateAccounts = Lists.newArrayList();

    /**
     * @zm-api-field-description Waitsets to remove
     */
    @XmlElementWrapper(name=MailConstants.E_WAITSET_REMOVE /* remove */, required=false)
    @XmlElement(name=MailConstants.E_A /* a */, required=false)
    private List<Id> removeAccounts = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private WaitSetRequest() {
        this((String) null, (String) null);
    }

    public WaitSetRequest(String waitSetId, String lastKnownSeqNo) {
        this.waitSetId = waitSetId;
        this.lastKnownSeqNo = lastKnownSeqNo;
    }

    public void setBlock(Boolean block) { this.block = ZmBoolean.fromBool(block); }
    public void setDefaultInterests(String defaultInterests) {
        this.defaultInterests = defaultInterests;
    }
    public void setTimeout(Long timeout) { this.timeout = timeout; }
    public void setAddAccounts(Iterable <WaitSetAddSpec> addAccounts) {
        this.addAccounts.clear();
        if (addAccounts != null) {
            Iterables.addAll(this.addAccounts,addAccounts);
        }
    }

    public WaitSetRequest addAddAccount(WaitSetAddSpec addAccount) {
        this.addAccounts.add(addAccount);
        return this;
    }

    public void setUpdateAccounts(Iterable <WaitSetAddSpec> updateAccounts) {
        this.updateAccounts.clear();
        if (updateAccounts != null) {
            Iterables.addAll(this.updateAccounts,updateAccounts);
        }
    }

    public WaitSetRequest addUpdateAccount(WaitSetAddSpec updateAccount) {
        this.updateAccounts.add(updateAccount);
        return this;
    }

    public void setRemoveAccounts(Iterable <Id> removeAccounts) {
        this.removeAccounts.clear();
        if (removeAccounts != null) {
            Iterables.addAll(this.removeAccounts,removeAccounts);
        }
    }

    public WaitSetRequest addRemoveAccount(Id removeAccount) {
        this.removeAccounts.add(removeAccount);
        return this;
    }

    public String getWaitSetId() { return waitSetId; }
    public String getLastKnownSeqNo() { return lastKnownSeqNo; }
    public Boolean getBlock() { return ZmBoolean.toBool(block); }
    public String getDefaultInterests() { return defaultInterests; }
    public Long getTimeout() { return timeout; }
    public List<WaitSetAddSpec> getAddAccounts() {
        return Collections.unmodifiableList(addAccounts);
    }
    public List<WaitSetAddSpec> getUpdateAccounts() {
        return Collections.unmodifiableList(updateAccounts);
    }
    public List<Id> getRemoveAccounts() {
        return Collections.unmodifiableList(removeAccounts);
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("waitSetId", waitSetId)
            .add("lastKnownSeqNo", lastKnownSeqNo)
            .add("block", block)
            .add("defaultInterests", defaultInterests)
            .add("timeout", timeout)
            .add("addAccounts", addAccounts)
            .add("updateAccounts", updateAccounts)
            .add("removeAccounts", removeAccounts);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
