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
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.WaitSetResp;
import com.zimbra.soap.type.AccountWithModifications;
import com.zimbra.soap.type.IdAndType;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_WAIT_SET_RESPONSE)
@XmlType(propOrder = {"signalledAccounts", "errors"})
public class WaitSetResponse implements WaitSetResp {

    /**
     * @zm-api-field-tag waitset-id
     * @zm-api-field-description WaitSet ID
     */
    @XmlAttribute(name=MailConstants.A_WAITSET_ID /* waitSet */, required=true)
    private String waitSetId;

    /**
     * @zm-api-field-description <b>1(true)</b> if canceled
     */
    @XmlAttribute(name=MailConstants.A_CANCELED /* canceled */, required=false)
    private ZmBoolean canceled;

    /**
     * @zm-api-field-tag sequence-num
     * @zm-api-field-description Sequence number
     */
    @XmlAttribute(name=MailConstants.A_SEQ /* seq */, required=false)
    private String seqNo;

    /**
     * @zm-api-field-tag signaled-accounts
     * @zm-api-field-description Information on signaled accounts.
     * <br />If folder IDs are included then changes only affect those folders.
     */
    @XmlElement(name=MailConstants.E_A /* a */, required=false)
    private final List<AccountWithModifications> signalledAccounts = Lists.newArrayList();

    /**
     * @zm-api-field-description Error information
     */
    @XmlElement(name=MailConstants.E_ERROR /* error */, required=false)
    private final List<IdAndType> errors = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    public WaitSetResponse() {
        this((String) null);
    }

    public WaitSetResponse(String waitSetId) {
        this.waitSetId = waitSetId;
    }

    @Override
    public void setWaitSetId(String waitSetId) { this.waitSetId = waitSetId; }
    @Override
    public void setCanceled(Boolean canceled) { this.canceled = ZmBoolean.fromBool(canceled); }
    @Override
    public void setSeqNo(String seqNo) { this.seqNo = seqNo; }
    @Override
    public void setSignalledAccounts(Iterable <AccountWithModifications> signalledAccounts) {
        this.signalledAccounts.clear();
        if (signalledAccounts != null) {
            Iterables.addAll(this.signalledAccounts,signalledAccounts);
        }
    }

    @Override
    public WaitSetResponse addSignalledAccount(AccountWithModifications signalledAccount) {
        this.signalledAccounts.add(signalledAccount);
        return this;
    }

    @Override
    public void setErrors(Iterable <IdAndType> errors) {
        this.errors.clear();
        if (errors != null) {
            Iterables.addAll(this.errors,errors);
        }
    }

    @Override
    public WaitSetResponse addError(IdAndType error) {
        this.errors.add(error);
        return this;
    }

    @Override
    public String getWaitSetId() { return waitSetId; }
    @Override
    public Boolean getCanceled() { return ZmBoolean.toBool(canceled); }
    @Override
    public String getSeqNo() { return seqNo; }
    @Override
    public List<AccountWithModifications> getSignalledAccounts() {
        return Collections.unmodifiableList(signalledAccounts);
    }
    @Override
    public List<IdAndType> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("waitSetId", waitSetId)
            .add("canceled", canceled)
            .add("seqNo", seqNo)
            .add("signalledAccounts", signalledAccounts)
            .add("errors", errors);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
