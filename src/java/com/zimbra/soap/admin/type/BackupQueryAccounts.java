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

package com.zimbra.soap.admin.type;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.BackupConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {})
public class BackupQueryAccounts {

    @XmlAttribute(name=BackupConstants.A_TOTAL_COUNT /* total */, required=false)
    private Integer totalCount;

    @XmlAttribute(name=BackupConstants.A_COMPLETION_COUNT /* completionCount */, required=false)
    private Integer completionCount;

    @XmlAttribute(name=BackupConstants.A_ERROR_COUNT /* errorCount */, required=false)
    private Integer errorCount;

    @XmlAttribute(name=BackupConstants.A_MORE /* more */, required=false)
    private ZmBoolean more;

    @XmlElement(name=BackupConstants.E_ACCOUNT /* account */, required=false)
    private List<BackupQueryAccountStatus> accounts = Lists.newArrayList();

    public BackupQueryAccounts() {
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }
    public void setCompletionCount(Integer completionCount) {
        this.completionCount = completionCount;
    }
    public void setErrorCount(Integer errorCount) {
        this.errorCount = errorCount;
    }
    public void setMore(Boolean more) { this.more = ZmBoolean.fromBool(more); }
    public void setAccounts(Iterable <BackupQueryAccountStatus> accounts) {
        this.accounts.clear();
        if (accounts != null) {
            Iterables.addAll(this.accounts,accounts);
        }
    }

    public void addAccount(BackupQueryAccountStatus account) {
        this.accounts.add(account);
    }

    public Integer getTotalCount() { return totalCount; }
    public Integer getCompletionCount() { return completionCount; }
    public Integer getErrorCount() { return errorCount; }
    public Boolean getMore() { return ZmBoolean.toBool(more); }
    public List<BackupQueryAccountStatus> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("totalCount", totalCount)
            .add("completionCount", completionCount)
            .add("errorCount", errorCount)
            .add("more", more)
            .add("accounts", accounts);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
