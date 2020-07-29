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

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.BackupConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"fileCopier", "accounts"})
public class RestoreSpec {

    /**
     * @zm-api-field-tag ca|ra|mb
     * @zm-api-field-description Method.  Valid values <b>ca|ra|mb</b>
     */
    @XmlAttribute(name=BackupConstants.A_METHOD /* method */, required=false)
    private String method;

    // Valid values are include/exclude (case insensitive)
    /**
     * @zm-api-field-tag include|exclude
     * @zm-api-field-description whether to include or exclude searchIndex.  Valid values <b>include|exclude</b>
     */
    @XmlAttribute(name=BackupConstants.A_SEARCH_INDEX /* searchIndex */, required=false)
    private String searchIndex;

    // Valid values are include/exclude (case insensitive)
    /**
     * @zm-api-field-tag include|exclude
     * @zm-api-field-description whether to include or exclude blobs.  Valid values <b>include|exclude</b>
     */
    @XmlAttribute(name=BackupConstants.A_BLOBS /* blobs */, required=false)
    private String blobs;

    // Valid values are include/exclude (case insensitive)
    /**
     * @zm-api-field-tag include|exclude
     * @zm-api-field-description whether to include or exclude secondary blobs.  Valid values <b>include|exclude</b>
     * <br />
     * Meaningful only when blob restore isn't excluded
     */
    @XmlAttribute(name=BackupConstants.A_SECONDARY_BLOBS /* secondaryBlobs */, required=false)
    private String secondaryBlobs;

    /**
     * @zm-api-field-tag path-to-backup-target
     * @zm-api-field-description Path to backup target
     */
    @XmlAttribute(name=BackupConstants.A_BACKUP_TARGET /* target */, required=false)
    private String target;

    /**
     * @zm-api-field-tag label-to-full-backup-set
     * @zm-api-field-description Label to full backup set
     */
    @XmlAttribute(name=BackupConstants.A_LABEL /* label */, required=false)
    private String label;

    /**
     * @zm-api-field-tag sysData-flag
     * @zm-api-field-description When sysData is set, restore system tables and local config.
     */
    @XmlAttribute(name=BackupConstants.A_SYSDATA /* sysData */, required=false)
    private ZmBoolean sysData;

    /**
     * @zm-api-field-tag include-incrementals
     * @zm-api-field-description when includeIncrementals is set, any incremental backups from the last full backup
     * are also restored. Default to <b>1 (true)</b>
     */
    @XmlAttribute(name=BackupConstants.A_INCLUDEINCREMENTALS /* includeIncrementals */, required=false)
    private ZmBoolean includeIncrementals;

    /**
     * @zm-api-field-tag replay-redo-logs
     * @zm-api-field-description Replay redo logs
     */
    @XmlAttribute(name=BackupConstants.A_REPLAY_CURRENT_REDOLOGS /* replayRedo */, required=false)
    private ZmBoolean replayCurrentRedoLogs;

    /**
     * @zm-api-field-tag continue-on-error
     * @zm-api-field-description Continue on error
     */
    @XmlAttribute(name=BackupConstants.A_CONTINUE /* continue */, required=false)
    private ZmBoolean continueOnError;

    /**
     * @zm-api-field-tag new-acct-prefix
     * @zm-api-field-description Used to produce new account names if the name is reused or a new account is to be
     * created
     */
    @XmlAttribute(name=BackupConstants.A_PREFIX /* prefix */, required=false)
    private String prefix;

    /**
     * @zm-api-field-tag restore-to-millis
     * @zm-api-field-description Restore to time in milliseconds
     */
    @XmlAttribute(name=BackupConstants.A_RESTORE_TO_TIME /* restoreToTime */, required=false)
    private Long restoreToTime;

    /**
     * @zm-api-field-tag redo-log-seq-number
     * @zm-api-field-description Redo log sequence number
     */
    @XmlAttribute(name=BackupConstants.A_RESTORE_TO_REDO_SEQ /* restoreToRedoSeq */, required=false)
    private Long restoreToRedoSeq;

    /**
     * @zm-api-field-tag incremental-backup-label
     * @zm-api-field-description Restore to incremental backup label
     */
    @XmlAttribute(name=BackupConstants.A_RESTORE_TO_INCR_LABEL /* restoreToIncrLabel */, required=false)
    private String restoreToIncrLabel;

    /**
     * @zm-api-field-tag ignore-redo-errors
     * @zm-api-field-description Ignore redo errors
     */
    @XmlAttribute(name=BackupConstants.A_IGNORE_REDO_ERRORS /* ignoreRedoErrors */, required=false)
    private ZmBoolean ignoreRedoErrors;

    /**
     * @zm-api-field-tag skip-delete-ops
     * @zm-api-field-description Skip delete operations during redo replay.  Default <b>0 (false)</b>
     */
    @XmlAttribute(name=BackupConstants.A_SKIP_DELETE_OPS /* skipDeleteOps */, required=false)
    private ZmBoolean skipDeleteOps;

    /**
     * @zm-api-field-tag skip-del-accts
     * @zm-api-field-description Skip deleted accounts
     */
    @XmlAttribute(name=BackupConstants.A_SKIP_DELETED_ACCT /* skipDeletedAccounts */, required=false)
    private ZmBoolean skipDeletedAccounts;

