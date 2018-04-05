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

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {})
public class BackupAccountQuerySpec {

    /**
     * @zm-api-field-tag path-to-backup-target
     * @zm-api-field-description Path to backup target
     */
    @XmlAttribute(name=BackupConstants.A_BACKUP_TARGET /* target */, required=false)
    private String target;

    /**
     * @zm-api-field-tag backup-type
     * @zm-api-field-description Backup type - <b>full|incremental</b>.  Means both types if omitted or bogus value
     */
    @XmlAttribute(name=BackupConstants.A_TYPE /* type */, required=false)
    private String type;

    /**
     * @zm-api-field-tag start-time-millis
     * @zm-api-field-description Return backups whose start time is at or after this time.  Defaults to beginning of
     * time if not specified.
     */
    @XmlAttribute(name=BackupConstants.A_FROM /* from */, required=false)
    private Long from;

    /**
     * @zm-api-field-tag end-time-millis
     * @zm-api-field-description Return backups whose start time is at or before this time.  Defaults to end of time
     * if not specified
     */
    @XmlAttribute(name=BackupConstants.A_TO /* to */, required=false)
    private Long to;

    /**
     * @zm-api-field-tag backup-list-offset-number
     * @zm-api-field-description Backup list offset number.  Default = 0
     */
    @XmlAttribute(name=BackupConstants.A_BACKUP_LIST_OFFSET /* backupListOffset */, required=false)
    private Integer backupListOffset;

    /**
     * @zm-api-field-tag backup-list-count-number
     * @zm-api-field-description Backup list count number.  Default = -1, meaning all
     */
    @XmlAttribute(name=BackupConstants.A_BACKUP_LIST_COUNT /* backupListCount */, required=false)
    private Integer backupListCount;

    /**
     * @zm-api-field-tag account-email-or-all
     * @zm-api-field-description Either the account email address or <b>all</b>
     */
    @XmlElement(name=BackupConstants.E_ACCOUNT /* account */, required=false)
    private List<Name> accounts = Lists.newArrayList();

    public BackupAccountQuerySpec() {
    }

    public void setTarget(String target) { this.target = target; }
    public void setType(String type) { this.type = type; }
    public void setFrom(Long from) { this.from = from; }
    public void setTo(Long to) { this.to = to; }
    public void setBackupListOffset(Integer backupListOffset) {
        this.backupListOffset = backupListOffset;
    }
    public void setBackupListCount(Integer backupListCount) {
        this.backupListCount = backupListCount;
    }
    public void setAccounts(Iterable <Name> accounts) {
        this.accounts.clear();
        if (accounts != null) {
            Iterables.addAll(this.accounts,accounts);
        }
    }

    public void addAccount(Name account) {
        this.accounts.add(account);
    }

    public String getTarget() { return target; }
    public String getType() { return type; }
    public Long getFrom() { return from; }
    public Long getTo() { return to; }
    public Integer getBackupListOffset() { return backupListOffset; }
    public Integer getBackupListCount() { return backupListCount; }
    public List<Name> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("target", target)
            .add("type", type)
            .add("from", from)
            .add("to", to)
            .add("backupListOffset", backupListOffset)
            .add("backupListCount", backupListCount)
            .add("accounts", accounts);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
