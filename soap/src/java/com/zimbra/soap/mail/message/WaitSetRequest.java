/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.mail.message;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.WaitSetReq;
import com.zimbra.soap.type.Id;
import com.zimbra.soap.type.WaitSetAddSpec;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description WaitSetRequest optionally modifies the wait set and checks for any notifications.
 * If <b>block</b> is set and there are no notifications, then this API will BLOCK until there is data.
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
 * The client may specify a custom timeout-length for their request if they know something about the particular
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
public class WaitSetRequest implements WaitSetReq {

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
     * <tr> <td> <b>f</b> </td> <td> folders </td> </tr>
     * <tr> <td> <b>m</b> </td> <td> messages </td> </tr>
     * <tr> <td> <b>c</b> </td> <td> contacts </td> </tr>
     * <tr> <td> <b>a</b> </td> <td> appointments </td> </tr>
     * <tr> <td> <b>t</b> </td> <td> tasks </td> </tr>
     * <tr> <td> <b>d</b> </td> <td> documents </td> </tr>
     * <tr> <td> <b>all</b> </td> <td> all types (equiv to "f,m,c,a,t,d") </td> </tr>
     * </table>
     * <p>This is used if <b>types</b> isn't specified for an account</p>
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
     * @zm-api-field-tag expand
     * @zm-api-field-description boolean flag. If true, WaitSetResponse will include details of Pending Modifications.
     */
    @XmlAttribute(name=MailConstants.A_EXPAND /* expand */, required=false)
    private ZmBoolean expand;

    /**
     * @zm-api-field-description Waitsets to add
     */
    @XmlElementWrapper(name=MailConstants.E_WAITSET_ADD /* add */, required=false)
    @XmlElement(name=MailConstants.E_A /* a */, required=false)
    private final List<WaitSetAddSpec> addAccounts = Lists.newArrayList();

    /**
     * @zm-api-field-description Waitsets to update
     */
    @XmlElementWrapper(name=MailConstants.E_WAITSET_UPDATE /* update */, required=false)
    @XmlElement(name=MailConstants.E_A /* a */, required=false)
    private final List<WaitSetAddSpec> updateAccounts = Lists.newArrayList();

    /**
     * @zm-api-field-description Waitsets to remove
     */
    @XmlElementWrapper(name=MailConstants.E_WAITSET_REMOVE /* remove */, required=false)
    @XmlElement(name=MailConstants.E_A /* a */, required=false)
    private final List<Id> removeAccounts = Lists.newArrayList();

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

    @Override
    public void setExpand(Boolean expand) {this.expand = ZmBoolean.fromBool(expand); }
    @Override
    public void setBlock(Boolean block) { this.block = ZmBoolean.fromBool(block); }
    @Override
    public void setDefaultInterests(String defaultInterests) {
        this.defaultInterests = defaultInterests;
    }
    @Override
    public void setTimeout(Long timeout) { this.timeout = timeout; }
    @Override
    public void setAddAccounts(Iterable <WaitSetAddSpec> addAccounts) {
        this.addAccounts.clear();
        if (addAccounts != null) {
            Iterables.addAll(this.addAccounts,addAccounts);
        }
    }

    @Override
    public WaitSetRequest addAddAccount(WaitSetAddSpec addAccount) {
        this.addAccounts.add(addAccount);
        return this;
    }

    @Override
    public void setUpdateAccounts(Iterable <WaitSetAddSpec> updateAccounts) {
        this.updateAccounts.clear();
        if (updateAccounts != null) {
            Iterables.addAll(this.updateAccounts,updateAccounts);
        }
    }

    @Override
    public WaitSetRequest addUpdateAccount(WaitSetAddSpec updateAccount) {
        this.updateAccounts.add(updateAccount);
        return this;
    }

    @Override
    public void setRemoveAccounts(Iterable <Id> removeAccounts) {
        this.removeAccounts.clear();
        if (removeAccounts != null) {
            Iterables.addAll(this.removeAccounts,removeAccounts);
        }
    }

    @Override
    public WaitSetRequest addRemoveAccount(Id removeAccount) {
        this.removeAccounts.add(removeAccount);
        return this;
    }

    @Override
    public boolean getExpand() { return ZmBoolean.toBool(expand, false); }
    @Override
    public String getWaitSetId() { return waitSetId; }
    @Override
    public String getLastKnownSeqNo() { return lastKnownSeqNo; }
    @Override
    public Boolean getBlock() { return ZmBoolean.toBool(block, false); }
    @Override
    public String getDefaultInterests() { return defaultInterests; }
    @Override
    public Long getTimeout() { return timeout; }
    @Override
    public List<WaitSetAddSpec> getAddAccounts() {
        return Collections.unmodifiableList(addAccounts);
    }
    @Override
    public List<WaitSetAddSpec> getUpdateAccounts() {
        return Collections.unmodifiableList(updateAccounts);
    }
    @Override
    public List<Id> getRemoveAccounts() {
        return Collections.unmodifiableList(removeAccounts);
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("waitSetId", waitSetId)
            .add("lastKnownSeqNo", lastKnownSeqNo)
            .add("block", block)
            .add("expand", expand)
            .add("defaultInterests", defaultInterests)
            .add("timeout", timeout)
            .add("addAccounts", addAccounts)
            .add("updateAccounts", updateAccounts)
            .add("removeAccounts", removeAccounts);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
