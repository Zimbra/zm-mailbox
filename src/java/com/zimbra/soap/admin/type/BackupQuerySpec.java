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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.BackupConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
public class BackupQuerySpec {

    @XmlAttribute(name=BackupConstants.A_BACKUP_TARGET
                    /* target */, required=false)
    private String target;

    @XmlAttribute(name=BackupConstants.A_LABEL /* label */, required=false)
    private String label;

    @XmlAttribute(name=BackupConstants.A_TYPE /* type */, required=false)
    private String type;

    @XmlAttribute(name=BackupConstants.A_FROM /* from */, required=false)
    private Long from;

    @XmlAttribute(name=BackupConstants.A_TO /* to */, required=false)
    private Long to;

    @XmlAttribute(name=BackupConstants.A_STATS /* stats */, required=false)
    private ZmBoolean showStats;

    @XmlAttribute(name=BackupConstants.A_BACKUP_LIST_OFFSET /* backupListOffset */, required=false)
    private Integer backupListOffset;

    @XmlAttribute(name=BackupConstants.A_BACKUP_LIST_COUNT /* backupListCount */, required=false)
    private Integer backupListCount;

    @XmlAttribute(name=BackupConstants.A_ACCOUNT_LIST_STATUS /* accountListStatus */, required=false)
    private String accountListStatus;

    @XmlAttribute(name=BackupConstants.A_ACCOUNT_LIST_OFFSET /* accountListOffset */, required=false)
    private Integer accountListOffset;

    @XmlAttribute(name=BackupConstants.A_ACCOUNT_LIST_COUNT /* accountListCount */, required=false)
    private Integer accountListCount;

    public BackupQuerySpec() {
    }

    public void setTarget(String target) { this.target = target; }
    public void setLabel(String label) { this.label = label; }
    public void setType(String type) { this.type = type; }
    public void setFrom(Long from) { this.from = from; }
    public void setTo(Long to) { this.to = to; }
    public void setShowStats(Boolean showStats) { this.showStats = ZmBoolean.fromBool(showStats); }
    public void setBackupListOffset(Integer backupListOffset) {
        this.backupListOffset = backupListOffset;
    }
    public void setBackupListCount(Integer backupListCount) {
        this.backupListCount = backupListCount;
    }
    public void setAccountListStatus(String accountListStatus) {
        this.accountListStatus = accountListStatus;
    }
    public void setAccountListOffset(Integer accountListOffset) {
        this.accountListOffset = accountListOffset;
    }
    public void setAccountListCount(Integer accountListCount) {
        this.accountListCount = accountListCount;
    }
    public String getTarget() { return target; }
    public String getLabel() { return label; }
    public String getType() { return type; }
    public Long getFrom() { return from; }
    public Long getTo() { return to; }
    public Boolean getShowStats() { return ZmBoolean.toBool(showStats); }
    public Integer getBackupListOffset() { return backupListOffset; }
    public Integer getBackupListCount() { return backupListCount; }
    public String getAccountListStatus() { return accountListStatus; }
    public Integer getAccountListOffset() { return accountListOffset; }
    public Integer getAccountListCount() { return accountListCount; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("target", target)
            .add("label", label)
            .add("type", type)
            .add("from", from)
            .add("to", to)
            .add("showStats", showStats)
            .add("backupListOffset", backupListOffset)
            .add("backupListCount", backupListCount)
            .add("accountListStatus", accountListStatus)
            .add("accountListOffset", accountListOffset)
            .add("accountListCount", accountListCount);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