    /**
     * @zm-api-field-tag skip-rehosted-accounts
     * @zm-api-field-description Skip rehosted accounts.  Default <b>0 (true)</b>
     */
    @XmlAttribute(name=BackupConstants.A_SKIP_REHOSTED_ACCT /* skipRehostedAccounts */, required=false)
    private ZmBoolean skipRehostedAccounts;

    /**
     * @zm-api-field-description File copier specification
     */
    @XmlElement(name=BackupConstants.E_FILE_COPIER /* fileCopier */, required=false)
    private FileCopierSpec fileCopier;

    /**
     * @zm-api-field-description Accounts - if all accounts then use <b>&lt;account name="all"/></b>
     */
    @XmlElement(name=BackupConstants.E_ACCOUNT /* account */, required=false)
    private List<Name> accounts = Lists.newArrayList();

    public RestoreSpec() {
    }

    public void setMethod(String method) { this.method = method; }
    public void setSearchIndex(String searchIndex) {
        this.searchIndex = searchIndex;
    }
    public void setBlobs(String blobs) { this.blobs = blobs; }
    public void setSecondaryBlobs(String secondaryBlobs) {
        this.secondaryBlobs = secondaryBlobs;
    }
    public void setTarget(String target) { this.target = target; }
    public void setLabel(String label) { this.label = label; }
    public void setSysData(Boolean sysData) { this.sysData = ZmBoolean.fromBool(sysData); }
    public void setIncludeIncrementals(Boolean includeIncrementals) {
        this.includeIncrementals = ZmBoolean.fromBool(includeIncrementals);
    }
    public void setReplayCurrentRedoLogs(Boolean replayCurrentRedoLogs) {
        this.replayCurrentRedoLogs = ZmBoolean.fromBool(replayCurrentRedoLogs);
    }
    public void setContinueOnError(Boolean continueOnError) {
        this.continueOnError = ZmBoolean.fromBool(continueOnError);
    }
    public void setPrefix(String prefix) { this.prefix = prefix; }
    public void setRestoreToTime(Long restoreToTime) {
        this.restoreToTime = restoreToTime;
    }
    public void setRestoreToRedoSeq(Long restoreToRedoSeq) {
        this.restoreToRedoSeq = restoreToRedoSeq;
    }
    public void setRestoreToIncrLabel(String restoreToIncrLabel) {
        this.restoreToIncrLabel = restoreToIncrLabel;
    }
    public void setIgnoreRedoErrors(Boolean ignoreRedoErrors) {
        this.ignoreRedoErrors = ZmBoolean.fromBool(ignoreRedoErrors);
    }
    public void setSkipDeleteOps(Boolean skipDeleteOps) {
        this.skipDeleteOps = ZmBoolean.fromBool(skipDeleteOps);
    }
    public void setSkipDeletedAccounts(Boolean skipDeletedAccounts) {
        this.skipDeletedAccounts = ZmBoolean.fromBool(skipDeletedAccounts);
    }

    public void setSkipRehostedAccounts(Boolean skipRehostedAccounts) {
        this.skipRehostedAccounts = ZmBoolean.fromBool(skipRehostedAccounts);
    }

    public void setFileCopier(FileCopierSpec fileCopier) {
        this.fileCopier = fileCopier;
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

    public String getMethod() { return method; }
    public String getSearchIndex() { return searchIndex; }
    public String getBlobs() { return blobs; }
    public String getSecondaryBlobs() { return secondaryBlobs; }
    public String getTarget() { return target; }
    public String getLabel() { return label; }
    public Boolean getSysData() { return ZmBoolean.toBool(sysData); }
    public Boolean getIncludeIncrementals() { return ZmBoolean.toBool(includeIncrementals); }
    public Boolean getReplayCurrentRedoLogs() { return ZmBoolean.toBool(replayCurrentRedoLogs); }
    public Boolean getContinueOnError() { return ZmBoolean.toBool(continueOnError); }
    public String getPrefix() { return prefix; }
    public Long getRestoreToTime() { return restoreToTime; }
    public Long getRestoreToRedoSeq() { return restoreToRedoSeq; }
    public String getRestoreToIncrLabel() { return restoreToIncrLabel; }
    public Boolean getIgnoreRedoErrors() { return ZmBoolean.toBool(ignoreRedoErrors); }
    public Boolean getSkipDeleteOps() { return ZmBoolean.toBool(skipDeleteOps); }
    public Boolean getSkipDeletedAccounts() { return ZmBoolean.toBool(skipDeletedAccounts); }
    public Boolean getSkipRehostedAccounts() { return ZmBoolean.toBool(skipRehostedAccounts); }
    public FileCopierSpec getFileCopier() { return fileCopier; }
    public List<Name> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("method", method)
            .add("searchIndex", searchIndex)
            .add("blobs", blobs)
            .add("secondaryBlobs", secondaryBlobs)
            .add("target", target)
            .add("label", label)
            .add("sysData", sysData)
            .add("includeIncrementals", includeIncrementals)
            .add("replayCurrentRedoLogs", replayCurrentRedoLogs)
            .add("continueOnError", continueOnError)
            .add("prefix", prefix)
            .add("restoreToTime", restoreToTime)
            .add("restoreToRedoSeq", restoreToRedoSeq)
            .add("restoreToIncrLabel", restoreToIncrLabel)
            .add("ignoreRedoErrors", ignoreRedoErrors)
            .add("skipDeleteOps", skipDeleteOps)
            .add("skipDeletedAccounts", skipDeletedAccounts)
            .add("fileCopier", fileCopier)
            .add("accounts", accounts);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
