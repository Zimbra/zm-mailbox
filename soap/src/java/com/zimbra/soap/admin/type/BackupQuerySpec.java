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

package com.zimbra.soap.admin.type;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.BackupConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class BackupQuerySpec {

    /**
     * @zm-api-field-tag path-to-backup-target
     * @zm-api-field-description Path to backup target
     */
    @XmlAttribute(name=BackupConstants.A_BACKUP_TARGET /* target */, required=false)
    private String target;

    /**
     * @zm-api-field-tag backup-set-label
     * @zm-api-field-description Backup set label
     */
    @XmlAttribute(name=BackupConstants.A_LABEL /* label */, required=false)
    private String label;

    /**
     * @zm-api-field-tag backup-type
     * @zm-api-field-description Backup type - <b>full|incremental</b>
     */
    @XmlAttribute(name=BackupConstants.A_TYPE /* type */, required=false)
    private String type;

    /**
     * @zm-api-field-tag start-time-millis
     * @zm-api-field-description Return backups whose start time is at or after this time.
     * Defaults to beginning of time if not specified
     */
    @XmlAttribute(name=BackupConstants.A_FROM /* from */, required=false)
    private Long from;

    /**
     * @zm-api-field-tag end-time-millis
     * @zm-api-field-description Return backups whose end time is at or before this time.
     * Defaults to end of time if not specified
     */
    @XmlAttribute(name=BackupConstants.A_TO /* to */, required=false)
    private Long to;

    /**
     * @zm-api-field-tag show-stats
     * @zm-api-field-description Statistics will be included in the response if this is set
     */
    @XmlAttribute(name=BackupConstants.A_STATS /* stats */, required=false)
    private ZmBoolean showStats;

    /**
     * @zm-api-field-tag backup-list-offset
     * @zm-api-field-description Backup list offset.  Used in backup list pagination.  Default = 0
     */
    @XmlAttribute(name=BackupConstants.A_BACKUP_LIST_OFFSET /* backupListOffset */, required=false)
    private Integer backupListOffset;

    /**
     * @zm-api-field-tag backup-list-count
     * @zm-api-field-description Backup list count.  Used in backup list pagination.  Default = -1, meaning all
     */
    @XmlAttribute(name=BackupConstants.A_BACKUP_LIST_COUNT /* backupListCount */, required=false)
    private Integer backupListCount;

    /**
     * @zm-api-field-tag account-list-status-filter
     * @zm-api-field-description Use this to filter the account list by backup status.
     * <br />
     * Values: <b>NONE | ALL | COMPLETED | ERROR | NOTSTARTED | INPROGRESS</b>
     * <br />
     * Default: <b>NONE</b>
     */
    @XmlAttribute(name=BackupConstants.A_ACCOUNT_LIST_STATUS /* accountListStatus */, required=false)
    private String accountListStatus;

    /**
     * @zm-api-field-tag account-list-offset
     * @zm-api-field-description Account list offset.  Used in account list pagination. Default = 0
     */
    @XmlAttribute(name=BackupConstants.A_ACCOUNT_LIST_OFFSET /* accountListOffset */, required=false)
    private Integer accountListOffset;

    /**
     * @zm-api-field-tag account-list-count
     * @zm-api-field-description Account list count.  Used in account list pagination.  Default = -1, meaning all
     */
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

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
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
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
